package com.example.myapplication;

import static com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL;

import android.content.Intent;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private Button signInButton;
    private Button googleSignInButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        View root = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        auth = AuthHelper.getAuthOrNull(this);
        emailLayout = findViewById(R.id.email_layout);
        passwordLayout = findViewById(R.id.password_layout);
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        signInButton = findViewById(R.id.sign_in_button);
        TextView forgotPassword = findViewById(R.id.forgot_password);
        TextView createAccount = findViewById(R.id.create_account);
        googleSignInButton = findViewById(R.id.google_sign_in_button);

        signInButton.setOnClickListener(view -> signInWithEmail());
        googleSignInButton.setOnClickListener(view -> startGoogleSignIn());
        forgotPassword.setOnClickListener(view -> sendPasswordReset());
        createAccount.setOnClickListener(view ->
                startActivity(new Intent(this, CreateAccountActivity.class)));
    }

    private void signInWithEmail() {
        String email = textOf(emailInput).trim();
        String password = textOf(passwordInput);

        boolean valid = true;
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

        if (!valid || !requireFirebase()) {
            return;
        }

        signInButton.setEnabled(false);
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            signInButton.setEnabled(true);
            if (task.isSuccessful()) {
                FirebaseUser user = auth.getCurrentUser();
                if (user != null) {
                    syncUserProfile(user);
                }
                openHome();
            } else {
                Exception exception = task.getException();
                if (exception instanceof FirebaseNetworkException) {
                    Toast.makeText(this, R.string.network_unavailable, Toast.LENGTH_LONG).show();
                } else if (exception instanceof FirebaseAuthInvalidUserException
                        || exception instanceof FirebaseAuthInvalidCredentialsException) {
                    Toast.makeText(this, R.string.sign_in_failed, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, R.string.auth_failed, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void sendPasswordReset() {
        String email = textOf(emailInput).trim();
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError(getString(R.string.error_email));
            return;
        }
        emailLayout.setError(null);
        if (!requireFirebase()) {
            return;
        }

        auth.sendPasswordResetEmail(email).addOnCompleteListener(this, task -> {
            int message = task.isSuccessful()
                    ? R.string.password_reset_sent
                    : R.string.auth_failed;
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });
    }

    private void startGoogleSignIn() {
        if (!requireFirebase()) {
            return;
        }

        int clientIdResource = getResources().getIdentifier(
                "default_web_client_id", "string", getPackageName());
        if (clientIdResource == 0 || getString(clientIdResource).trim().isEmpty()) {
            Toast.makeText(this, R.string.google_setup_required, Toast.LENGTH_LONG).show();
            return;
        }

        String webClientId = getString(clientIdResource);
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .build();
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        CredentialManager credentialManager = CredentialManager.create(this);
        googleSignInButton.setEnabled(false);
        credentialManager.getCredentialAsync(
                this,
                request,
                new CancellationSignal(),
                ContextCompat.getMainExecutor(this),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        googleSignInButton.setEnabled(true);
                        handleGoogleCredential(result.getCredential());
                    }

                    @Override
                    public void onError(GetCredentialException exception) {
                        googleSignInButton.setEnabled(true);
                        Toast.makeText(LoginActivity.this,
                                R.string.google_sign_in_failed, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void handleGoogleCredential(Credential credential) {
        if (!(credential instanceof CustomCredential)
                || !TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType())) {
            Toast.makeText(this, R.string.google_sign_in_failed, Toast.LENGTH_LONG).show();
            return;
        }

        GoogleIdTokenCredential googleCredential;
        try {
            googleCredential = GoogleIdTokenCredential.createFrom(
                    ((CustomCredential) credential).getData());
        } catch (IllegalArgumentException | NullPointerException exception) {
            Toast.makeText(this, R.string.google_sign_in_failed, Toast.LENGTH_LONG).show();
            return;
        }
        auth.signInWithCredential(
                        GoogleAuthProvider.getCredential(googleCredential.getIdToken(), null))
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            syncUserProfile(user);
                        }
                        openHome();
                    } else {
                        Toast.makeText(this, R.string.auth_failed, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void syncUserProfile(FirebaseUser user) {
        AuthHelper.ensureUserProfile(user).addOnFailureListener(exception -> {
            Log.e("Firestore", "Could not sync the signed-in user's profile.", exception);
            Toast.makeText(this, R.string.firestore_sync_failed, Toast.LENGTH_LONG).show();
        });
    }

    private boolean requireFirebase() {
        if (auth != null) {
            return true;
        }
        Toast.makeText(this, R.string.firebase_setup_required, Toast.LENGTH_LONG).show();
        return false;
    }

    private void openHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private String textOf(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }
}
