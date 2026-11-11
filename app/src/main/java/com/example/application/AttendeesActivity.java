package com.example.application;

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
import com.google.firebase.FirebaseApp;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class AttendeesActivity extends AppCompatActivity {
    private static final String TAG = "AttendeesActivity";

    private TextView countText;
    private TextView checkedinCountText;
    private TextView emptyMessage;
    private ProgressBar progress;
    private AttendeeAdapter adapter;

    private FirebaseFirestore database;
    private String eventId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendees);

        eventId = getIntent().getStringExtra(EventsActivity.EXTRA_EVENT_ID);
        if (eventId == null || eventId.trim().isEmpty()) {
            Toast.makeText(this, R.string.event_details_invalid_id, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        findViewById(R.id.attendees_back_button).setOnClickListener(v -> finish());
        countText = findViewById(R.id.attendees_count_text);
        checkedinCountText = findViewById(R.id.attendees_checkedin_count_text);
        emptyMessage = findViewById(R.id.attendees_empty_message);
        progress = findViewById(R.id.attendees_progress);

        RecyclerView recyclerView = findViewById(R.id.attendees_list);
        adapter = new AttendeeAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        if (FirebaseApp.getApps(this).isEmpty()) {
            emptyMessage.setText(R.string.firebase_not_configured);
            emptyMessage.setVisibility(View.VISIBLE);
            return;
        }

        database = FirebaseFirestore.getInstance();
        loadAttendees();
    }

    private void loadAttendees() {
        if (database == null || eventId == null) return;

        progress.setVisibility(View.VISIBLE);
        emptyMessage.setVisibility(View.GONE);

        database.collection("subscriptions")
                .whereEqualTo("eventId", eventId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot == null || querySnapshot.isEmpty()) {
                        progress.setVisibility(View.GONE);
                        countText.setText("0");
                        checkedinCountText.setText("0");
                        emptyMessage.setVisibility(View.VISIBLE);
                        adapter.setAttendees(new ArrayList<>());
                        return;
                    }

                    List<Task<DocumentSnapshot>> userTasks = new ArrayList<>();
                    List<Attendee> tempAttendees = new ArrayList<>();

                    for (QueryDocumentSnapshot subDoc : querySnapshot) {
                        String subId = subDoc.getId();
                        String userId = subDoc.getString("userId");
                        Timestamp subscribedAt = subDoc.getTimestamp("subscribedAt");
                        Boolean checkedIn = subDoc.getBoolean("checkedIn");
                        Timestamp checkedInAt = subDoc.getTimestamp("checkedInAt");

                        boolean isCheckedIn = checkedIn != null && checkedIn;

                        Attendee attendee = new Attendee(
                                subId, userId, "Carregando...", "",
                                subscribedAt, isCheckedIn, checkedInAt
                        );
                        tempAttendees.add(attendee);

                        if (userId != null && !userId.isEmpty()) {
                            userTasks.add(database.collection("users").document(userId).get());
                        }
                    }

                    if (userTasks.isEmpty()) {
                        progress.setVisibility(View.GONE);
                        displayAttendees(tempAttendees);
                        return;
                    }

                    Tasks.whenAllComplete(userTasks).addOnCompleteListener(allTasks -> {
                        for (Task<DocumentSnapshot> task : userTasks) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                DocumentSnapshot userDoc = task.getResult();
                                if (userDoc.exists()) {
                                    String uid = userDoc.getId();
                                    String name = userDoc.getString("name");
                                    String email = userDoc.getString("email");

                                    for (Attendee att : tempAttendees) {
                                        if (Objects.equals(uid, att.getUserId())) {
                                            if (name != null && !name.isEmpty()) att.setName(name);
                                            if (email != null && !email.isEmpty()) att.setEmail(email);
                                        }
                                    }
                                }
                            }
                        }

                        progress.setVisibility(View.GONE);
                        displayAttendees(tempAttendees);
                    });
                })
                .addOnFailureListener(e -> {
                    progress.setVisibility(View.GONE);
                    Log.e(TAG, "Erro ao carregar lista de inscritos", e);
                    emptyMessage.setText(R.string.events_load_error);
                    emptyMessage.setVisibility(View.VISIBLE);
                });
    }

    private void displayAttendees(List<Attendee> attendees) {
        attendees.sort(Comparator.comparing(
                Attendee::getSubscribedAt,
                Comparator.nullsLast(Comparator.naturalOrder())
        ));

        int total = attendees.size();
        int totalCheckedIn = 0;
        for (Attendee a : attendees) {
            if (a.isCheckedIn()) {
                totalCheckedIn++;
            }
        }

        countText.setText(String.format(Locale.getDefault(), "%d", total));
        checkedinCountText.setText(String.format(Locale.getDefault(), "%d", totalCheckedIn));

        if (total == 0) {
            emptyMessage.setVisibility(View.VISIBLE);
        } else {
            emptyMessage.setVisibility(View.GONE);
        }

        adapter.setAttendees(attendees);
    }
}
