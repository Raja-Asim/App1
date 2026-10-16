[README.md](https://github.com/user-attachments/files/33566313/README.md)
# My Application

A Java Android app with a splash screen, Firebase-backed account access, and a simple signed-in home screen.

## Features

- Splash screen that checks the current Firebase session and routes to sign-in or the home screen.
- Create an account with a name, email address, password, and password confirmation.
- Sign in with email and password.
- Continue with Google using Android Credential Manager and Firebase Authentication.
- Send a password reset email.
- Sign out and clear the saved Google credential state.
- Save a user profile to Cloud Firestore at `users/{uid}`. Profiles contain `uid`, `name`, `email`, and `createdAt`. The app creates a profile after registration and creates one for existing accounts when needed.

## Built with

- Java 11
- Android Gradle Plugin 9.4.1
- Android SDK: compile SDK 37; minimum SDK 24
- Firebase Authentication and Cloud Firestore, managed with Firebase BoM 35.0.0
- AndroidX Credential Manager, Google ID, AppCompat, and Material Components

## Firebase setup

The project expects Firebase configuration at `app/google-services.json`. The current file must belong to the Firebase project where you enable Authentication and Firestore, and its Android app package must match:

```
com.example.myapplication
```

To configure Firebase:

1. In the Firebase Console, open the project that owns `app/google-services.json` and register an Android app with the package name above if it is not already registered.
2. Under **Authentication → Sign-in method**, enable **Email/Password** and **Google**.
3. In Firebase project settings, add the app's SHA-1 and SHA-256 signing certificate fingerprints. For local development, get the debug fingerprints from Android Studio's Gradle signing report.
4. After changing the app registration or OAuth setup, download the updated `google-services.json` and place it in the project's `app/` directory, replacing the old file. Sync the project with Gradle.
5. In **Authentication → Settings → Authorized domains**, make sure the domains required by your Firebase configuration are authorized. Google sign-in also requires the Firebase-generated web OAuth client ID, which the Google Services Gradle plugin exposes to the app.
6. In **Firestore Database**, create a database in the same Firebase project. The app creates the `users` collection and user documents when users register or sign in.

### Firestore security rules

The app reads and writes only each signed-in user's own `users/{uid}` profile document. A starting ruleset for that data shape is:

```text
rules_version = '2';

service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read: if request.auth != null
                  && request.auth.uid == userId;
      allow create, update: if request.auth != null
                            && request.auth.uid == userId
                            && request.resource.data.uid == request.auth.uid;
      allow delete: if false;
    }
  }
}
```

Publish the rules in **Firestore Database → Rules**. Do not leave Firestore in test mode for a production app. Do not put Firebase Admin SDK service-account keys in the Android app.

## Run the app

1. Open the project root in Android Studio.
2. Wait for Gradle sync to finish.
3. Complete the Firebase setup above.
4. Select an emulator or connected Android device and run the **app** configuration.

The app requires internet access for Firebase requests.

## Main app files

- `app/src/main/java/com/example/myapplication/MainActivity.java` — splash screen and initial session routing.
- `app/src/main/java/com/example/myapplication/LoginActivity.java` — email/password login, Google sign-in, and password reset.
- `app/src/main/java/com/example/myapplication/CreateAccountActivity.java` — account registration and initial profile save.
- `app/src/main/java/com/example/myapplication/HomeActivity.java` — signed-in screen and sign-out.
- `app/src/main/java/com/example/myapplication/AuthHelper.java` — Firebase initialization and Firestore profile helpers.


