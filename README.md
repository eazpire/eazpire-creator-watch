# Eazpire Creator Watch (Wear OS)

Creator-only companion for Wear OS (no shop). Synced to `eazpire/eazpire-creator-watch` for Google Play releases.

## Features (MVP)

- Dashboard stats (products, sales, designs, payout balance)
- Active jobs list
- Phone upload via QR (`creator-phone-upload` API)
- Auth synced from phone app via Wearable Data Layer

## Requirements

- **Non-standalone** companion: phone app `com.eazpire.creator` installed, signed in, paired via Bluetooth
- Play review: see [docs/setup/CREATOR_WATCH_PLAY_REVIEW.md](../docs/setup/CREATOR_WATCH_PLAY_REVIEW.md)
- Android Studio or JDK 17 + Android SDK

## Build

```bash
cd creator-watch
./gradlew assembleDebug
```

From repo root: `npm run creator-watch:build` or `npm run creator-watch:run-emulator`

On Windows, outputs go to `%LOCALAPPDATA%\eazpire-creator-watch-build\` (avoids OneDrive file locks). See `EMULATOR_QUICKSTART.md`.

## Setup & Play

See [docs/setup/CREATOR_WATCH_REPO_SETUP.md](../docs/setup/CREATOR_WATCH_REPO_SETUP.md).
