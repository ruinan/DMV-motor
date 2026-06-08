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
import {
  browserLocalPersistence,
  createUserWithEmailAndPassword,
  EmailAuthProvider,
  onIdTokenChanged,
  reauthenticateWithCredential,
  sendPasswordResetEmail,
  setPersistence,
  signInWithEmailAndPassword,
  signOut as firebaseSignOut,
  type User,
} from "firebase/auth";
import { firebaseAuth } from "@/lib/firebase";

type AuthState = {
  user: User | null;
  loading: boolean;
  signIn: (email: string, password: string) => Promise<void>;
  signUp: (email: string, password: string) => Promise<void>;
  resetPassword: (email: string) => Promise<void>;
  /** Re-verify the current user's password before a sensitive action; refreshes
   *  the ID token so its auth_time updates (the backend reauth gate reads it). */
  reauth: (password: string) => Promise<void>;
  signOut: () => Promise<void>;
};

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const queryClient = useQueryClient();
  const router = useRouter();
  // Track the last-seen Firebase uid so we can tell an identity CHANGE (login as
  // a different user / logout) apart from a silent token refresh (same uid).
  const prevUid = useRef<string | null | undefined>(undefined);

  useEffect(() => {
    // Force IndexedDB/localStorage persistence so a hard reload rehydrates
    // the user. The SDK already defaults to LOCAL on web, but in restrictive
    // environments (privacy modes, blocked storage) it silently falls back
    // to in-memory — this asserts the contract instead of failing silently.
    setPersistence(firebaseAuth, browserLocalPersistence).catch(() => {
      // Storage unavailable (e.g. partitioned cookies + no IDB). The
      // listener below will still fire, just without rehydrated state.
    });
    // onIdTokenChanged is a superset of onAuthStateChanged — it fires on
    // sign-in / sign-out AND on every silent token refresh (~5min before
    // the 1h TTL expires). Using it keeps the cached user reference fresh
    // so callers reading currentUser.getIdToken() never get a stale token,
    // and gives us a single place to observe TTL boundary events.
    const unsub = onIdTokenChanged(firebaseAuth, (u) => {
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
      setLoading(false);
    });
    return unsub;
  }, [queryClient]);

  const value = useMemo<AuthState>(
    () => ({
      user,
      loading,
      signIn: async (email, password) => {
        await signInWithEmailAndPassword(firebaseAuth, email, password);
      },
      signUp: async (email, password) => {
        // Backend's UserProvisioner JIT-creates the users row on first
        // /api/v1/me call after Firebase issues a token, so the client
        // doesn't need to coordinate row creation here.
        await createUserWithEmailAndPassword(firebaseAuth, email, password);
      },
      resetPassword: async (email) => {
        await sendPasswordResetEmail(firebaseAuth, email);
      },
      reauth: async (password) => {
        const u = firebaseAuth.currentUser;
        if (!u || !u.email) throw new Error("Not signed in");
        const credential = EmailAuthProvider.credential(u.email, password);
        await reauthenticateWithCredential(u, credential);
        // Force-refresh so the cached token carries the new auth_time; the next
        // apiFetch then passes the backend reauth gate.
        await u.getIdToken(true);
      },
      signOut: async () => {
        await firebaseSignOut(firebaseAuth);
        // Land on the marketing index, not whatever authed/anon page we were on
        // (e.g. /practice would otherwise drop you into the free-practice view).
        if (typeof window !== "undefined") {
          const lang = window.location.pathname.split("/")[1] || "en";
          router.push(`/${lang}`);
        }
      },
    }),
    [user, loading, router],
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
