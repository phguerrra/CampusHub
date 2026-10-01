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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class ProfileActivity extends AppCompatActivity {
    private TextInputLayout nameLayout;
    private TextInputEditText nameInput;
    private TextView emailText;
    private TextView profileError;
    private MaterialButton saveButton;
    private MaterialButton backButton;
    private ProgressBar profileProgress;

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        bindViews();
        setupActions();

        if (FirebaseApp.getApps(this).isEmpty()) {
            showError(getString(R.string.firebase_not_configured));
            saveButton.setEnabled(false);
            return;
        }
        auth = FirebaseAuth.getInstance();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (auth == null) {
            return;
        }
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            openLogin();
            return;
        }
        showUser(user);
    }

    private void bindViews() {
        nameLayout = findViewById(R.id.profile_name_layout);
        nameInput = findViewById(R.id.profile_name_input);
        emailText = findViewById(R.id.profile_email);
        profileError = findViewById(R.id.profile_error);
        saveButton = findViewById(R.id.save_profile_button);
        backButton = findViewById(R.id.back_to_events_button);
        profileProgress = findViewById(R.id.profile_progress);
    }

    private void setupActions() {
        saveButton.setOnClickListener(view -> saveProfile());
        backButton.setOnClickListener(view -> finish());
        nameInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveProfile();
                return true;
            }
            return false;
        });
    }

    private void showUser(FirebaseUser user) {
        nameInput.setText(user.getDisplayName() == null ? "" : user.getDisplayName());
        emailText.setText(user.getEmail() == null
                ? getString(R.string.email_not_available)
                : user.getEmail());
    }

    private void saveProfile() {
        FirebaseUser user = auth == null ? null : auth.getCurrentUser();
        if (user == null) {
            openLogin();
            return;
        }

        clearErrors();
        String name = textOf(nameInput).trim();
        String nameError = AuthInputValidator.validateName(name);
        nameLayout.setError(nameError);
        if (nameError != null) {
            return;
        }

        setLoading(true);
        UserProfileChangeRequest profile = new UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build();
        user.updateProfile(profile).addOnCompleteListener(this, task -> {
            setLoading(false);
            if (task.isSuccessful()) {
                nameInput.setText(name);
                Toast.makeText(this, R.string.profile_updated, Toast.LENGTH_SHORT).show();
            } else {
                showError(AuthErrorMessages.from(this, task.getException()));
            }
        });
    }

    private void setLoading(boolean loading) {
        profileProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        saveButton.setEnabled(!loading);
        backButton.setEnabled(!loading);
        nameInput.setEnabled(!loading);
    }

    private void clearErrors() {
        profileError.setVisibility(View.GONE);
        nameLayout.setError(null);
    }

    private void showError(String message) {
        profileError.setText(message);
        profileError.setVisibility(View.VISIBLE);
    }

    private void openLogin() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private static String textOf(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }
}
