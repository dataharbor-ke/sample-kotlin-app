# FDIS Kotlin sample

Native Android implementation for FDIS SDK `2.0.1`.

## Setup

Set the organisation key in `gradle.properties`.

```properties
FDIS_ORG_KEY=your_org_key_here
```

Build the app with Gradle.

```bash
./gradlew :app:assembleDebug
```

## Android configuration

- `compileSdk`: `37`
- `targetSdk`: `37`
- `minSdk`: `24`
- `libs/fdis-sdk-2.0.1.aar`
- `com.google.code.gson:gson:2.13.2`
- `com.squareup.okhttp3:okhttp:5.3.2`
- `androidx.work:work-runtime-ktx:2.11.2`

## Integration flow

`MainActivity` calls `FDIS.init(...)` with `syncEnabled = true` and `autoUpload = true`. The SDK shows the selected consent dialog during initialization.

When consent is granted, `onConsentGranted` runs in the host app. The app then requests `READ_SMS` and `RECEIVE_SMS`. After both permissions are granted, the app calls `FDIS.onSmsPermissionGranted(...)`.

Permission retry and settings fallback are handled in the app UI. `FDIS.clearData(...)` is used to clear local SDK state.

## Custom consent fields

The custom consent dialog accepts the following options.

| Field | Type |
| --- | --- |
| `backgroundColorHex` | `String` |
| `titleColorHex` | `String` |
| `messageColorHex` | `String` |
| `buttonColorHex` | `String` |
| `buttonCornerRadius` | `Float` |
| `iconColorHex` | `String` |
| `closeIconColorHex` | `String` |
| `titleText` | `String` |
| `introText` | `String` |
| `smsTitleText` | `String` |
| `smsDescText` | `String` |
| `privacyTitleText` | `String` |
| `privacyDescText` | `String` |
| `consentHtmlText` | `String` |
| `buttonText` | `String` |

## Backup exclusions

Android backup and device transfer exclude these SDK preference files:

- `client_prefs.xml`
- `fdis_sdk_prefs.xml`
- `uploader_prefs.xml`
- `sms_prefs.xml`
