# RemoteFamily Development Rules

1. Read `docs/REMOTEFAMILY_ROADMAP.md` before changing the project.
2. Work only on the requested milestone. Do not anticipate later features without a proven technical requirement.
3. Do not add a dependency without documenting why it is needed and fixing its version explicitly.
4. Do not install or require Android Studio, Android SDK, Android NDK, an emulator, global Gradle, or global Kotlin on the workstation.
5. All Android builds must be reproducible through GitHub Actions using the versioned Gradle Wrapper.
6. Do not call Android work complete while its GitHub Actions workflow is failing.
7. Keep the interface simple: large controls, few elements, clear text, and no unnecessary decoration.
8. Never bypass Android consent, `FLAG_SECURE`, foreground-service notifications, permissions, or other platform protections.
9. Explain and document material architectural changes before implementing them.
10. Write tests first for application rules, state transitions, validation, and protocol behavior.
11. Never commit secrets, tokens, passwords, signing files, or permanent TURN credentials.
12. Use WebRTC for future streaming. Never create JPEG/WebSocket streaming or use Javalin as a video relay.
13. Keep classes focused, use explicit solutions, and avoid abstractions without current use.
14. Record physical-device validation in `docs/testing.md` when a milestone requires it.
