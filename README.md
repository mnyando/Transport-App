# Transport App

An Android app for school transport and child safety. Schools manage their buses, routes, staff and students; drivers record pick-ups and drop-offs; parents follow their child's trips and get notified.

Built in Java with Firebase (Authentication, Cloud Firestore, Cloud Messaging).

## Features

The app opens on a landing screen. **Get Started** leads to a role picker (School, Parent, Driver) and then to login or sign-up. After login the user is sent to the dashboard for the role stored on their account.

| Role | What they can do |
|---|---|
| **School** | Add, modify and delete students, staff, vehicles and routes. View driver, student and route reports with charts. |
| **Driver** | See their assigned vehicle and route. Record student **pick-up** and **drop-off**, which logs the trip and notifies the parent. |
| **Parent** | See their child's details and trip status, read notifications, and chat. |

## Tech stack

- Java 11, Android Gradle Plugin 8.8.0, Gradle 8.10.2 (wrapper included)
- `minSdk` 24 (Android 7.0), `compileSdk` / `targetSdk` 35
- AndroidX AppCompat, Material Components, ConstraintLayout, RecyclerView, CardView
- Firebase Auth, Cloud Firestore, Firebase Cloud Messaging
- [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart) for the report charts (from JitPack)

## Getting started

### Requirements

- Android Studio (recent stable version)
- **JDK 17 or 21 to run Gradle.** Gradle 8.10.2 cannot run on JDK 25, which is what newer Android Studio versions bundle. In Android Studio, set **Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK** to a JDK 17 or 21. On the command line, point `JAVA_HOME` at one.
- Android SDK Platform 35 and Build-Tools 35. Gradle downloads them on first build if the SDK licenses are accepted.

### Build and run

1. Clone the repo and open the folder in Android Studio.
2. Let Gradle sync. Android Studio creates `local.properties` with your SDK path. This file is git-ignored; on the command line you can set `ANDROID_HOME` instead.
3. Run the **app** configuration on an emulator or a device.

From the command line:

```bash
./gradlew assembleDebug            # debug APK  -> app/build/outputs/apk/debug/
./gradlew assembleRelease          # release APK (minified with R8)
./gradlew connectedAndroidTest     # instrumented tests, needs a running emulator/device
```

On Windows use `gradlew.bat` instead of `./gradlew`.

### Firebase

`app/google-services.json` connects the app to the existing Firebase project and is registered for the package `com.example.transportapp`. To use your own Firebase project:

1. Create a project in the [Firebase console](https://console.firebase.google.com/) and add an Android app with the app's `applicationId`.
2. Enable **Email/Password** sign-in under Authentication, and create a **Cloud Firestore** database.
3. Download the new `google-services.json` and replace `app/google-services.json`.

The app reads and writes these Firestore collections: `users`, `staff`, `students/{grade}/studentList`, `vehicle`, `Routes`, `routes`, `trips`, `attendance`, `notifications`, `schoolAlerts` and `busGroups` (live bus location and bus chat). The `users` documents hold each account's `role` (`school`, `parent` or `driver`), which decides where login sends the user.

#### Accounts and access

The role always comes from `users/{uid}.role` on the server, never from the role picker, and `firestore.rules` enforces it:

- **Parents** sign up in the app (role `parent`, with recorded consent to the privacy notice), must verify their email, and see only the children whose record holds that email.
- **Drivers** also sign up in the app. The school then enters the driver's sign-in email on their staff record (Add/Modify Staff). The `linkStaffAccount` Cloud Function sets `role: driver` and `staffId` on the account, and the driver sees only the children, trips, alerts and chat of the bus on that staff record. Removing the email, setting the staff member to Not Active or deleting the record ends access.
- **School** accounts are set up by an administrator in the Firebase console (`role: school`).

Deploy the rules, indexes and functions together (functions need the Blaze plan):

```
firebase deploy --only firestore:rules,firestore:indexes,functions
```

Existing driver accounts keep working only after their email is added to their staff record, because login no longer matches drivers by name.

## Release builds

Release builds use R8 code minification and resource shrinking. The Firestore model classes (`Driver`, `Notification`, `Route`, `Staff`, `Student`, `Vehicle`) are kept in `app/proguard-rules.pro` because Firestore maps documents onto them by reflection. Add any new Firestore model class there too.

### Signing

Without a signing config, `assembleRelease` produces an unsigned APK. To sign it, create `keystore.properties` in the project root:

```properties
storeFile=C:/path/to/release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

`keystore.properties`, `*.jks` and `*.keystore` are git-ignored. Never commit them.

### Before publishing to Google Play

- **Change the package name.** Google Play rejects `com.example.*`. Changing `applicationId` in `app/build.gradle.kts` also requires registering the new package in Firebase and replacing `google-services.json`, or login and data access will stop working.
- Bump `versionCode` and `versionName` for every release.
- Test the minified release build on a real device, especially login and the Firestore-backed lists.

## Testing

`app/src/androidTest/.../LaunchAndNavigationTest.java` checks that the app launches and that **Get Started** opens the role picker. This catches problems such as an activity missing from the manifest. Run it with `./gradlew connectedAndroidTest` while an emulator or device is connected.

`app/src/test/.../ParentDashboardViewModelTest.java` unit-tests the parent dashboard's ViewModel with a fake repository. Run it with `./gradlew testDebugUnitTest`.

## Project structure

```
app/src/main/
├── AndroidManifest.xml
├── java/com/example/transportapp/
│   ├── MainActivity, RoleSelectActivity, Login, SignUp, PrivacyNotice   # entry flow and auth
│   ├── SchoolLanding, Add*/Modify*/Delete*           # school management screens
│   ├── Reports, *ReportActivity                      # reports and charts
│   ├── DriverLanding, PickUp, DropOff, TripLocationService   # driver screens
│   ├── ParentLanding, ParentDashboardViewModel, *ParentRepository, ChatActivity   # parent screens
│   ├── Student, Staff, Driver, Vehicle, Route, Notification   # Firestore models
│   └── *Adapter                                      # RecyclerView adapters
└── res/
    ├── layout/        # screen layouts; toolbar.xml and footer.xml are shared with <include>
    ├── drawable/      # images and vector icons
    ├── mipmap-*/      # launcher icon
    └── xml/           # backup, data extraction and network security rules
```

Screen titles are each activity's `android:label` in the manifest; `TransportApp` shows it in the shared toolbar. Text and colours live in `values/strings.xml` and `values/colors.xml`, not in layouts.

## Privacy

The app handles children's personal data. Android backup is disabled (`allowBackup="false"`), and the backup and data extraction rules exclude all app data from cloud backup and device-to-device transfer.

- Only HTTPS traffic is allowed (`res/xml/network_security_config.xml`).
- The bus location is stored only while a trip is running. The app deletes it when the trip ends, and the `clearStaleBusLocations` function clears any position not updated for 30 minutes (for example when the driver's phone goes offline).
- Staff phone numbers are not copied onto child records or shown to parents; parents use the bus chat.
- Sign-up requires accepting the privacy notice (`privacy_notice_text` in `strings.xml`). The text is a draft: the school must fill in the bracketed details and check it against local data-protection law. Raise `PrivacyNotice.VERSION` when it changes.
