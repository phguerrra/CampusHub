package com.example.application;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseApp;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CreateEventActivity extends AppCompatActivity {
    private static final String TAG = "CreateEventActivity";
    private TextInputLayout nameLayout;
    private TextInputLayout descriptionLayout;
    private TextInputLayout dateLayout;
    private TextInputLayout timeLayout;
    private TextInputLayout locationLayout;
    private TextInputLayout slotsLayout;
    private TextInputEditText nameInput;
    private TextInputEditText descriptionInput;
    private TextInputEditText dateInput;
    private TextInputEditText timeInput;
    private TextInputEditText locationInput;
    private TextInputEditText slotsInput;
    private TextView errorText;
    private MaterialButton saveButton;
    private MaterialButton cancelButton;
    private ProgressBar progress;

    private FirebaseAuth auth;
    private FirebaseFirestore database;
    private Calendar selectedDate;
    private String selectedTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);
        bindViews();
        setupActions();

        if (FirebaseApp.getApps(this).isEmpty()) {
            showError(getString(R.string.firebase_not_configured));
            saveButton.setEnabled(false);
            return;
        }
        auth = FirebaseAuth.getInstance();
        database = FirebaseFirestore.getInstance();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (auth != null && auth.getCurrentUser() == null) {
            openLogin();
        }
    }

    private void bindViews() {
        nameLayout = findViewById(R.id.create_event_name_layout);
        descriptionLayout = findViewById(R.id.create_event_description_layout);
        dateLayout = findViewById(R.id.create_event_date_layout);
        timeLayout = findViewById(R.id.create_event_time_layout);
        locationLayout = findViewById(R.id.create_event_location_layout);
        slotsLayout = findViewById(R.id.create_event_slots_layout);
        nameInput = findViewById(R.id.create_event_name_input);
        descriptionInput = findViewById(R.id.create_event_description_input);
        dateInput = findViewById(R.id.create_event_date_input);
        timeInput = findViewById(R.id.create_event_time_input);
        locationInput = findViewById(R.id.create_event_location_input);
        slotsInput = findViewById(R.id.create_event_slots_input);
        errorText = findViewById(R.id.create_event_error);
        saveButton = findViewById(R.id.save_event_button);
        cancelButton = findViewById(R.id.cancel_create_event_button);
        progress = findViewById(R.id.create_event_progress);
    }

    private void setupActions() {
        dateInput.setOnClickListener(view -> showDatePicker());
        timeInput.setOnClickListener(view -> showTimePicker());
        saveButton.setOnClickListener(view -> createEvent());
        cancelButton.setOnClickListener(view -> finish());
    }

    private void showDatePicker() {
        Calendar initial = selectedDate == null ? Calendar.getInstance() : selectedDate;
        new DatePickerDialog(
                this,
                (picker, year, month, day) -> {
                    selectedDate = Calendar.getInstance();
                    selectedDate.clear();
                    selectedDate.set(year, month, day, 0, 0, 0);
                    DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    dateInput.setText(formatter.format(selectedDate.getTime()));
                    dateLayout.setError(null);
                },
                initial.get(Calendar.YEAR),
                initial.get(Calendar.MONTH),
                initial.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void showTimePicker() {
        Calendar now = Calendar.getInstance();
        new TimePickerDialog(
                this,
                (picker, hour, minute) -> {
                    selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
                    timeInput.setText(selectedTime);
                    timeLayout.setError(null);
                },
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                true
        ).show();
    }

    private void createEvent() {
        if (auth == null || auth.getCurrentUser() == null || database == null) {
            openLogin();
            return;
        }

        clearErrors();
        String name = textOf(nameInput).trim();
        String description = textOf(descriptionInput).trim();
        String location = textOf(locationInput).trim();
        String slotsText = textOf(slotsInput).trim();

        boolean valid = true;
        if (name.length() < 2) {
            nameLayout.setError(getString(R.string.event_name_required));
            valid = false;
        }
        if (description.length() < 10) {
            descriptionLayout.setError(getString(R.string.event_description_required));
            valid = false;
        }
        if (selectedDate == null) {
            dateLayout.setError(getString(R.string.event_date_required));
            valid = false;
        }
        if (selectedTime == null) {
            timeLayout.setError(getString(R.string.event_time_required));
            valid = false;
        }
        if (location.length() < 2) {
            locationLayout.setError(getString(R.string.event_location_required));
            valid = false;
        }

        long availableSlots = parsePositiveLong(slotsText);
        if (availableSlots <= 0) {
            slotsLayout.setError(getString(R.string.event_slots_required));
            valid = false;
        }
        if (!valid) {
            return;
        }

        Map<String, Object> event = new HashMap<>();
        event.put("name", name);
        event.put("description", description);
        event.put("date", new Timestamp(selectedDate.getTime()));
        event.put("time", selectedTime);
        event.put("location", location);
        event.put("availableSlots", availableSlots);
        event.put("createdBy", auth.getCurrentUser().getUid());
        event.put("createdAt", FieldValue.serverTimestamp());

        setLoading(true);
        database.collection("events")
                .add(event)
                .addOnSuccessListener(document -> {
                    setLoading(false);
                    Toast.makeText(this, R.string.event_created, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(exception -> {
                    setLoading(false);
                    Log.e(TAG, "Falha ao salvar evento no Firestore", exception);
                    showError(firestoreErrorMessage(exception));
                });
    }

    private String firestoreErrorMessage(Exception exception) {
        if (!(exception instanceof FirebaseFirestoreException)) {
            return getString(R.string.event_create_error);
        }

        FirebaseFirestoreException firestoreException =
                (FirebaseFirestoreException) exception;
        switch (firestoreException.getCode()) {
            case PERMISSION_DENIED:
            case UNAUTHENTICATED:
                return getString(R.string.firestore_permission_denied);
            case UNAVAILABLE:
            case DEADLINE_EXCEEDED:
                return getString(R.string.firestore_unavailable);
            default:
                return getString(
                        R.string.firestore_error_with_code,
                        firestoreException.getCode().name()
                );
        }
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        saveButton.setEnabled(!loading);
        cancelButton.setEnabled(!loading);
        nameInput.setEnabled(!loading);
        descriptionInput.setEnabled(!loading);
        dateInput.setEnabled(!loading);
        timeInput.setEnabled(!loading);
        locationInput.setEnabled(!loading);
        slotsInput.setEnabled(!loading);
    }

    private void clearErrors() {
        errorText.setVisibility(View.GONE);
        nameLayout.setError(null);
        descriptionLayout.setError(null);
        dateLayout.setError(null);
        timeLayout.setError(null);
        locationLayout.setError(null);
        slotsLayout.setError(null);
    }

    private void showError(String message) {
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
    }

    private void openLogin() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private static long parsePositiveLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    private static String textOf(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }
}
