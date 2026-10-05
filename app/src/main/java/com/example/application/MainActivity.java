package com.example.application;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {
    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private TextView authError;
    private MaterialButton loginButton;
    private MaterialButton openRegisterButton;
    private MaterialButton forgotPasswordButton;
    private ProgressBar authProgress;

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindViews();
        setupActions();

        if (FirebaseApp.getApps(this).isEmpty()) {
            showConfigurationRequired();
            return;
        }
        auth = FirebaseAuth.getInstance();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (auth != null && auth.getCurrentUser() != null) {
            openEvents();
        }
    }

    private void bindViews() {
        emailLayout = findViewById(R.id.email_layout);
        passwordLayout = findViewById(R.id.password_layout);
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        authError = findViewById(R.id.auth_error);
        loginButton = findViewById(R.id.login_button);
        openRegisterButton = findViewById(R.id.open_register_button);
        forgotPasswordButton = findViewById(R.id.forgot_password_button);
        authProgress = findViewById(R.id.auth_progress);
    }

    private void setupActions() {
        loginButton.setOnClickListener(view -> signIn());
        openRegisterButton.setOnClickListener(view ->
                startActivity(new Intent(this, RegisterActivity.class)));
        forgotPasswordButton.setOnClickListener(view -> resetPassword());
        passwordInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                signIn();
                return true;
            }
            return false;
        });
    }

    private void signIn() {
        if (auth == null) {
            showConfigurationRequired();
            return;
        }
        clearErrors();

        String email = textOf(emailInput).trim();
        String password = textOf(passwordInput);
        String emailError = AuthInputValidator.validateEmail(email);
        String passwordError = AuthInputValidator.validatePassword(password);
        emailLayout.setError(emailError);
        passwordLayout.setError(passwordError);
        if (emailError != null || passwordError != null) {
            return;
        }

        setLoading(true);
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);
                    if (task.isSuccessful() && auth.getCurrentUser() != null) {
                        passwordInput.setText("");
                        openEvents();
                    } else {
                        showError(AuthErrorMessages.from(this, task.getException()));
                    }
                });
    }

    private void resetPassword() {
        if (auth == null) {
            showConfigurationRequired();
            return;
        }
        clearErrors();

        String email = textOf(emailInput).trim();
        String emailError = AuthInputValidator.validateEmail(email);
        if (emailError != null) {
            emailLayout.setError(emailError);
            return;
        }

        setLoading(true);
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        Toast.makeText(this, R.string.reset_email_sent, Toast.LENGTH_LONG).show();
                    } else {
                        showError(AuthErrorMessages.from(this, task.getException()));
                    }
                });
    }

    private void openEvents() {
        Intent intent = new Intent(this, EventsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean loading) {
        authProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        loginButton.setEnabled(!loading);
        openRegisterButton.setEnabled(!loading);
        forgotPasswordButton.setEnabled(!loading);
        emailInput.setEnabled(!loading);
        passwordInput.setEnabled(!loading);
    }

    private void clearErrors() {
        authError.setVisibility(View.GONE);
        emailLayout.setError(null);
        passwordLayout.setError(null);
    }

    private void showConfigurationRequired() {
        showError(getString(R.string.firebase_not_configured));
        loginButton.setEnabled(false);
        forgotPasswordButton.setEnabled(false);
    }

    private void showError(String message) {
        authError.setText(message);
        authError.setVisibility(View.VISIBLE);
    }

    private static String textOf(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }
}
