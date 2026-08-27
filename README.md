# RemoteFamily

RemoteFamily is a private Android remote-support application for authorized family devices. One APK can act as controller or host depending on the session.

The project is developed incrementally from the requirements in [`docs/REMOTEFAMILY_ROADMAP.md`](docs/REMOTEFAMILY_ROADMAP.md). Workstations do not require an Android SDK or Android Studio; GitHub Actions is the authoritative Android build environment.

## Current milestone

Milestone 0.1.0 proves that CI can test, lint, assemble, and publish a minimal debug APK.

## Android artifact

After a successful Android CI run, download the `remote-family-debug` artifact from that workflow run. It contains the debug APK for physical-device validation.
