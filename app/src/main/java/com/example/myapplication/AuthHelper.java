package com.example.myapplication;

import android.content.Context;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public final class AuthHelper {
    private AuthHelper() {
    }

    public static FirebaseAuth getAuthOrNull(Context context) {
        try {
            FirebaseApp app = FirebaseApp.initializeApp(context.getApplicationContext());
            return app == null ? null : FirebaseAuth.getInstance(app);
        } catch (IllegalStateException | IllegalArgumentException exception) {
            return null;
        }
    }

    public static Task<Void> saveNewUserProfile(FirebaseUser user, String displayName) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("uid", user.getUid());
        profile.put("name", displayName == null ? "" : displayName.trim());
        profile.put("email", user.getEmail());
        profile.put("createdAt", FieldValue.serverTimestamp());

        return FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .set(profile);
    }

    public static Task<Void> ensureUserProfile(FirebaseUser user) {
        DocumentReference profileDocument = FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid());

        return profileDocument.get().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                Exception exception = task.getException();
                return Tasks.forException(exception == null
                        ? new IllegalStateException("Could not read the Firestore user profile.")
                        : exception);
            }

            DocumentSnapshot snapshot = task.getResult();
            if (snapshot != null && snapshot.exists()) {
                return Tasks.forResult(null);
            }
            return saveNewUserProfile(user, user.getDisplayName());
        });
    }
}
