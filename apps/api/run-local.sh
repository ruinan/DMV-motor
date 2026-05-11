#!/usr/bin/env bash
# Local dev launcher for DMV Motor API.
#
# What it does:
#   - Loads DeepSeek API key from gcloud Secret Manager (so no plaintext on disk).
#   - Wires the Firebase Auth emulator (must already be running on 127.0.0.1:9099).
#   - Starts Spring Boot with the `local` profile (application-local.yml, gitignored).
#
# Prereqs (one-time per dev box):
#   1. Docker container `dmv-motor-postgres` running (see CLAUDE.md §10 "本地 DB").
#   2. Firebase Auth emulator started in a separate terminal:
#        cd apps/web && npx firebase emulators:start --only auth --project demo-dmv-motor
#   3. `gcloud auth login` (for Secret Manager access).
#
# Pair with frontend: cd apps/web && NEXT_PUBLIC_USE_FIREBASE_EMULATOR=true npm run dev
# (or set the flag in apps/web/.env.local).

set -e

DEEPSEEK_KEY=$(gcloud secrets versions access latest \
    --secret=deepseek-api-key \
    --project=dmv-motor-prod 2>/dev/null)

if [ -z "$DEEPSEEK_KEY" ]; then
    echo "ERROR: failed to read deepseek-api-key from Secret Manager." >&2
    echo "       Check 'gcloud auth list' and project access." >&2
    exit 1
fi

export APP_AI_DEEPSEEK_API_KEY="$DEEPSEEK_KEY"
export FIREBASE_AUTH_EMULATOR_HOST=127.0.0.1:9099
export GOOGLE_CLOUD_PROJECT=demo-dmv-motor

mvn spring-boot:run -Dspring-boot.run.profiles=local
