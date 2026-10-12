package com.example.application;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
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
import java.util.Locale;

public class EventsActivity extends AppCompatActivity {
    private static final String TAG = "EventsActivity";
    public static final String EXTRA_EVENT_ID = "com.example.application.EVENT_ID";

    private static final int FILTER_ALL = 0;
    private static final int FILTER_MY_EVENTS = 1;
    private static final int FILTER_FAVORITES = 2;

    private EventAdapter eventAdapter;
    private TextView emptyMessage;
    private ProgressBar eventsProgress;
    private TextInputEditText searchInput;
    private MaterialAutoCompleteTextView categoryFilterInput;
    private MaterialAutoCompleteTextView statusFilterInput;

    private FirebaseAuth auth;
    private FirebaseFirestore database;

    private int currentFilterMode = FILTER_ALL;
    private final List<Event> fullLoadedEventsList = new ArrayList<>();
    private String currentSearchQuery = "";
    private String selectedCategoryFilter = "";
    private String selectedStatusFilter = "";

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
        searchInput = findViewById(R.id.search_events_input);
        categoryFilterInput = findViewById(R.id.filter_category_input);
        statusFilterInput = findViewById(R.id.filter_status_input);
        MaterialButtonToggleGroup filterToggle = findViewById(R.id.events_filter_toggle);
        eventAdapter = new EventAdapter(this::openEventDetails);

        eventsList.setLayoutManager(new LinearLayoutManager(this));
        eventsList.setAdapter(eventAdapter);

        setupFilterDropdowns();

        filterToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.filter_my_events) {
                    currentFilterMode = FILTER_MY_EVENTS;
                } else if (checkedId == R.id.filter_favorite_events) {
                    currentFilterMode = FILTER_FAVORITES;
                } else {
                    currentFilterMode = FILTER_ALL;
                }
                refreshEventsList();
            }
        });

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s != null ? s.toString().trim().toLowerCase(Locale.getDefault()) : "";
                applyFiltersAndSearch();
            }

            @Override
            public void afterTextChanged(Editable s) {
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

    private void setupFilterDropdowns() {
        String[] categories = new String[]{
                getString(R.string.all_categories),
                getString(R.string.category_academic),
                getString(R.string.category_sports),
                getString(R.string.category_tech),
                getString(R.string.category_cultural),
                getString(R.string.category_other)
        };
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                categories
        );
        categoryFilterInput.setAdapter(categoryAdapter);
        categoryFilterInput.setOnItemClickListener((parent, view, position, id) -> {
            String selected = categories[position];
            if (selected.equals(getString(R.string.all_categories))) {
                selectedCategoryFilter = "";
            } else {
                selectedCategoryFilter = selected;
            }
            applyFiltersAndSearch();
        });

        String[] statuses = new String[]{
                getString(R.string.all_statuses),
                getString(R.string.status_upcoming),
                getString(R.string.status_ended)
        };
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                statuses
        );
        statusFilterInput.setAdapter(statusAdapter);
        statusFilterInput.setOnItemClickListener((parent, view, position, id) -> {
            String selected = statuses[position];
            if (selected.equals(getString(R.string.all_statuses))) {
                selectedStatusFilter = "";
            } else {
                selectedStatusFilter = selected;
            }
            applyFiltersAndSearch();
        });
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
        if (currentFilterMode == FILTER_MY_EVENTS) {
            loadMyEvents();
        } else if (currentFilterMode == FILTER_FAVORITES) {
            loadFavoriteEvents();
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

    private void loadFavoriteEvents() {
        emptyMessage.setText(R.string.favorite_events_empty);
        if (auth == null || auth.getCurrentUser() == null) {
            openLogin();
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        setLoading(true);

        database.collection("favorites")
                .whereEqualTo("userId", userId)
                .get(Source.SERVER)
                .addOnSuccessListener(this::processFavorites)
                .addOnFailureListener(exception -> {
                    Log.w(TAG, "Falha ao carregar favoritos do servidor, tentando cache...", exception);
                    database.collection("favorites")
                            .whereEqualTo("userId", userId)
                            .get(Source.CACHE)
                            .addOnSuccessListener(this::processFavorites)
                            .addOnFailureListener(cacheException -> {
                                setLoading(false);
                                showEvents(Collections.emptyList());
                                handleFirestoreError(exception);
                            });
                });
    }

    private void processFavorites(QuerySnapshot querySnapshot) {
        if (querySnapshot == null || querySnapshot.isEmpty()) {
            setLoading(false);
            showEvents(Collections.emptyList());
            return;
        }

        List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
        for (QueryDocumentSnapshot favDoc : querySnapshot) {
            String eventId = favDoc.getString("eventId");
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
            List<Event> favEvents = new ArrayList<>();
            for (Task<DocumentSnapshot> task : tasks) {
                if (task.isSuccessful() && task.getResult() != null) {
                    DocumentSnapshot doc = task.getResult();
                    if (doc.exists()) {
                        try {
                            Event event = doc.toObject(Event.class);
                            if (event != null) {
                                event.setId(doc.getId());
                                favEvents.add(event);
                            }
                        } catch (RuntimeException exception) {
                            Log.e(TAG, "Erro ao converter evento favorito: " + doc.getId(), exception);
                        }
                    }
                }
            }
            favEvents.sort(Comparator.comparing(
                    Event::getDate,
                    Comparator.nullsLast(Comparator.naturalOrder())
            ));
            setLoading(false);
            showEvents(favEvents);
        });
    }

    private void applyFiltersAndSearch() {
        List<Event> filtered = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (Event event : fullLoadedEventsList) {
            // Search Query filter
            boolean matchesSearch = currentSearchQuery.isEmpty() ||
                    (event.getName() != null && event.getName().toLowerCase(Locale.getDefault()).contains(currentSearchQuery));

            if (!matchesSearch) {
                continue;
            }

            // Category filter
            if (!selectedCategoryFilter.isEmpty()) {
                String cat = event.getCategory() != null ? event.getCategory().trim() : "";
                if (!cat.equalsIgnoreCase(selectedCategoryFilter)) {
                    continue;
                }
            }

            // Status filter (Próximos vs Encerrados)
            if (!selectedStatusFilter.isEmpty()) {
                boolean isEnded = event.getDate() != null && event.getDate().toDate().getTime() < now;
                if (selectedStatusFilter.equals(getString(R.string.status_upcoming)) && isEnded) {
                    continue;
                }
                if (selectedStatusFilter.equals(getString(R.string.status_ended)) && !isEnded) {
                    continue;
                }
            }

            filtered.add(event);
        }

        eventAdapter.setEvents(filtered);

        if (fullLoadedEventsList.isEmpty()) {
            if (currentFilterMode == FILTER_MY_EVENTS) {
                emptyMessage.setText(R.string.my_events_empty);
            } else if (currentFilterMode == FILTER_FAVORITES) {
                emptyMessage.setText(R.string.favorite_events_empty);
            } else {
                emptyMessage.setText(R.string.events_empty);
            }
            emptyMessage.setVisibility(View.VISIBLE);
        } else if (filtered.isEmpty()) {
            emptyMessage.setText(R.string.search_empty_results);
            emptyMessage.setVisibility(View.VISIBLE);
        } else {
            emptyMessage.setVisibility(View.GONE);
        }
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
        fullLoadedEventsList.clear();
        if (events != null) {
            fullLoadedEventsList.addAll(events);
        }
        applyFiltersAndSearch();
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
