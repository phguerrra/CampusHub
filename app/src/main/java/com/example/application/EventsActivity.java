package com.example.application;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class EventsActivity extends AppCompatActivity {
    private static final String TAG = "EventsActivity";
    public static final String EXTRA_EVENT_ID = "com.example.application.EVENT_ID";

    private EventAdapter eventAdapter;
    private TextView emptyMessage;
    private ProgressBar eventsProgress;
    private FirebaseAuth auth;
    private FirebaseFirestore database;

    private boolean showingMyEvents = false;

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
        MaterialButtonToggleGroup filterToggle = findViewById(R.id.events_filter_toggle);
        eventAdapter = new EventAdapter(this::openEventDetails);

        eventsList.setLayoutManager(new LinearLayoutManager(this));
        eventsList.setAdapter(eventAdapter);

        filterToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                showingMyEvents = (checkedId == R.id.filter_my_events);
                refreshEventsList();
            }
        });

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
                refreshEventsList();
            }
        }
    }

    private void refreshEventsList() {
        if (showingMyEvents) {
            loadMyEvents();
        } else {
            loadAllEvents();
        }
    }

    private void loadAllEvents() {
        emptyMessage.setText(R.string.events_empty);
        setLoading(true);
        database.collection("events")
                .get(Source.SERVER)
                .addOnSuccessListener(querySnapshot -> {
                    List<Event> events = parseEventsSnapshot(querySnapshot);
                    setLoading(false);
                    showEvents(events);
                })
                .addOnFailureListener(exception -> {
                    Log.w(TAG, "Falha ao carregar do servidor, tentando cache local...", exception);
                    database.collection("events")
                            .get(Source.CACHE)
                            .addOnSuccessListener(cacheSnapshot -> {
                                List<Event> events = parseEventsSnapshot(cacheSnapshot);
                                setLoading(false);
                                showEvents(events);
                                handleFirestoreError(exception);
                            })
                            .addOnFailureListener(cacheException -> {
                                setLoading(false);
                                showEvents(Collections.emptyList());
                                handleFirestoreError(exception);
                            });
                });
    }

    private List<Event> parseEventsSnapshot(QuerySnapshot querySnapshot) {
        List<Event> events = new ArrayList<>();
        if (querySnapshot != null) {
            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                try {
                    Event event = document.toObject(Event.class);
                    if (event != null) {
                        event.setId(document.getId());
                        events.add(event);
                    }
                } catch (RuntimeException exception) {
                    Log.e(TAG, "Documento de evento inválido: " + document.getId(), exception);
                }
            }
            events.sort(Comparator.comparing(
                    Event::getDate,
                    Comparator.nullsLast(Comparator.naturalOrder())
            ));
        }
        return events;
    }

    private void loadMyEvents() {
        emptyMessage.setText(R.string.my_events_empty);
        if (auth == null || auth.getCurrentUser() == null) {
            openLogin();
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        setLoading(true);

        database.collection("subscriptions")
                .whereEqualTo("userId", userId)
                .get(Source.SERVER)
                .addOnSuccessListener(this::processSubscriptions)
                .addOnFailureListener(exception -> {
                    Log.w(TAG, "Falha ao carregar minhas inscrições do servidor, tentando cache...", exception);
                    database.collection("subscriptions")
                            .whereEqualTo("userId", userId)
                            .get(Source.CACHE)
                            .addOnSuccessListener(this::processSubscriptions)
                            .addOnFailureListener(cacheException -> {
                                setLoading(false);
                                showEvents(Collections.emptyList());
                                handleFirestoreError(exception);
                            });
                });
    }

    private void processSubscriptions(QuerySnapshot querySnapshot) {
        if (querySnapshot == null || querySnapshot.isEmpty()) {
            setLoading(false);
            showEvents(Collections.emptyList());
            return;
        }

        List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
        for (QueryDocumentSnapshot subDoc : querySnapshot) {
            String eventId = subDoc.getString("eventId");
            if (eventId != null && !eventId.trim().isEmpty()) {
                tasks.add(database.collection("events").document(eventId).get());
            }
        }

        if (tasks.isEmpty()) {
            setLoading(false);
            showEvents(Collections.emptyList());
            return;
        }

        Tasks.whenAllComplete(tasks).addOnCompleteListener(allTasks -> {
            List<Event> myEvents = new ArrayList<>();
            for (Task<DocumentSnapshot> task : tasks) {
                if (task.isSuccessful() && task.getResult() != null) {
                    DocumentSnapshot doc = task.getResult();
                    if (doc.exists()) {
                        try {
                            Event event = doc.toObject(Event.class);
                            if (event != null) {
                                event.setId(doc.getId());
                                myEvents.add(event);
                            }
                        } catch (RuntimeException exception) {
                            Log.e(TAG, "Erro ao converter evento inscrito: " + doc.getId(), exception);
                        }
                    }
                }
            }
            myEvents.sort(Comparator.comparing(
                    Event::getDate,
                    Comparator.nullsLast(Comparator.naturalOrder())
            ));
            setLoading(false);
            showEvents(myEvents);
        });
    }

    private void handleFirestoreError(Exception exception) {
        String errorTextMsg;
        if (exception instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException firestoreException = (FirebaseFirestoreException) exception;
            switch (firestoreException.getCode()) {
                case PERMISSION_DENIED:
                case UNAUTHENTICATED:
                    errorTextMsg = getString(R.string.firestore_permission_denied);
                    break;
                case UNAVAILABLE:
                case DEADLINE_EXCEEDED:
                    errorTextMsg = getString(R.string.firestore_unavailable);
                    break;
                default:
                    errorTextMsg = getString(
                            R.string.firestore_error_with_code,
                            firestoreException.getCode().name()
                    );
                    break;
            }
        } else {
            errorTextMsg = getString(R.string.events_load_error);
        }
        emptyMessage.setText(errorTextMsg);
        emptyMessage.setVisibility(View.VISIBLE);
        Toast.makeText(this, errorTextMsg, Toast.LENGTH_LONG).show();
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
