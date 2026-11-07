package com.example.application;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {
    private TextInputLayout nameLayout;
    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private TextInputLayout roleLayout;

    private TextInputEditText nameInput;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private AutoCompleteTextView roleInput;
    private TextView authError;
    private MaterialButton registerButton;
    private MaterialButton backToLoginButton;
    private ProgressBar authProgress;

    private FirebaseAuth auth;
    private FirebaseFirestore database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        bindViews();
        setupRoleDropdown();
        setupActions();

        if (FirebaseApp.getApps(this).isEmpty()) {
            showConfigurationRequired();
            return;
        }
        auth = FirebaseAuth.getInstance();
        database = FirebaseFirestore.getInstance();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (auth != null && auth.getCurrentUser() != null) {
            openEvents();
        }
    }

    private void bindViews() {
        nameLayout = findViewById(R.id.name_layout);
        emailLayout = findViewById(R.id.email_layout);
        passwordLayout = findViewById(R.id.password_layout);
        roleLayout = findViewById(R.id.role_layout);

        nameInput = findViewById(R.id.name_input);
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        roleInput = findViewById(R.id.role_input);

        authError = findViewById(R.id.auth_error);
        registerButton = findViewById(R.id.register_button);
        backToLoginButton = findViewById(R.id.back_to_login_button);
        authProgress = findViewById(R.id.auth_progress);
    }

    private void setupRoleDropdown() {
        String[] roles = new String[]{
                getString(R.string.role_student),
                getString(R.string.role_organizer)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                roles
        );
        roleInput.setAdapter(adapter);
        roleInput.setText(roles[0], false);
    }

    private void setupActions() {
        registerButton.setOnClickListener(view -> createAccount());
        backToLoginButton.setOnClickListener(view -> finish());
        passwordInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                createAccount();
                return true;
            }
            return false;
        });
    }

    private void createAccount() {
        if (auth == null) {
            showConfigurationRequired();
            return;
        }
        clearErrors();

        String name = textOf(nameInput).trim();
        String email = textOf(emailInput).trim();
        String password = textOf(passwordInput);
        String roleText = roleInput.getText() != null ? roleInput.getText().toString().trim() : "";
        String selectedRole = getString(R.string.role_organizer).equalsIgnoreCase(roleText)
                ? User.ROLE_ORGANIZER
                : User.ROLE_STUDENT;

        String nameError = AuthInputValidator.validateName(name);
        String emailError = AuthInputValidator.validateEmail(email);
        String passwordError = AuthInputValidator.validatePassword(password);
        nameLayout.setError(nameError);
        emailLayout.setError(emailError);
        passwordLayout.setError(passwordError);
        if (nameError != null || emailError != null || passwordError != null) {
            return;
        }

        setLoading(true);
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    FirebaseUser user = auth.getCurrentUser();
                    if (!task.isSuccessful() || user == null) {
                        setLoading(false);
                        showError(AuthErrorMessages.from(this, task.getException()));
                        return;
                    }

                    UserProfileChangeRequest profile = new UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build();
                    user.updateProfile(profile).addOnCompleteListener(this, profileTask ->
                            saveUserToFirestore(user.getUid(), name, email, selectedRole));
                });
    }

    private void saveUserToFirestore(String userId, String name, String email, String role) {
        if (database == null) {
            setLoading(false);
            openEvents();
            return;
        }

        Map<String, Object> userData = new HashMap<>();
        userData.put("userId", userId);
        userData.put("name", name);
        userData.put("email", email);
        userData.put("role", role);
        userData.put("createdAt", FieldValue.serverTimestamp());

        database.collection("users")
                .document(userId)
                .set(userData)
                .addOnCompleteListener(task -> {
                    setLoading(false);
                    openEvents();
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
        registerButton.setEnabled(!loading);
        backToLoginButton.setEnabled(!loading);
        nameInput.setEnabled(!loading);
        emailInput.setEnabled(!loading);
        passwordInput.setEnabled(!loading);
        roleInput.setEnabled(!loading);
    }

    private void clearErrors() {
        authError.setVisibility(View.GONE);
        nameLayout.setError(null);
        emailLayout.setError(null);
        passwordLayout.setError(null);
        roleLayout.setError(null);
    }

    private void showConfigurationRequired() {
        showError(getString(R.string.firebase_not_configured));
        registerButton.setEnabled(false);
    }

    private void showError(String message) {
        authError.setText(message);
        authError.setVisibility(View.VISIBLE);
    }

    private static String textOf(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }
}
