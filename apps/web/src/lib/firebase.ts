import type { FirebaseApp } from "firebase/app";
import type { Auth } from "firebase/auth";

/**
 * Lazy Firebase bootstrap. Nothing here touches the SDK at module scope, so
 * importing this file costs ~0 bytes — the ~113 KB firebase/auth chunk only
 * downloads (async, post-hydration) when loadFirebaseAuth() first runs. That
 * keeps it off the critical path of every route, anonymous pages included.
 */

const firebaseConfig = {
  apiKey: process.env.NEXT_PUBLIC_FIREBASE_API_KEY,
  authDomain: process.env.NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN,
  projectId: process.env.NEXT_PUBLIC_FIREBASE_PROJECT_ID,
  storageBucket: process.env.NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: process.env.NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID,
  appId: process.env.NEXT_PUBLIC_FIREBASE_APP_ID,
};

/** The full firebase/auth module — consumers pull SDK functions off this
 * instead of static imports (which would drag the SDK into the eager bundle). */
export type FirebaseAuthModule = typeof import("firebase/auth");

export type FirebaseHandles = {
  app: FirebaseApp;
  auth: Auth;
  mod: FirebaseAuthModule;
};

let handlesPromise: Promise<FirebaseHandles> | null = null;

/**
 * Loads the SDK and initializes the singleton app + auth. Memoized; safe under
 * HMR (initializeApp() throws if called twice with the same name, hence the
 * getApps() reuse; connectAuthEmulator() throws on a second call, hence the
 * emulatorConfig guard).
 */
export function loadFirebaseAuth(): Promise<FirebaseHandles> {
  if (!handlesPromise) {
    handlesPromise = (async () => {
      const [{ getApps, initializeApp }, mod] = await Promise.all([
        import("firebase/app"),
        import("firebase/auth"),
      ]);
      const existing = getApps();
      const app = existing.length > 0 ? existing[0]! : initializeApp(firebaseConfig);
      const auth = mod.getAuth(app);
      // E2E: when NEXT_PUBLIC_USE_FIREBASE_EMULATOR=true, point the SDK at the
      // local Auth emulator instead of prod Firebase.
      if (
        typeof window !== "undefined" &&
        process.env.NEXT_PUBLIC_USE_FIREBASE_EMULATOR === "true" &&
        !auth.emulatorConfig
      ) {
        mod.connectAuthEmulator(auth, "http://127.0.0.1:9099", {
          disableWarnings: true,
        });
      }
      return { app, auth, mod };
    })();
  }
  return handlesPromise;
}
