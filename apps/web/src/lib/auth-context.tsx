"use client";

import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import {
  browserLocalPersistence,
  createUserWithEmailAndPassword,
  onIdTokenChanged,
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
  signOut: () => Promise<void>;
};

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

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
      setUser(u);
      setLoading(false);
    });
    return unsub;
  }, []);

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
      signOut: async () => {
        await firebaseSignOut(firebaseAuth);
      },
    }),
    [user, loading],
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
