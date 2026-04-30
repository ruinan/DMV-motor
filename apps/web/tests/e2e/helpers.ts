const EMULATOR_HOST = "http://127.0.0.1:9099";
const PROJECT_ID = "demo-dmv-motor";
// API key value is irrelevant in emulator mode; the path requires *some* key.
const FAKE_API_KEY = "fake-api-key";

/**
 * Wipes all accounts in the Auth emulator. Call from beforeEach for a clean slate.
 */
export async function resetEmulator(): Promise<void> {
  const url = `${EMULATOR_HOST}/emulator/v1/projects/${PROJECT_ID}/accounts`;
  const res = await fetch(url, { method: "DELETE" });
  if (!res.ok) {
    throw new Error(`emulator reset failed: ${res.status} ${await res.text()}`);
  }
}

/**
 * Creates a user via emulator REST API. Returns the user's localId (Firebase UID).
 */
export async function createTestUser(
  email: string,
  password: string,
): Promise<string> {
  const url = `${EMULATOR_HOST}/identitytoolkit.googleapis.com/v1/accounts:signUp?key=${FAKE_API_KEY}`;
  const res = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password, returnSecureToken: true }),
  });
  if (!res.ok) {
    throw new Error(`signUp failed: ${res.status} ${await res.text()}`);
  }
  const json = (await res.json()) as { localId: string };
  return json.localId;
}
