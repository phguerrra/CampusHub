package com.example.application;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class EventDetailsActivity extends AppCompatActivity {
    private TextView nameText;
    private TextView dateTimeText;
    private TextView locationText;
    private TextView descriptionText;
    private TextView availableSlotsText;
    private TextView errorText;
    private View detailsContent;
    private ProgressBar progress;
    private MaterialButton retryButton;

    private FirebaseAuth auth;
    private FirebaseFirestore database;
    private String eventId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);
        bindViews();

        findViewById(R.id.event_details_back_button).setOnClickListener(view -> finish());
        retryButton.setOnClickListener(view -> loadEvent());

        eventId = getIntent().getStringExtra(EventsActivity.EXTRA_EVENT_ID);
        if (eventId == null || eventId.trim().isEmpty()) {
            showError(getString(R.string.event_details_invalid_id), false);
            return;
        }
        if (FirebaseApp.getApps(this).isEmpty()) {
            showError(getString(R.string.firebase_not_configured), false);
            return;
        }

        auth = FirebaseAuth.getInstance();
        database = FirebaseFirestore.getInstance();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (auth == null) {
            return;
        }
        if (auth.getCurrentUser() == null) {
            openLogin();
        } else {
            loadEvent();
        }
    }

    private void bindViews() {
        nameText = findViewById(R.id.event_details_name);
        dateTimeText = findViewById(R.id.event_details_date_time);
        locationText = findViewById(R.id.event_details_location);
        descriptionText = findViewById(R.id.event_details_description);
        availableSlotsText = findViewById(R.id.event_details_available_slots);
        errorText = findViewById(R.id.event_details_error);
        detailsContent = findViewById(R.id.event_details_content);
        progress = findViewById(R.id.event_details_progress);
        retryButton = findViewById(R.id.event_details_retry_button);
    }

    private void loadEvent() {
        showLoading();
        database.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        showError(getString(R.string.event_details_not_found), false);
                        return;
                    }
                    Event event = document.toObject(Event.class);
                    if (event == null) {
                        showError(getString(R.string.event_details_load_error), true);
                        return;
                    }
                    event.setId(document.getId());
                    showEvent(event);
                })
                .addOnFailureListener(exception ->
                        showError(getString(R.string.event_details_load_error), true));
    }

    private void showLoading() {
        progress.setVisibility(View.VISIBLE);
        detailsContent.setVisibility(View.GONE);
        errorText.setVisibility(View.GONE);
        retryButton.setVisibility(View.GONE);
    }

    private void showEvent(Event event) {
        progress.setVisibility(View.GONE);
        errorText.setVisibility(View.GONE);
        retryButton.setVisibility(View.GONE);
        detailsContent.setVisibility(View.VISIBLE);

        nameText.setText(valueOrFallback(event.getName()));
        dateTimeText.setText(getString(
                R.string.event_date_time,
                formatDate(event.getDate()),
                valueOrFallback(event.getTime())));
        locationText.setText(valueOrFallback(event.getLocation()));
        descriptionText.setText(valueOrFallback(event.getDescription()));
        availableSlotsText.setText(getResources().getQuantityString(
                R.plurals.event_available_slots,
                (int) event.getAvailableSlots(),
                event.getAvailableSlots()));
    }

    private void showError(String message, boolean canRetry) {
        progress.setVisibility(View.GONE);
        detailsContent.setVisibility(View.GONE);
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
        retryButton.setVisibility(canRetry ? View.VISIBLE : View.GONE);
    }

    private String formatDate(Timestamp timestamp) {
        if (timestamp == null) {
            return getString(R.string.event_date_unavailable);
        }
        DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return formatter.format(timestamp.toDate());
    }

    private String valueOrFallback(String value) {
        return value == null || value.trim().isEmpty()
                ? getString(R.string.event_information_unavailable)
                : value;
    }

    private void openLogin() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
