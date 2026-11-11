# lavalynx

Firefox for Android (Fenix) with the parts I don't want taken out. This is a
fork of [mozilla-firefox/firefox](https://github.com/mozilla-firefox/firefox),
the Firefox monorepo; Fenix lives under `mobile/android/fenix`. Everything
lavalynx-specific is in this directory, in `.github/workflows/lavalynx-release.yml`,
and in a handful of marked `// lavalynx:` lines in the Kotlin.

## What is changed

- **No Mozilla VPN / "IP Protection".** `FenixIPProtectionEligibilityStorage`
  reports the user ineligible no matter what Nimbus, the region or the secret
  settings say, which removes the menu entry (the "Try for free" badge), the
  Settings row, the trust-panel row and the onboarding prompts, and keeps the
  proxy service uninitialized.
- **No Firefox Sync / Mozilla account UI.** `FeatureFlags.SYNC_UI_ENABLED` is
  `false`, which hides the Synced Tabs page in the tab manager, the account
  entry in both main menus, the sign-in rows in Settings, the sync preferences
  under Passwords and Autofill, the sign-in onboarding and setup-checklist steps,
  "Send to device" and the bookmarks sign-in prompt. The account machinery
  itself is untouched.
- **No nags or feature tips.** `FeatureFlags.NAG_PROMPTS_ENABLED` and
  `FEATURE_TIPS_ENABLED` are `false`, which removes the default-browser prompts,
  the "turn on notifications" card, continuous onboarding, the review prompt,
  the setup checklist, the weekly privacy report, Terms of Use re-prompts, the
  privacy notice banner, CFR tooltips and onboarding cards. The matching
  Settings entries still work.
- **Its own package name and label.** The Nightly build type is
  `org.mozilla.lavalynx`, named "Lavalynx", so it installs next to any Firefox
  channel rather than colliding with Firefox Nightly.

## Building

Everything lives in a container; the host needs only `podman`. The build is an
*artifact build*: mach downloads the compiled Gecko (GeckoView's native
libraries and omni.ja) that Mozilla's CI built for the nearest upstream commit,
and only the Kotlin side is compiled here. That is why no change below the
Kotlin layer can be made in this fork without switching to a full build.

```sh
lavalynx/dev image       # build the toolchain image once (Ubuntu 24.04 + jj)
lavalynx/dev bootstrap   # once: JDK, Android SDK, node, python deps into the cache volume
lavalynx/dev build       # ./mach build + assemble the arm64 Nightly APK (unsigned)
lavalynx/dev sign        # sign it with .secrets/ -> obj-lavalynx/lavalynx/lavalynx-<v>.apk
lavalynx/dev apk         # both
lavalynx/dev test mobile/android/fenix/app/src/test/java/org/mozilla/fenix/tabstray
lavalynx/dev lint        # ./mach gradle ktfmtFormat detekt
lavalynx/dev shell       # poke around inside
lavalynx/dev clean       # drop the objdir and the cache volumes
```

The container runs as your UID (`--userns=keep-id`) so build outputs are yours.
The mach state dir (SDK, JDK), Gradle's cache and the container HOME live in the
named volumes `lavalynx-mozbuild`, `lavalynx-gradle` and `lavalynx-home`; the
object directory is `obj-lavalynx/` in the checkout (gitignored).

The mozconfig is `lavalynx/mozconfig` and is picked up through `MOZCONFIG`.

## Installing on a phone

Tag a commit and CI builds, signs and publishes the APK as a release asset:

```sh
jj bookmark set main -r @- && jj git push --bookmark main
lavalynx/dev tag
```

Tags are dates (`v2026.11.11`, then `v2026.11.11.2` for a second one that day).
Android decides what is an upgrade from the APK's `versionCode`, which Fenix's
build derives from the build time, so it is monotonic without anything to
coordinate. The release's APK is named `lavalynx-<versionName>-<versionCode>.apk`.

[Obtainium](https://github.com/ImranR98/Obtainium) watches the releases page:
add `https://github.com/BackSlasher/lavalynx` as an app source. Play Protect
will ask whether to send the app for a check the first time, because the
signing certificate isn't one Google knows; declining is fine.

Each CI-built APK carries a signed build-provenance attestation:

```sh
gh attestation verify lavalynx-<v>.apk --repo BackSlasher/lavalynx
```

Nothing in the install path checks it, so it is provenance rather than
protection.

## Signing

The keystore lives in `.secrets/`, beside the project and excluded by
`.gitignore` (which also keeps jj from snapshotting it):

```
.secrets/lavalynx-release.jks    4096-bit RSA, 30-year validity, lavalynx only
.secrets/keystore.properties     storeFile / storePassword / keyAlias / keyPassword, mode 600
```

It is a PKCS12 keystore, so the key password is the store password (keytool
ignores a different `-keypass` for PKCS12); the properties file carries both
only because apksigner asks for both.

CI gets the same material as repository secrets `LAVALYNX_KEYSTORE_BASE64`,
`LAVALYNX_STORE_PASSWORD`, `LAVALYNX_KEY_ALIAS` and `LAVALYNX_KEY_PASSWORD`:

```sh
gh secret set LAVALYNX_KEYSTORE_BASE64 -R BackSlasher/lavalynx < <(base64 -w0 .secrets/lavalynx-release.jks)
. .secrets/keystore.properties   # or read the values out by hand
gh secret set LAVALYNX_STORE_PASSWORD -R BackSlasher/lavalynx --body "$storePassword"
gh secret set LAVALYNX_KEY_ALIAS      -R BackSlasher/lavalynx --body "$keyAlias"
gh secret set LAVALYNX_KEY_PASSWORD   -R BackSlasher/lavalynx --body "$keyPassword"
```

Local builds are signed with the same key, so a release installs over a local
build as an ordinary upgrade. **Back the keystore up**: lose it and no future
build can upgrade an installed lavalynx. **Never commit it**: the repo is public.

## Keeping up with upstream

```sh
jj git fetch --remote upstream
jj rebase -d main@upstream        # rebase the lavalynx changes onto the new upstream main
```

The lavalynx changes are deliberately small and each is one `// lavalynx:`
marked spot, so rebases should be quiet. If upstream moves the VPN or Sync
entry points, the markers are what to look for.
