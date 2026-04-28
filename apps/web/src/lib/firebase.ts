import { getApps, initializeApp, type FirebaseApp } from "firebase/app";
import { getAuth, type Auth } from "firebase/auth";

const firebaseConfig = {
  apiKey: process.env.NEXT_PUBLIC_FIREBASE_API_KEY,
  authDomain: process.env.NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN,
  projectId: process.env.NEXT_PUBLIC_FIREBASE_PROJECT_ID,
  storageBucket: process.env.NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: process.env.NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID,
  appId: process.env.NEXT_PUBLIC_FIREBASE_APP_ID,
};

// Re-using the singleton across HMR is critical: initializeApp() throws if
// called twice with the same name, and Next.js fast-refresh re-imports modules.
function getFirebaseApp(): FirebaseApp {
  const existing = getApps();
  if (existing.length > 0) return existing[0]!;
  return initializeApp(firebaseConfig);
}

export const firebaseApp = getFirebaseApp();
export const firebaseAuth: Auth = getAuth(firebaseApp);
