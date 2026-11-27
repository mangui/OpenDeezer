# Stable signing for the family Portal Go build

The personal Portal Go build has its own Android identity, independent of
upstream OpenDeezer's release signing key:

| Field | Stable value |
| --- | --- |
| Package | `fr.cyclooo.opendeezer.familyportal` |
| Key alias | `portalgo` |
| Certificate SHA-256 | `A0:DB:72:93:9C:78:C9:81:27:4E:90:B2:62:FA:0E:C5:0E:6D:8C:7E:32:EB:DC:97:4A:20:7F:05:5F:9A:B4:E9` |
| First versionCode | `1000` |

This key is **not** the upstream release key described in `ANDROID_SIGNING.md`.
It must never be committed to Git or added to the APK itself.

## Build another update

Keep a backup of `opendeezer-family-portal.p12` and its password separately.
The private keystore is stored outside the repository. A build uses the same
keystore for every subsequent APK:

```sh
export ANDROID_KEYSTORE_FILE=/private/path/opendeezer-family-portal.p12
export ANDROID_KEYSTORE_PASSWORD='(retrieve from your password manager)'
export PORTAL_VERSION_CODE=1001  # strictly greater than installed versionCode
./scripts/build-portalgo-signed.sh
```

Requires JDK 17, the Android SDK including build-tools 34.0.0, and the current
`gui/android/app/libs/odmobile.aar` (build it via `gomobile bind` if missing).
The script builds `assembleMobileRelease`, verifies APK Signature v2/v3 and the
exact certificate, package ID and versionCode, then prints the APK path.
If release Lint cannot fetch uncached dependencies in an offline build environment,
set `PORTAL_OFFLINE_SKIP_LINT=true` for that local build only; keep Lint enabled
in CI.

The first stable APK uses versionCode `1000`, later APKs use `1001`, `1002`, etc.
Do **not** change `portalBuild`'s application ID suffix or replace the key.
On Android, updates require an identical package ID, identical signing
certificate, and a higher `versionCode`.

The v4-v8 preview builds used different debug certificates and packages and
cannot upgrade to this stable identity in place. After installing the first
stable APK and logging in once, future signed updates retain the app data.

For CI, configure a separate copy of this private keystore as repository
secrets (as described in `ANDROID_SIGNING.md`). Keep an independent,
user-controlled backup: GitHub's write-only secrets cannot recover a lost key.
