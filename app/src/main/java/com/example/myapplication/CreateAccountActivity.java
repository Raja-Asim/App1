package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class CreateAccountActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private TextInputLayout nameLayout;
    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private TextInputLayout confirmPasswordLayout;
    private TextInputEditText nameInput;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private TextInputEditText confirmPasswordInput;
    private Button createAccountButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_account);

        View root = findViewById(R.id.create_account_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        auth = AuthHelper.getAuthOrNull(this);
        nameLayout = findViewById(R.id.name_layout);
        emailLayout = findViewById(R.id.register_email_layout);
        passwordLayout = findViewById(R.id.register_password_layout);
        confirmPasswordLayout = findViewById(R.id.confirm_password_layout);
        nameInput = findViewById(R.id.name_input);
        emailInput = findViewById(R.id.register_email_input);
        passwordInput = findViewById(R.id.register_password_input);
        confirmPasswordInput = findViewById(R.id.confirm_password_input);
        createAccountButton = findViewById(R.id.create_account_button);
        TextView signInLink = findViewById(R.id.back_to_sign_in);

        createAccountButton.setOnClickListener(view -> createAccount());
        signInLink.setOnClickListener(view -> finish());
    }

    private void createAccount() {
        String name = textOf(nameInput).trim();
        String email = textOf(emailInput).trim();
        String password = textOf(passwordInput);
        String confirmPassword = textOf(confirmPasswordInput);

        boolean valid = true;
        if (name.isEmpty()) {
            nameLayout.setError(getString(R.string.error_name));
            valid = false;
        } else {
            nameLayout.setError(null);
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError(getString(R.string.error_email));
            valid = false;
        } else {
            emailLayout.setError(null);
        }

        if (password.length() < 6) {
            passwordLayout.setError(getString(R.string.error_password));
            valid = false;
        } else {
            passwordLayout.setError(null);
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordLayout.setError(getString(R.string.error_password_mismatch));
            valid = false;
        } else {
            confirmPasswordLayout.setError(null);
        }

        if (!valid) {
            return;
        }
        if (auth == null) {
            Toast.makeText(this, R.string.firebase_setup_required, Toast.LENGTH_LONG).show();
            return;
        }

        createAccountButton.setEnabled(false);
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            if (!task.isSuccessful()) {
                createAccountButton.setEnabled(true);
                Exception exception = task.getException();
                int message = R.string.account_create_failed;
                if (exception instanceof FirebaseAuthUserCollisionException) {
                    message = R.string.account_email_in_use;
                } else if (exception instanceof FirebaseAuthWeakPasswordException) {
                    message = R.string.error_password;
                } else if (exception instanceof FirebaseNetworkException) {
                    message = R.string.network_unavailable;
                }
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                return;
            }

            FirebaseUser user = auth.getCurrentUser();
            if (user == null) {
                openHome();
                return;
            }

            UserProfileChangeRequest profile = new UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build();
            user.updateProfile(profile).addOnCompleteListener(this, profileTask -> AuthHelper.saveNewUserProfile(user, name)
                    .addOnCompleteListener(this, saveTask -> {
                        createAccountButton.setEnabled(true);
                        if (!profileTask.isSuccessful() || !saveTask.isSuccessful()) {
                            if (!saveTask.isSuccessful() && saveTask.getException() != null) {
                                Log.e("Firestore", "Could not save the new user profile.",
                                        saveTask.getException());
                            }
                            Toast.makeText(this, R.string.profile_update_failed,
                                    Toast.LENGTH_LONG).show();
                        }
                        openHome();
                    }));
        });
    }

    private String textOf(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }

    private void openHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}


