package com.example.application;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EventDetailsActivity extends AppCompatActivity {
    private static final String TAG = "EventDetailsActivity";

    private TextView nameText;
    private TextView dateTimeText;
    private TextView locationText;
    private TextView descriptionText;
    private TextView availableSlotsText;
    private TextView errorText;
    private View detailsContent;
    private ProgressBar progress;
    private MaterialButton retryButton;
    private MaterialButton subscribeButton;
    private ProgressBar subscribeProgress;

    private FirebaseAuth auth;
    private FirebaseFirestore database;
    private String eventId;
    private Event currentEvent;
    private boolean isSubscribed = false;

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
        subscribeButton = findViewById(R.id.event_details_subscribe_button);
        subscribeProgress = findViewById(R.id.event_details_subscribe_progress);
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
                    currentEvent = event;
                    showEvent(event);
                    checkSubscriptionStatus();
                })
                .addOnFailureListener(exception -> {
                    Log.e(TAG, "Erro ao carregar detalhes do evento", exception);
                    showError(getString(R.string.event_details_load_error), true);
                });
    }

    private void checkSubscriptionStatus() {
        if (auth == null || auth.getCurrentUser() == null || database == null) {
            return;
        }
        String userId = auth.getCurrentUser().getUid();
        String subscriptionDocId = eventId + "_" + userId;

        database.collection("subscriptions")
                .document(subscriptionDocId)
                .get()
                .addOnSuccessListener(document -> {
                    isSubscribed = document.exists();
                    updateSubscribeButtonUI();
                })
                .addOnFailureListener(exception -> {
                    Log.e(TAG, "Erro ao checar status de inscrição", exception);
                    isSubscribed = false;
                    updateSubscribeButtonUI();
                });
    }

    private void updateSubscribeButtonUI() {
        setSubscribeLoading(false);
        if (currentEvent == null) {
            return;
        }

        if (isSubscribed) {
            subscribeButton.setText(R.string.unsubscribe_from_event);
            subscribeButton.setEnabled(true);
            subscribeButton.setOnClickListener(v -> unsubscribeFromEvent());
        } else {
            if (currentEvent.getAvailableSlots() > 0) {
                subscribeButton.setText(R.string.subscribe_to_event);
                subscribeButton.setEnabled(true);
                subscribeButton.setOnClickListener(v -> subscribeToEvent());
            } else {
                subscribeButton.setText(R.string.no_slots_available);
                subscribeButton.setEnabled(false);
                subscribeButton.setOnClickListener(null);
            }
        }
    }

    private void subscribeToEvent() {
        if (auth == null || auth.getCurrentUser() == null || database == null) {
            openLogin();
            return;
        }

        if (currentEvent != null && currentEvent.getAvailableSlots() <= 0) {
            Toast.makeText(this, R.string.no_slots_left, Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        String subscriptionDocId = eventId + "_" + userId;

        setSubscribeLoading(true);

        DocumentReference eventRef = database.collection("events").document(eventId);
        DocumentReference subRef = database.collection("subscriptions").document(subscriptionDocId);

        Map<String, Object> subData = new HashMap<>();
        subData.put("eventId", eventId);
        subData.put("userId", userId);
        subData.put("subscribedAt", FieldValue.serverTimestamp());

        subRef.set(subData)
                .addOnSuccessListener(aVoid -> {
                    eventRef.update("availableSlots", FieldValue.increment(-1))
                            .addOnSuccessListener(aVoid2 -> {
                                Toast.makeText(this, R.string.subscribe_success, Toast.LENGTH_SHORT).show();
                                loadEvent();
                            })
                            .addOnFailureListener(exception -> {
                                Log.e(TAG, "Erro ao decrementar vagas", exception);
                                Toast.makeText(this, R.string.subscribe_success, Toast.LENGTH_SHORT).show();
                                loadEvent();
                            });
                })
                .addOnFailureListener(exception -> {
                    setSubscribeLoading(false);
                    Log.e(TAG, "Erro ao realizar inscrição no Firestore", exception);
                    String errorMsg = handleSubscribeError(exception);
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                });
    }

    private void unsubscribeFromEvent() {
        if (auth == null || auth.getCurrentUser() == null || database == null) {
            openLogin();
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        String subscriptionDocId = eventId + "_" + userId;

        setSubscribeLoading(true);

        DocumentReference eventRef = database.collection("events").document(eventId);
        DocumentReference subRef = database.collection("subscriptions").document(subscriptionDocId);

        subRef.delete()
                .addOnSuccessListener(aVoid -> {
                    eventRef.update("availableSlots", FieldValue.increment(1))
                            .addOnSuccessListener(aVoid2 -> {
                                Toast.makeText(this, R.string.unsubscribe_success, Toast.LENGTH_SHORT).show();
                                loadEvent();
                            })
                            .addOnFailureListener(exception -> {
                                Log.e(TAG, "Erro ao incrementar vagas", exception);
                                Toast.makeText(this, R.string.unsubscribe_success, Toast.LENGTH_SHORT).show();
                                loadEvent();
                            });
                })
                .addOnFailureListener(exception -> {
                    setSubscribeLoading(false);
                    Log.e(TAG, "Erro ao cancelar inscrição no Firestore", exception);
                    String errorMsg = handleSubscribeError(exception);
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                });
    }

    private String handleSubscribeError(Exception exception) {
        if (exception instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException firestoreException = (FirebaseFirestoreException) exception;
            switch (firestoreException.getCode()) {
                case NOT_FOUND:
                    return getString(R.string.event_details_not_found);
                case ABORTED:
                    return getString(R.string.no_slots_left);
                case ALREADY_EXISTS:
                    return getString(R.string.already_subscribed);
                case PERMISSION_DENIED:
                case UNAUTHENTICATED:
                    return getString(R.string.firestore_permission_denied);
                case UNAVAILABLE:
                    return getString(R.string.firestore_unavailable);
                default:
                    return getString(R.string.firestore_error_with_code, firestoreException.getCode().name());
            }
        }
        String message = exception.getMessage();
        return message != null && !message.trim().isEmpty()
                ? message
                : getString(R.string.subscribe_error);
    }

    private void setSubscribeLoading(boolean loading) {
        subscribeProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        subscribeButton.setEnabled(!loading);
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
