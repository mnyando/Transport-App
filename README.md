# Transport App

An Android app for school transport and child safety. Schools manage their buses, routes, staff and students; drivers record pick-ups and drop-offs; parents follow their child's trips and get notified.

Built in Java with Firebase (Authentication, Cloud Firestore, Cloud Messaging).

## Features

The app opens on a landing screen. **Get Started** leads to a role picker (School, Parent, Driver) and then to login or sign-up. After login the user is sent to the dashboard for the role stored on their account.

| Role | What they can do |
|---|---|
| **School** | Add, modify and delete students, staff, vehicles and routes. View driver, student and route reports with charts. |
| **Driver** | See their assigned bus and route. Start a **pick-up** or **drop-off** trip, mark each child with large buttons, and complete the trip, which tells each parent the status actually recorded. An unfinished trip is resumed, not duplicated. The bus location is shared during the trip, and there is an SOS button. |
| **Parent** | See their child's details and trip status, follow the bus on a live map during a trip, read alerts, choose which routine alerts to get, and message the bus. |

## Tech stack

- Java 11, Android Gradle Plugin 8.13.2, Gradle 9.1.0 (wrapper included)
- `minSdk` 24 (Android 7.0), `compileSdk` / `targetSdk` 36
- AndroidX AppCompat, Material Components, ConstraintLayout, RecyclerView, SwipeRefreshLayout, Lifecycle (ViewModel/LiveData)
- Firebase, versioned by the Firebase BoM: Auth, Cloud Firestore, Cloud Messaging, Cloud Functions client
- [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart) for the report charts (from JitPack)
- [osmdroid](https://github.com/osmdroid/osmdroid) for the parent's live bus map (OpenStreetMap tiles)
- Every dependency version lives in `gradle/libs.versions.toml`

## Getting started

### Requirements

- Android Studio (recent stable version)
- **JDK 17 or 21 to run Gradle.** This Gradle version cannot run on JDK 25, which is what newer Android Studio versions bundle. In Android Studio, set **Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK** to a JDK 17 or 21. On the command line, point `JAVA_HOME` at one.
- Android SDK Platform 36 and Build-Tools 36. Gradle downloads them on first build if the SDK licenses are accepted.

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

The app reads and writes these Firestore collections: `users`, `staff`, `students/{grade}/studentList`, `archivedStudents`, `vehicle`, `Routes`, `trips`, `notifications`, `schoolAlerts`, `busGroups` (live bus location, driver and attendant names, bus chat) and `config/signup` (the parent sign-up code). All names are in `FirestorePaths.java`; `Routes` and `vehicle` keep their old spelling because existing data lives there. The `users` documents hold each account's `role` (`school`, `parent` or `driver`), which decides where login sends the user.

#### Accounts and access

The role always comes from `users/{uid}.role` on the server, never from the role picker, and `firestore.rules` enforces it:

- **Parents** sign up in the app with the school's **sign-up code** (role `parent`, with recorded consent to the privacy notice) and must verify their email. The school enters the parent's email on the student record. Cloud Functions then link the verified account to the child by account ID (`parentUids`), and parents can read only children linked that way.
- **Drivers** also sign up in the app, with the same sign-up code. The school then enters the driver's sign-in email on their staff record (Add/Modify Staff). The `linkStaffAccount` Cloud Function sets `role: driver` and `staffId` on the account, and the driver sees only the children, trips, alerts and chat of the bus on that staff record. Removing the email, setting the staff member to Not Active or deleting the record ends access.
- **School** accounts are set up by an administrator in the Firebase console (`role: school`).

Deploy the rules, indexes and functions together (functions need the Blaze plan):

```
cd functions && npm ci && cd ..
firebase deploy --only firestore:rules,firestore:indexes,functions
```

**First-time setup after deploying**

1. Sign in with a school account and tap **Parent sign-up code** on the School page to create a code. Until a code exists, nobody can sign up. Give it to parents and drivers, and change it if it gets out.
2. Existing parents are linked to their children the next time they open the app (the `linkMyChildren` function).
3. Existing driver accounts keep working only after their email is added to their staff record, because login no longer matches drivers by name.
4. Set `SCHOOL_TIME_ZONE` in `functions/index.js` to the school's time zone (it is `Africa/Johannesburg`), so the 18:00 end-of-day check runs at the right local time.
5. In Google Cloud Console, restrict the Firebase API key to this Android app (package name and signing certificate SHA-1).

**Cloud Functions** (`functions/index.js`)

| Function | What it does |
|---|---|
| `sendParentPush` | Sends each pick-up, drop-off and SOS alert to the parent's phones, respecting their alert settings. |
| `linkStaffAccount`, `onUserProfileCreated` | Link a driver's account to their staff record by email. |
| `linkMyChildren`, `onStudentWritten` | Link verified parents to their children by account ID, and keep each bus's `studentCount` correct. |
| `syncBusStaff` | Keeps the driver and attendant names on `busGroups/{bus}` for parents. |
| `notifyChatMessage` | Pushes a new bus-chat message to the other people in that bus's chat. |
| `endOfDayCheck` | At 18:00 on school days, alerts the school about any child picked up but not dropped off. |
| `clearStaleBusLocations` | Clears a bus position that has not been updated for 30 minutes. |

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

| Suite | What it covers | Run |
|---|---|---|
| `app/src/test` | ViewModel unit tests with fake repositories: the parent dashboard and the trip rules (explicit start, one trip at a time, resume, honest alerts). | `./gradlew testDebugUnitTest` |
| `app/src/androidTest` | The app launches and **Get Started** opens the role picker. | `./gradlew connectedAndroidTest` (emulator or device) |
| `functions/test` | Cloud Function helpers (time zones, missing drop-offs). | `cd functions && npm ci && npm test` |
| `rules-tests` | `firestore.rules` in the Firestore emulator: who can read and write what. | `cd rules-tests && npm ci && npm test` (needs Java 11+) |

GitHub Actions (`.github/workflows/ci.yml`) runs the build, the unit tests, the function tests and the rules tests on every push to `main` and every pull request. Lint also runs and its report is uploaded, but lint does not fail the build yet.

## Project structure

```
app/src/main/
├── AndroidManifest.xml
├── java/com/example/transportapp/
│   ├── MainActivity, RoleSelectActivity, Login, SignUp, PrivacyNotice   # entry flow and auth
│   ├── SchoolLanding, Add*/Modify*/Delete*           # school management screens
│   ├── Reports, *ReportActivity                      # reports and charts
│   ├── DriverLanding, TripActivity, TripViewModel, *TripRepository, TripLocationService   # driver screens
│   ├── ParentLanding, ParentDashboardViewModel, *ParentRepository, BusMapActivity, ChatActivity   # parent screens
│   ├── StudentRecords, InviteCodes, Session, ParentLinks, FirestorePaths, UserErrors   # shared helpers
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
- Staff phone numbers are not copied onto child records or shown to parents; parents use the bus chat. Driver and attendant names are kept on the bus record, not copied onto each child.
- Error messages shown to users never include raw exception text. Details go to the log only, and debug logs are stripped from release builds.
- The parent map uses OpenStreetMap's free tile servers, which are meant for light use. If the app grows, switch to a paid tile provider (`BusMapActivity.TILES`).
- Sign-up requires accepting the privacy notice (`privacy_notice_text` in `strings.xml`). The text is a draft: the school must fill in the bracketed details and check it against local data-protection law. Raise `PrivacyNotice.VERSION` when it changes.
