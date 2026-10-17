package com.example.application;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseApp;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.maplibre.android.MapLibre;
import org.maplibre.android.annotations.MarkerOptions;
import org.maplibre.android.camera.CameraUpdateFactory;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.Style;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EventDetailsActivity extends AppCompatActivity {
    private static final String TAG = "EventDetailsActivity";

    private TextView nameText;
    private TextView categoryText;
    private TextView dateTimeText;
    private TextView locationText;
    private MapView mapView;
    private TextView mapUnavailableMessage;
    private MaterialButton openMapButton;
    private TextView descriptionText;
    private TextView availableSlotsText;
    private TextView errorText;
    private View detailsContent;
    private ProgressBar progress;
    private MaterialButton retryButton;
    private MaterialButton subscribeButton;
    private MaterialButton favoriteButton;
    private ProgressBar subscribeProgress;

    private TextView ratingSummaryText;
    private TextView ratingNoticeText;
    private View ratingInputContainer;
    private ImageButton star1, star2, star3, star4, star5;
    private MaterialButton saveRatingButton;

    private MaterialButton recordAudioButton;
    private TextView recordingIndicator;
    private TextView audiosEmptyMessage;
    private RecyclerView audiosRecyclerView;
    private AudioAdapter audioAdapter;
    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;
    private String currentAudioPath = null;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    private TextInputEditText commentInput;
    private MaterialButton sendCommentButton;
    private TextView commentsEmptyMessage;
    private RecyclerView commentsRecyclerView;
    private CommentAdapter commentAdapter;

    private FirebaseAuth auth;
    private FirebaseFirestore database;
    private String eventId;
    private Event currentEvent;
    private boolean isSubscribed = false;
    private boolean isFavorited = false;
    private float userExistingRating = 0f;
    private boolean hasUserRated = false;
    private int selectedRatingValue = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapLibre.getInstance(this);
        setContentView(R.layout.activity_event_details);

        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        startRecordingAudio();
                    } else {
                        Toast.makeText(this, R.string.audio_permission_required, Toast.LENGTH_SHORT).show();
                    }
                }
        );

        bindViews();
        if (mapView != null) {
            mapView.onCreate(savedInstanceState);
        }
        setupAudiosRecyclerView();
        setupCommentsRecyclerView();

        findViewById(R.id.event_details_back_button).setOnClickListener(view -> finish());
        retryButton.setOnClickListener(view -> loadEvent());
        sendCommentButton.setOnClickListener(view -> sendComment());
        saveRatingButton.setOnClickListener(view -> saveRating());
        openMapButton.setOnClickListener(view -> openEventMap());
        recordAudioButton.setOnClickListener(view -> toggleRecordAudio());

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
        if (mapView != null) {
            mapView.onStart();
        }
        if (auth == null) {
            return;
        }
        if (auth.getCurrentUser() == null) {
            openLogin();
        } else {
            loadEvent();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mapView != null) {
            mapView.onStop();
        }
        if (isRecording) {
            stopRecordingAndPublishAudio();
        }
        if (audioAdapter != null) {
            audioAdapter.stopPlayback();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) {
            mapView.onSaveInstanceState(outState);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapView != null) {
            mapView.onDestroy();
        }
        if (audioAdapter != null) {
            audioAdapter.stopPlayback();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) {
            mapView.onLowMemory();
        }
    }

    private void bindViews() {
        nameText = findViewById(R.id.event_details_name);
        categoryText = findViewById(R.id.event_details_category);
        dateTimeText = findViewById(R.id.event_details_date_time);
        locationText = findViewById(R.id.event_details_location);
        mapView = findViewById(R.id.map_view);
        mapUnavailableMessage = findViewById(R.id.map_unavailable_message);
        openMapButton = findViewById(R.id.event_details_open_map_button);
        descriptionText = findViewById(R.id.event_details_description);
        availableSlotsText = findViewById(R.id.event_details_available_slots);
        errorText = findViewById(R.id.event_details_error);
        detailsContent = findViewById(R.id.event_details_content);
        progress = findViewById(R.id.event_details_progress);
        retryButton = findViewById(R.id.event_details_retry_button);
        subscribeButton = findViewById(R.id.event_details_subscribe_button);
        favoriteButton = findViewById(R.id.event_details_favorite_button);
        subscribeProgress = findViewById(R.id.event_details_subscribe_progress);

        ratingSummaryText = findViewById(R.id.event_details_rating_summary);
        ratingNoticeText = findViewById(R.id.event_details_rating_notice);
        ratingInputContainer = findViewById(R.id.event_details_rating_input_container);
        saveRatingButton = findViewById(R.id.event_details_save_rating_button);

        star1 = findViewById(R.id.star_1);
        star2 = findViewById(R.id.star_2);
        star3 = findViewById(R.id.star_3);
        star4 = findViewById(R.id.star_4);
        star5 = findViewById(R.id.star_5);

        star1.setOnClickListener(v -> setStarRating(1));
        star2.setOnClickListener(v -> setStarRating(2));
        star3.setOnClickListener(v -> setStarRating(3));
        star4.setOnClickListener(v -> setStarRating(4));
        star5.setOnClickListener(v -> setStarRating(5));

        recordAudioButton = findViewById(R.id.event_details_record_audio_button);
        recordingIndicator = findViewById(R.id.event_details_recording_indicator);
        audiosEmptyMessage = findViewById(R.id.event_details_audios_empty);
        audiosRecyclerView = findViewById(R.id.event_details_audios_list);

        commentInput = findViewById(R.id.add_comment_input);
        sendCommentButton = findViewById(R.id.send_comment_button);
        commentsEmptyMessage = findViewById(R.id.event_details_comments_empty);
        commentsRecyclerView = findViewById(R.id.event_details_comments_list);
    }

    private void openEventMap() {
        if (currentEvent == null) {
            return;
        }

        double lat = currentEvent.getLatitude();
        double lng = currentEvent.getLongitude();
        String address = currentEvent.getLocation();

        Uri mapUri;
        if (lat != 0.0 || lng != 0.0) {
            mapUri = Uri.parse("geo:" + lat + "," + lng + "?q=" + lat + "," + lng + "(" + Uri.encode(currentEvent.getName()) + ")");
        } else if (address != null && !address.trim().isEmpty()) {
            mapUri = Uri.parse("geo:0,0?q=" + Uri.encode(address));
        } else {
            Toast.makeText(this, R.string.event_information_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent mapIntent = new Intent(Intent.ACTION_VIEW, mapUri);
        try {
            startActivity(mapIntent);
        } catch (Exception e) {
            Log.e(TAG, "Erro ao abrir mapa", e);
            Toast.makeText(this, R.string.generic_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void setStarRating(int rating) {
        selectedRatingValue = rating;
        if (star1 != null) star1.setImageResource(rating >= 1 ? R.drawable.ic_star : R.drawable.ic_star_border);
        if (star2 != null) star2.setImageResource(rating >= 2 ? R.drawable.ic_star : R.drawable.ic_star_border);
        if (star3 != null) star3.setImageResource(rating >= 3 ? R.drawable.ic_star : R.drawable.ic_star_border);
        if (star4 != null) star4.setImageResource(rating >= 4 ? R.drawable.ic_star : R.drawable.ic_star_border);
        if (star5 != null) star5.setImageResource(rating >= 5 ? R.drawable.ic_star : R.drawable.ic_star_border);
    }

    private void setupAudiosRecyclerView() {
        String currentUserId = auth != null && auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        audioAdapter = new AudioAdapter(currentUserId, this::showDeleteAudioDialog);
        audiosRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        audiosRecyclerView.setAdapter(audioAdapter);
    }

    private void setupCommentsRecyclerView() {
        String currentUserId = auth != null && auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        commentAdapter = new CommentAdapter(currentUserId, new CommentAdapter.OnCommentActionListener() {
            @Override
            public void onEditComment(Comment comment) {
                showEditCommentDialog(comment);
            }

            @Override
            public void onDeleteComment(Comment comment) {
                showDeleteCommentDialog(comment);
            }
        });
        commentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        commentsRecyclerView.setAdapter(commentAdapter);
    }

    private void toggleRecordAudio() {
        if (isRecording) {
            stopRecordingAndPublishAudio();
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED) {
                startRecordingAudio();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
            }
        }
    }

    private void startRecordingAudio() {
        try {
            File audioDir = new File(getExternalFilesDir(null), "audios");
            if (!audioDir.exists()) {
                audioDir.mkdirs();
            }
            File audioFile = new File(audioDir, "audio_" + System.currentTimeMillis() + ".m4a");
            currentAudioPath = audioFile.getAbsolutePath();

            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setOutputFile(currentAudioPath);
            mediaRecorder.prepare();
            mediaRecorder.start();

            isRecording = true;
            recordAudioButton.setText(R.string.stop_recording_button);
            recordAudioButton.setIconResource(R.drawable.ic_pause);
            recordingIndicator.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            Log.e(TAG, "Erro ao iniciar gravação de áudio", e);
            Toast.makeText(this, R.string.generic_error, Toast.LENGTH_SHORT).show();
            resetRecordingState();
        }
    }

    private void stopRecordingAndPublishAudio() {
        if (!isRecording || mediaRecorder == null) {
            return;
        }
        try {
            mediaRecorder.stop();
            mediaRecorder.release();
        } catch (Exception e) {
            Log.e(TAG, "Erro ao parar gravação de áudio", e);
        }
        mediaRecorder = null;
        isRecording = false;

        resetRecordingState();

        if (currentAudioPath != null && new File(currentAudioPath).exists()) {
            publishAudioToFirestore(currentAudioPath);
        }
    }

    private void resetRecordingState() {
        isRecording = false;
        recordAudioButton.setText(R.string.record_audio_button);
        recordAudioButton.setIconResource(R.drawable.ic_mic);
        recordingIndicator.setVisibility(View.GONE);
    }

    private void publishAudioToFirestore(String audioPath) {
        if (auth == null || auth.getCurrentUser() == null || database == null || eventId == null) {
            openLogin();
            return;
        }

        FirebaseUser user = auth.getCurrentUser();
        String userId = user.getUid();
        String authorName = user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()
                ? user.getDisplayName().trim()
                : "Aluno";

        Map<String, Object> audioData = new HashMap<>();
        audioData.put("eventId", eventId);
        audioData.put("userId", userId);
        audioData.put("authorName", authorName);
        audioData.put("audioPath", audioPath);
        audioData.put("createdAt", FieldValue.serverTimestamp());

        database.collection("events")
                .document(eventId)
                .collection("audios")
                .add(audioData)
                .addOnSuccessListener(doc -> {
                    Toast.makeText(this, R.string.audio_recorded_success, Toast.LENGTH_SHORT).show();
                    loadAudios();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao salvar áudio no Firestore", e);
                    Toast.makeText(this, R.string.generic_error, Toast.LENGTH_SHORT).show();
                });
    }

    private void loadAudios() {
        if (database == null || eventId == null) {
            return;
        }
        database.collection("events")
                .document(eventId)
                .collection("audios")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<EventAudio> audios = new ArrayList<>();
                    if (querySnapshot != null) {
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            try {
                                EventAudio audio = doc.toObject(EventAudio.class);
                                audio.setId(doc.getId());
                                audios.add(audio);
                            } catch (Exception e) {
                                Log.e(TAG, "Erro ao converter áudio: " + doc.getId(), e);
                            }
                        }
                    }
                    String currentUserId = auth != null && auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
                    audioAdapter = new AudioAdapter(currentUserId, this::showDeleteAudioDialog);
                    audiosRecyclerView.setAdapter(audioAdapter);
                    audioAdapter.setAudios(audios);
                    audiosEmptyMessage.setVisibility(audios.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Erro ao carregar áudios", e));
    }

    private void showDeleteAudioDialog(EventAudio audio) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_audio_title)
                .setMessage(R.string.delete_audio_confirm)
                .setPositiveButton(R.string.delete_comment, (dialog, which) -> deleteAudio(audio))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deleteAudio(EventAudio audio) {
        if (database == null || eventId == null || audio.getId() == null) {
            return;
        }
        database.collection("events")
                .document(eventId)
                .collection("audios")
                .document(audio.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    if (audio.getAudioPath() != null) {
                        File file = new File(audio.getAudioPath());
                        if (file.exists()) {
                            file.delete();
                        }
                    }
                    Toast.makeText(this, R.string.audio_deleted, Toast.LENGTH_SHORT).show();
                    loadAudios();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao excluir áudio", e);
                    Toast.makeText(this, R.string.generic_error, Toast.LENGTH_SHORT).show();
                });
    }

    private void loadEvent() {
        showLoading();
        if (database == null || eventId == null) {
            showError(getString(R.string.event_details_invalid_id), false);
            return;
        }
        database.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        showError(getString(R.string.event_details_not_found), false);
                        return;
                    }
                    try {
                        Event event = document.toObject(Event.class);
                        if (event == null) {
                            showError(getString(R.string.event_details_load_error), true);
                            return;
                        }
                        event.setId(document.getId());
                        currentEvent = event;
                        showEvent(event);
                        checkSubscriptionStatus();
                        checkFavoriteStatus();
                        loadRatingsAndCalculateAverage();
                        loadAudios();
                        loadComments();
                    } catch (Exception e) {
                        Log.e(TAG, "Erro ao desserializar evento", e);
                        showError(getString(R.string.event_details_load_error), true);
                    }
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
                    updateUserRatingUI();
                })
                .addOnFailureListener(exception -> {
                    Log.e(TAG, "Erro ao checar status de inscrição", exception);
                    isSubscribed = false;
                    updateSubscribeButtonUI();
                    updateUserRatingUI();
                });
    }

    private void checkFavoriteStatus() {
        if (auth == null || auth.getCurrentUser() == null || database == null) {
            return;
        }
        String userId = auth.getCurrentUser().getUid();
        String favoriteDocId = eventId + "_" + userId;

        database.collection("favorites")
                .document(favoriteDocId)
                .get()
                .addOnSuccessListener(document -> {
                    isFavorited = document.exists();
                    updateFavoriteButtonUI();
                })
                .addOnFailureListener(exception -> {
                    Log.e(TAG, "Erro ao checar status de favorito", exception);
                    isFavorited = false;
                    updateFavoriteButtonUI();
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

    private void updateFavoriteButtonUI() {
        favoriteButton.setEnabled(true);
        if (isFavorited) {
            favoriteButton.setText(R.string.unfavorite_event);
            favoriteButton.setIconResource(R.drawable.ic_star);
        } else {
            favoriteButton.setText(R.string.favorite_event);
            favoriteButton.setIconResource(R.drawable.ic_star_border);
        }
        favoriteButton.setOnClickListener(v -> toggleFavorite());
    }

    private void toggleFavorite() {
        if (auth == null || auth.getCurrentUser() == null || database == null) {
            openLogin();
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        String favoriteDocId = eventId + "_" + userId;
        DocumentReference favRef = database.collection("favorites").document(favoriteDocId);

        favoriteButton.setEnabled(false);

        if (isFavorited) {
            favRef.delete()
                    .addOnSuccessListener(aVoid -> {
                        isFavorited = false;
                        updateFavoriteButtonUI();
                        Toast.makeText(this, R.string.favorite_removed, Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(exception -> {
                        favoriteButton.setEnabled(true);
                        Log.e(TAG, "Erro ao remover favorito", exception);
                        Toast.makeText(this, R.string.favorite_error, Toast.LENGTH_SHORT).show();
                    });
        } else {
            Map<String, Object> favData = new HashMap<>();
            favData.put("eventId", eventId);
            favData.put("userId", userId);
            favData.put("favoritedAt", FieldValue.serverTimestamp());

            favRef.set(favData)
                    .addOnSuccessListener(aVoid -> {
                        isFavorited = true;
                        updateFavoriteButtonUI();
                        Toast.makeText(this, R.string.favorite_added, Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(exception -> {
                        favoriteButton.setEnabled(true);
                        Log.e(TAG, "Erro ao adicionar favorito", exception);
                        Toast.makeText(this, R.string.favorite_error, Toast.LENGTH_SHORT).show();
                    });
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

    private void loadRatingsAndCalculateAverage() {
        if (database == null || eventId == null) {
            return;
        }
        String currentUserId = auth != null && auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        database.collection("events")
                .document(eventId)
                .collection("ratings")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    float totalScore = 0f;
                    int count = 0;
                    hasUserRated = false;
                    userExistingRating = 0f;

                    if (querySnapshot != null) {
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            Double score = doc.getDouble("rating");
                            if (score != null) {
                                totalScore += score.floatValue();
                                count++;
                                if (currentUserId != null && currentUserId.equals(doc.getString("userId"))) {
                                    hasUserRated = true;
                                    userExistingRating = score.floatValue();
                                }
                            }
                        }
                    }

                    if (count > 0) {
                        float avg = totalScore / count;
                        ratingSummaryText.setText(getString(R.string.rating_summary, avg, count));
                    } else {
                        ratingSummaryText.setText(R.string.rating_summary_empty);
                    }

                    updateUserRatingUI();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao carregar avaliações", e);
                    ratingSummaryText.setText(R.string.rating_summary_empty);
                    updateUserRatingUI();
                });
    }

    private void updateUserRatingUI() {
        if (currentEvent == null) {
            return;
        }

        boolean isEnded = isEventEnded(currentEvent);

        if (!isSubscribed) {
            ratingNoticeText.setText(R.string.rating_user_notice_not_subscribed);
            ratingInputContainer.setVisibility(View.GONE);
        } else if (!isEnded) {
            ratingNoticeText.setText(R.string.rating_user_notice_not_ended);
            ratingInputContainer.setVisibility(View.GONE);
        } else {
            ratingNoticeText.setText(R.string.rating_user_notice_eligible);
            ratingInputContainer.setVisibility(View.VISIBLE);
            if (hasUserRated) {
                setStarRating((int) userExistingRating);
                saveRatingButton.setText(R.string.update_rating);
            } else {
                setStarRating(5);
                saveRatingButton.setText(R.string.save_rating);
            }
        }
    }

    private boolean isEventEnded(Event event) {
        if (event == null || event.getDate() == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        long eventTime = event.getDate().toDate().getTime();
        return eventTime < now;
    }

    private void saveRating() {
        if (auth == null || auth.getCurrentUser() == null || database == null) {
            openLogin();
            return;
        }

        if (selectedRatingValue < 1 || selectedRatingValue > 5) {
            Toast.makeText(this, R.string.rating_select_error, Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        DocumentReference ratingRef = database.collection("events")
                .document(eventId)
                .collection("ratings")
                .document(userId);

        Map<String, Object> ratingData = new HashMap<>();
        ratingData.put("eventId", eventId);
        ratingData.put("userId", userId);
        ratingData.put("rating", (double) selectedRatingValue);
        ratingData.put("updatedAt", FieldValue.serverTimestamp());

        saveRatingButton.setEnabled(false);

        ratingRef.set(ratingData)
                .addOnSuccessListener(aVoid -> {
                    saveRatingButton.setEnabled(true);
                    Toast.makeText(this, R.string.rating_saved, Toast.LENGTH_SHORT).show();
                    loadRatingsAndCalculateAverage();
                })
                .addOnFailureListener(e -> {
                    saveRatingButton.setEnabled(true);
                    Log.e(TAG, "Erro ao salvar avaliação", e);
                    Toast.makeText(this, R.string.generic_error, Toast.LENGTH_SHORT).show();
                });
    }

    private void loadComments() {
        if (database == null || eventId == null) {
            return;
        }
        database.collection("events")
                .document(eventId)
                .collection("comments")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Comment> comments = new ArrayList<>();
                    if (querySnapshot != null) {
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            try {
                                Comment comment = doc.toObject(Comment.class);
                                comment.setId(doc.getId());
                                comments.add(comment);
                            } catch (RuntimeException e) {
                                Log.e(TAG, "Erro ao converter comentário: " + doc.getId(), e);
                            }
                        }
                    }
                    String currentUserId = auth != null && auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
                    commentAdapter = new CommentAdapter(currentUserId, new CommentAdapter.OnCommentActionListener() {
                        @Override
                        public void onEditComment(Comment comment) {
                            showEditCommentDialog(comment);
                        }

                        @Override
                        public void onDeleteComment(Comment comment) {
                            showDeleteCommentDialog(comment);
                        }
                    });
                    commentsRecyclerView.setAdapter(commentAdapter);
                    commentAdapter.setComments(comments);
                    commentsEmptyMessage.setVisibility(comments.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Erro ao carregar comentários", e));
    }

    private void sendComment() {
        if (auth == null || auth.getCurrentUser() == null || database == null) {
            openLogin();
            return;
        }

        String commentText = commentInput.getText() != null ? commentInput.getText().toString().trim() : "";
        if (commentText.isEmpty()) {
            Toast.makeText(this, R.string.comment_empty_error, Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = auth.getCurrentUser();
        String userId = user.getUid();
        String authorName = user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()
                ? user.getDisplayName().trim()
                : "Aluno";

        Map<String, Object> commentData = new HashMap<>();
        commentData.put("eventId", eventId);
        commentData.put("userId", userId);
        commentData.put("authorName", authorName);
        commentData.put("text", commentText);
        commentData.put("createdAt", FieldValue.serverTimestamp());

        sendCommentButton.setEnabled(false);

        database.collection("events")
                .document(eventId)
                .collection("comments")
                .add(commentData)
                .addOnSuccessListener(documentReference -> {
                    sendCommentButton.setEnabled(true);
                    commentInput.setText("");
                    Toast.makeText(this, R.string.comment_added, Toast.LENGTH_SHORT).show();
                    loadComments();
                })
                .addOnFailureListener(e -> {
                    sendCommentButton.setEnabled(true);
                    Log.e(TAG, "Erro ao enviar comentário", e);
                    Toast.makeText(this, R.string.generic_error, Toast.LENGTH_SHORT).show();
                });
    }

    private void showEditCommentDialog(Comment comment) {
        EditText editInput = new EditText(this);
        editInput.setText(comment.getText());
        editInput.setSelection(comment.getText().length());
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        editInput.setPadding(padding, padding, padding, padding);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.edit_comment)
                .setView(editInput)
                .setPositiveButton(R.string.save_profile, (dialog, which) -> {
                    String updatedText = editInput.getText().toString().trim();
                    if (!updatedText.isEmpty()) {
                        updateCommentText(comment, updatedText);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void updateCommentText(Comment comment, String updatedText) {
        if (database == null || eventId == null || comment.getId() == null) {
            return;
        }
        database.collection("events")
                .document(eventId)
                .collection("comments")
                .document(comment.getId())
                .update("text", updatedText)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, R.string.comment_updated, Toast.LENGTH_SHORT).show();
                    loadComments();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao atualizar comentário", e);
                    Toast.makeText(this, R.string.generic_error, Toast.LENGTH_SHORT).show();
                });
    }

    private void showDeleteCommentDialog(Comment comment) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_comment)
                .setMessage(R.string.comment_delete_confirm)
                .setPositiveButton(R.string.delete_comment, (dialog, which) -> deleteComment(comment))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deleteComment(Comment comment) {
        if (database == null || eventId == null || comment.getId() == null) {
            return;
        }
        database.collection("events")
                .document(eventId)
                .collection("comments")
                .document(comment.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, R.string.comment_deleted, Toast.LENGTH_SHORT).show();
                    loadComments();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao excluir comentário", e);
                    Toast.makeText(this, R.string.generic_error, Toast.LENGTH_SHORT).show();
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

        if (nameText != null) nameText.setText(valueOrFallback(event.getName()));
        if (descriptionText != null) descriptionText.setText(valueOrFallback(event.getDescription()));
        if (categoryText != null) {
            if (event.getCategory() != null && !event.getCategory().trim().isEmpty()) {
                categoryText.setText(event.getCategory().trim());
                categoryText.setVisibility(View.VISIBLE);
            } else {
                categoryText.setVisibility(View.GONE);
            }
        }
        if (dateTimeText != null) {
            dateTimeText.setText(getString(
                    R.string.event_date_time,
                    formatDate(event.getDate()),
                    valueOrFallback(event.getTime())));
        }
        if (locationText != null) locationText.setText(valueOrFallback(event.getLocation()));

        if (availableSlotsText != null) {
            availableSlotsText.setText(getResources().getQuantityString(
                    R.plurals.event_available_slots,
                    (int) event.getAvailableSlots(),
                    event.getAvailableSlots()));
        }

        setupMapForEvent(event);
    }

    private void setupMapForEvent(Event event) {
        if (mapView == null) {
            return;
        }

        double lat = event != null ? event.getLatitude() : 0.0;
        double lng = event != null ? event.getLongitude() : 0.0;

        if (lat == 0.0 && lng == 0.0) {
            mapView.setVisibility(View.GONE);
            if (mapUnavailableMessage != null) {
                mapUnavailableMessage.setVisibility(View.VISIBLE);
            }
            return;
        }

        if (mapUnavailableMessage != null) {
            mapUnavailableMessage.setVisibility(View.GONE);
        }
        mapView.setVisibility(View.VISIBLE);

        mapView.getMapAsync(mapLibreMap -> {
            String osmStyle = "{\n" +
                    "  \"version\": 8,\n" +
                    "  \"sources\": {\n" +
                    "    \"osm-tiles\": {\n" +
                    "      \"type\": \"raster\",\n" +
                    "      \"tiles\": [\"https://tile.openstreetmap.org/{z}/{x}/{y}.png\"],\n" +
                    "      \"tileSize\": 256,\n" +
                    "      \"attribution\": \"© OpenStreetMap contributors\"\n" +
                    "    }\n" +
                    "  },\n" +
                    "  \"layers\": [\n" +
                    "    {\n" +
                    "      \"id\": \"osm-tiles-layer\",\n" +
                    "      \"type\": \"raster\",\n" +
                    "      \"source\": \"osm-tiles\"\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}";

            mapLibreMap.setStyle(new Style.Builder().fromJson(osmStyle), style -> {
                LatLng position = new LatLng(lat, lng);
                mapLibreMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 15));
                mapLibreMap.addMarker(new MarkerOptions()
                        .position(position)
                        .title(event.getName() != null ? event.getName() : "Local do Evento"));
            });
        });
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
