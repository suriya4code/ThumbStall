# Release signing

One signing key for the life of the project. Key continuity matters for any
future store listing — losing it means users must uninstall/reinstall.

## Generate (one-time, human step)

```bash
keytool -genkeypair \
  -keystore thumbstall-release.jks \
  -alias thumbstall \
  -keyalg RSA -keysize 4096 -validity 10950 \
  -storetype JKS
```

- Store `thumbstall-release.jks` **offline** (password manager + offline copy).
- **Never commit it.** `*.jks` and `*.keystore` are gitignored.

## Local signing config (Phase 4 wires this into CI)

Add to `~/.gradle/gradle.properties` (never the repo's):

```properties
THUMBSTALL_STORE_FILE=/absolute/path/to/thumbstall-release.jks
THUMBSTALL_STORE_PASSWORD=...
THUMBSTALL_KEY_ALIAS=thumbstall
THUMBSTALL_KEY_PASSWORD=...
```
