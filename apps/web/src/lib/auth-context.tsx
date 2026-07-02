"use client";

import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import type { MultiFactorResolver, TotpSecret, User } from "firebase/auth";
import { loadFirebaseAuth } from "@/lib/firebase";
import { apiFetch } from "@/lib/api-client";

// Only type imports from firebase/auth here — every SDK value is reached via
// loadFirebaseAuth() so the ~113 KB chunk stays out of the eager bundle (this
// provider wraps every route, anonymous landing included).

/**
 * Thrown by {@link AuthState.signIn} when the account has 2FA enrolled and
 * Firebase needs the second factor. Carries the resolver so the UI can prompt
 * for the TOTP code and finish via {@link AuthState.resolveMfaSignIn}.
 */
export class MfaRequiredError extends Error {
  constructor(public readonly resolver: MultiFactorResolver) {
    super("Multi-factor authentication required");
    this.name = "MfaRequiredError";
  }
}

type AuthState = {
  user: User | null;
  loading: boolean;
  /** Whether the signed-in user has at least one 2FA factor enrolled. Computed
   *  in the token listener (SDK in scope there) and republished on every token
   *  refresh — finishTotpEnrollment forces one, so gates react immediately. */
  mfaEnrolled: boolean;
  signIn: (email: string, password: string) => Promise<void>;
  signUp: (email: string, password: string) => Promise<void>;
  resetPassword: (email: string) => Promise<void>;
  /** Re-verify the current user's password before a sensitive action; refreshes
   *  the ID token so its auth_time updates (the backend reauth gate reads it). */
  reauth: (password: string) => Promise<void>;
  /** Change the password: re-authenticate with the current password, then set
   *  the new one. Throws auth/wrong-password (current) or auth/weak-password. */
  changePassword: (currentPassword: string, newPassword: string) => Promise<void>;
  /** Change the sign-in email: re-authenticate, then send a confirmation link to
   *  the NEW address. The email only changes once that link is clicked. */
  changeEmail: (newEmail: string, currentPassword: string) => Promise<void>;
  /** Permanently delete the account: re-authenticate, hard-delete all server-side
   *  data, then remove the Firebase identity and land on the marketing index. */
  deleteAccount: (password: string) => Promise<void>;
  /** (Re)send the email-verification link to the signed-in user. */
  resendVerificationEmail: () => Promise<void>;
  /** Reload the signed-in user from Firebase and report whether the email is now
   *  verified. Used by the verification gate to detect the user clicked the link. */
  reloadUser: () => Promise<boolean>;
  /** EMULATOR-ONLY dev bypass: pull the pending verification oob code straight
   *  from the Auth emulator and apply it, so local testing (no real inbox) can
   *  get past the email-verification gate. No-op / throws against real Firebase. */
  devVerifyEmail: () => Promise<boolean>;
  /** Finish a 2FA-gated sign-in: verify the TOTP code against the resolver. */
  resolveMfaSignIn: (resolver: MultiFactorResolver, code: string) => Promise<void>;
  /** Begin TOTP enrollment: returns the secret + an otpauth URL for the QR. */
  startTotpEnrollment: () => Promise<{ secret: TotpSecret; qrUrl: string }>;
  /** Complete TOTP enrollment by confirming a code from the authenticator. */
  finishTotpEnrollment: (secret: TotpSecret, code: string) => Promise<void>;
  signOut: () => Promise<void>;
};

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [mfaEnrolled, setMfaEnrolled] = useState(false);
  const [loading, setLoading] = useState(true);
  const queryClient = useQueryClient();
  const router = useRouter();
  // Track the last-seen Firebase uid so we can tell an identity CHANGE (login as
  // a different user / logout) apart from a silent token refresh (same uid).
  const prevUid = useRef<string | null | undefined>(undefined);

  useEffect(() => {
    let cancelled = false;
    let unsub: (() => void) | undefined;
    void loadFirebaseAuth().then(({ auth, mod }) => {
      if (cancelled) return;
      // Force IndexedDB/localStorage persistence so a hard reload rehydrates
      // the user. The SDK already defaults to LOCAL on web, but in restrictive
      // environments (privacy modes, blocked storage) it silently falls back
      // to in-memory — this asserts the contract instead of failing silently.
      mod.setPersistence(auth, mod.browserLocalPersistence).catch(() => {
        // Storage unavailable (e.g. partitioned cookies + no IDB). The
        // listener below will still fire, just without rehydrated state.
      });
      // onIdTokenChanged is a superset of onAuthStateChanged — it fires on
      // sign-in / sign-out AND on every silent token refresh (~5min before
      // the 1h TTL expires). Using it keeps the cached user reference fresh
      // so callers reading currentUser.getIdToken() never get a stale token,
      // and gives us a single place to observe TTL boundary events.
      unsub = mod.onIdTokenChanged(auth, (u) => {
        const uid = u?.uid ?? null;
        // On a real identity change (not the initial fire, not a token refresh),
        // drop ALL cached query data so the next user never inherits the previous
        // user's / anonymous session, /me, history, etc. — that mismatch caused
        // "Session belongs to a different user". Covers login-as-different-user
        // and logout.
        if (prevUid.current !== undefined && prevUid.current !== uid) {
          queryClient.clear();
        }
        prevUid.current = uid;
        setUser(u);
        setMfaEnrolled(!!u && mod.multiFactor(u).enrolledFactors.length > 0);
        setLoading(false);
      });
    });
    return () => {
      cancelled = true;
      unsub?.();
    };
  }, [queryClient]);

  const value = useMemo<AuthState>(
    () => ({
      user,
      loading,
      mfaEnrolled,
      signIn: async (email, password) => {
        const { auth, mod } = await loadFirebaseAuth();
        try {
          await mod.signInWithEmailAndPassword(auth, email, password);
        } catch (e) {
          // 2FA-enrolled account → Firebase needs the second factor. Surface the
          // resolver so the form can prompt for the TOTP code.
          if ((e as { code?: string })?.code === "auth/multi-factor-auth-required") {
            throw new MfaRequiredError(
              mod.getMultiFactorResolver(
                auth,
                e as Parameters<typeof mod.getMultiFactorResolver>[1],
              ),
            );
          }
          throw e;
        }
      },
      signUp: async (email, password) => {
        // Backend's UserProvisioner JIT-creates the users row on first
        // /api/v1/me call after Firebase issues a token, so the client
        // doesn't need to coordinate row creation here.
        const { auth, mod } = await loadFirebaseAuth();
        await mod.createUserWithEmailAndPassword(auth, email, password);
      },
      resetPassword: async (email) => {
        const { auth, mod } = await loadFirebaseAuth();
        await mod.sendPasswordResetEmail(auth, email);
      },
      resendVerificationEmail: async () => {
        const { auth, mod } = await loadFirebaseAuth();
        const u = auth.currentUser;
        if (!u) throw new Error("Not signed in");
        await mod.sendEmailVerification(u);
      },
      reloadUser: async () => {
        const { auth } = await loadFirebaseAuth();
        const u = auth.currentUser;
        if (!u) return false;
        await u.reload();
        // reload() mutates currentUser in place; force a token refresh so the
        // onIdTokenChanged listener re-publishes the user and downstream reads
        // (and the next backend call) see the verified state.
        await u.getIdToken(true);
        return u.emailVerified;
      },
      devVerifyEmail: async () => {
        const { app, auth, mod } = await loadFirebaseAuth();
        const u = auth.currentUser;
        if (!u || !u.email) throw new Error("Not signed in");
        // Hard guard: this hits the emulator's admin REST surface, which only
        // exists when the SDK is pointed at the emulator. Never against prod.
        if (!auth.emulatorConfig) {
          throw new Error("Email-verify bypass is emulator-only");
        }
        const projectId = app.options.projectId;
        const res = await fetch(
          `http://127.0.0.1:9099/emulator/v1/projects/${projectId}/oobCodes`,
        );
        const data = (await res.json()) as {
          oobCodes?: { email: string; requestType: string; oobCode: string }[];
        };
        // Most recent VERIFY_EMAIL code for this user (the gate auto-sends one).
        const match = (data.oobCodes ?? [])
          .filter(
            (c) => c.email === u.email && c.requestType === "VERIFY_EMAIL",
          )
          .pop();
        if (!match) throw new Error("No verification code found in emulator");
        await mod.applyActionCode(auth, match.oobCode);
        await u.reload();
        await u.getIdToken(true);
        return u.emailVerified;
      },
      reauth: async (password) => {
        const { auth, mod } = await loadFirebaseAuth();
        const u = auth.currentUser;
        if (!u || !u.email) throw new Error("Not signed in");
        const credential = mod.EmailAuthProvider.credential(u.email, password);
        await mod.reauthenticateWithCredential(u, credential);
        // Force-refresh so the cached token carries the new auth_time; the next
        // apiFetch then passes the backend reauth gate.
        await u.getIdToken(true);
      },
      changePassword: async (currentPassword, newPassword) => {
        const { auth, mod } = await loadFirebaseAuth();
        const u = auth.currentUser;
        if (!u || !u.email) throw new Error("Not signed in");
        // Firebase requires a recent login to set a new password — re-auth with
        // the current one first (also surfaces a clear "wrong current password").
        const credential = mod.EmailAuthProvider.credential(u.email, currentPassword);
        await mod.reauthenticateWithCredential(u, credential);
        await mod.updatePassword(u, newPassword);
      },
      changeEmail: async (newEmail, currentPassword) => {
        const { auth, mod } = await loadFirebaseAuth();
        const u = auth.currentUser;
        if (!u || !u.email) throw new Error("Not signed in");
        // Re-auth first (Firebase requires recent login + surfaces a clear wrong
        // password). verifyBeforeUpdateEmail sends a confirmation link to the NEW
        // address; the email only actually changes once that link is clicked, so
        // a typo can't lock the user out of their account.
        const credential = mod.EmailAuthProvider.credential(u.email, currentPassword);
        await mod.reauthenticateWithCredential(u, credential);
        await mod.verifyBeforeUpdateEmail(u, newEmail);
      },
      deleteAccount: async (password) => {
        const { auth, mod } = await loadFirebaseAuth();
        const u = auth.currentUser;
        if (!u || !u.email) throw new Error("Not signed in");
        // Recent login is required by BOTH the backend reauth gate and Firebase
        // deleteUser(); re-auth once up front.
        const credential = mod.EmailAuthProvider.credential(u.email, password);
        await mod.reauthenticateWithCredential(u, credential);
        await u.getIdToken(true);
        // 1) Server-side hard delete (cascades every user-owned row) while the
        //    token is still valid.
        await apiFetch("/api/v1/me", { method: "DELETE" });
        // 2) Remove the Firebase identity so the email can't sign back in or be
        //    JIT re-provisioned into a fresh empty account.
        await mod.deleteUser(u);
        // 3) Land on the marketing index (the auth listener also clears state).
        if (typeof window !== "undefined") {
          const lang = window.location.pathname.split("/")[1] || "en";
          router.push(`/${lang}`);
        }
      },
      resolveMfaSignIn: async (resolver, code) => {
        const { mod } = await loadFirebaseAuth();
        // TOTP is the only factor we enroll; use the first hint.
        const hint = resolver.hints[0];
        const assertion = mod.TotpMultiFactorGenerator.assertionForSignIn(hint.uid, code);
        await resolver.resolveSignIn(assertion);
      },
      startTotpEnrollment: async () => {
        const { auth, mod } = await loadFirebaseAuth();
        const u = auth.currentUser;
        if (!u) throw new Error("Not signed in");
        const session = await mod.multiFactor(u).getSession();
        const secret = await mod.TotpMultiFactorGenerator.generateSecret(session);
        const qrUrl = secret.generateQrCodeUrl(u.email ?? "account", "DMV Prep");
        return { secret, qrUrl };
      },
      finishTotpEnrollment: async (secret, code) => {
        const { auth, mod } = await loadFirebaseAuth();
        const u = auth.currentUser;
        if (!u) throw new Error("Not signed in");
        const assertion = mod.TotpMultiFactorGenerator.assertionForEnrollment(secret, code);
        await mod.multiFactor(u).enroll(assertion, "Authenticator app");
        // Refresh so downstream reads (the enrollment gate) see the new factor.
        await u.getIdToken(true);
      },
      signOut: async () => {
        const { auth, mod } = await loadFirebaseAuth();
        await mod.signOut(auth);
        // Land on the marketing index, not whatever authed/anon page we were on
        // (e.g. /practice would otherwise drop you into the free-practice view).
        if (typeof window !== "undefined") {
          const lang = window.location.pathname.split("/")[1] || "en";
          router.push(`/${lang}`);
        }
      },
    }),
    [user, loading, mfaEnrolled, router],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used inside <AuthProvider>");
  }
  return ctx;
}
