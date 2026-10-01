package com.example.application;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Collections;
import java.util.List;

public class EventsActivity extends AppCompatActivity {
    private EventAdapter eventAdapter;
    private TextView emptyMessage;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_events);

        findViewById(R.id.profile_button).setOnClickListener(view -> openProfile());
        findViewById(R.id.logout_button).setOnClickListener(view -> signOut());
        RecyclerView eventsList = findViewById(R.id.events_list);
        emptyMessage = findViewById(R.id.events_empty_message);
        eventAdapter = new EventAdapter();

        eventsList.setLayoutManager(new LinearLayoutManager(this));
        eventsList.setAdapter(eventAdapter);

        showEvents(Collections.emptyList());

        if (FirebaseApp.getApps(this).isEmpty()) {
            openLogin();
            return;
        }
        auth = FirebaseAuth.getInstance();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (auth != null && auth.getCurrentUser() == null) {
            openLogin();
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

    private void openLogin() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
