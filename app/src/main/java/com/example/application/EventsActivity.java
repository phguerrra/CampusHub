package com.example.application;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EventsActivity extends AppCompatActivity {
    public static final String EXTRA_EVENT_ID = "com.example.application.EVENT_ID";

    private EventAdapter eventAdapter;
    private TextView emptyMessage;
    private ProgressBar eventsProgress;
    private FirebaseAuth auth;
    private FirebaseFirestore database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_events);

        findViewById(R.id.create_event_button).setOnClickListener(view -> openCreateEvent());
        findViewById(R.id.profile_button).setOnClickListener(view -> openProfile());
        findViewById(R.id.logout_button).setOnClickListener(view -> signOut());
        RecyclerView eventsList = findViewById(R.id.events_list);
        emptyMessage = findViewById(R.id.events_empty_message);
        eventsProgress = findViewById(R.id.events_progress);
        eventAdapter = new EventAdapter(this::openEventDetails);

        eventsList.setLayoutManager(new LinearLayoutManager(this));
        eventsList.setAdapter(eventAdapter);

        showEvents(Collections.emptyList());

        if (FirebaseApp.getApps(this).isEmpty()) {
            openLogin();
            return;
        }
        auth = FirebaseAuth.getInstance();
        database = FirebaseFirestore.getInstance();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (auth != null) {
            if (auth.getCurrentUser() == null) {
                openLogin();
            } else {
                loadEvents();
            }
        }
    }

    private void loadEvents() {
        setLoading(true);
        database.collection("events")
                .orderBy("date")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Event> events = new ArrayList<>();
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        Event event = document.toObject(Event.class);
                        if (event != null) {
                            event.setId(document.getId());
                            events.add(event);
                        }
                    }
                    setLoading(false);
                    showEvents(events);
                })
                .addOnFailureListener(exception -> {
                    setLoading(false);
                    showEvents(Collections.emptyList());
                    Toast.makeText(this, R.string.events_load_error, Toast.LENGTH_LONG).show();
                });
    }

    private void setLoading(boolean loading) {
        eventsProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) {
            emptyMessage.setVisibility(View.GONE);
        }
    }

    private void showEvents(List<Event> events) {
        eventAdapter.setEvents(events);
        emptyMessage.setVisibility(events.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void signOut() {
        if (auth != null) {
            auth.signOut();
        }
        openLogin();
    }

    private void openProfile() {
        startActivity(new Intent(this, ProfileActivity.class));
    }

    private void openCreateEvent() {
        startActivity(new Intent(this, CreateEventActivity.class));
    }

    private void openEventDetails(Event event) {
        if (event.getId() == null || event.getId().trim().isEmpty()) {
            Toast.makeText(this, R.string.event_details_invalid_id, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, EventDetailsActivity.class);
        intent.putExtra(EXTRA_EVENT_ID, event.getId());
        startActivity(intent);
    }

    private void openLogin() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
