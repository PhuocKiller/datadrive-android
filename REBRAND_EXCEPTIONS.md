# DataDrive rebrand – kept "Nextcloud" occurrences

The generic flavor is shipped as **DataDrive** (`vn.datadrive.client`). Every place below still contains
"Nextcloud" on purpose.

## Legal (must stay)
- SPDX / copyright headers in source, resource and build files (~1300 lines).
- `LICENSE.txt`, `COPYING*`, `LICENSES/` and `AUTHORS`.
- Settings › About › "Open source licenses": "Based on Nextcloud Android (GPLv2)". It links to
  `sourcecode_url` = https://github.com/nextcloud/android. The GPL requires this attribution and the source offer.
- Store description ends with "DataDrive is based on the open source Nextcloud Android app (GPLv2)".

## Technical (not shown to users)
- Java/Kotlin packages and class names: `com.nextcloud.*`, `com.owncloud.*`, `org.nextcloud.*`.
  The Gradle `namespace` stays `com.owncloud.android`, so `R` and `BuildConfig` stay in that package.
- User-Agent strings `nextcloud_user_agent` / `office_user_agent` ("Nextcloud-android/…"). The server uses them to recognise the client.
- `login_data_own_scheme` = `nc` (the `nc://login/...` deep link returned by Login Flow v2).
- Server API paths such as `/index.php/login/v2`, `/ocs/...` and `/remote.php/dav`.
- Library artifacts (`com.github.nextcloud:android-library`, `android-common`) and the SSO constants in `com.nextcloud.android.sso`.
- Deck integration package names (`it.niedermann.nextcloud.deck*`).
- Tool-only placeholders (`placeholder_*`, `translatable="false"`). They are used only in layout previews.
- Log/debug strings (`Log_OC`, exception messages).
- Test fixtures and androidTest/unit test sources.
- Links used only by the disabled "Participate" screen and by the dev flavor: `fdroid_link`,
  `fdroid_beta_link`, `beta_apk_link`, `play_store_register_beta`, `dev_link`, `dev_latest` and `dev_changelog`.
  `participate_enabled` is now false.
- `app/src/gplay/res/values/setup.xml`: Firebase/Google push keys belong to Nextcloud's Firebase project. The gplay flavor is not used. It needs DataDrive's own Firebase project before it is published.
- `google-services.json` in generic/gplay still names `com.nextcloud.client`. The Google Services plugin is not applied, so it has no effect.

## Product names of third-party apps
- `ecosystem_apps_notes` = "Nextcloud Notes", `ecosystem_apps_talk` = "Nextcloud Talk". These are the names of those apps.
  The app-switcher banner is hidden (`is_branded_client=true`). The names can still appear in the
  "This folder is best viewed in Nextcloud Notes" hint, which only shows for a Notes folder.

## Not converted (outside the en/vi scope)
- `values-xx/strings.xml` for other languages still say "Nextcloud". `localeFilters = en, vi` strips them from the APK.
- Store metadata for other locales under `src/generic/fastlane/metadata/*` and `src/versionDev/...`.
  Only en-US and vi-VI were rewritten.
- The qa and versionDev flavors keep their own Nextcloud launcher icons (`app/src/qa/res`, `app/src/versionDev/res`).
  They were not deleted. Only the generic flavor is built.

## Deviations from the brief
- Brand colour: `primary` = #0A93E0 (the main blue of the DataDrive logo) instead of the temporary #0082C9.
  `primary_dark` = #0877B8.
- The logo came from the supplied artwork (`branding/`), so no "DD" placeholder was needed.
- APK name: `DataDrive-<versionName>.apk`, where spaces in the name become "-" (e.g. `DataDrive-35.1.0-Alpha51.apk`).
