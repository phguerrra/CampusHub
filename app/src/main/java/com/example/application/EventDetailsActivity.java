package com.example.application;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
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
    private TextView descriptionText;
    private TextView availableSlotsText;
    private TextView errorText;
    private View detailsContent;
    private ProgressBar progress;
    private MaterialButton retryButton;
    private MaterialButton subscribeButton;
    private MaterialButton favoriteButton;
    private ProgressBar subscribeProgress;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);
        bindViews();
        setupCommentsRecyclerView();

        findViewById(R.id.event_details_back_button).setOnClickListener(view -> finish());
        retryButton.setOnClickListener(view -> loadEvent());
        sendCommentButton.setOnClickListener(view -> sendComment());

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
        categoryText = findViewById(R.id.event_details_category);
        dateTimeText = findViewById(R.id.event_details_date_time);
        locationText = findViewById(R.id.event_details_location);
        descriptionText = findViewById(R.id.event_details_description);
        availableSlotsText = findViewById(R.id.event_details_available_slots);
        errorText = findViewById(R.id.event_details_error);
        detailsContent = findViewById(R.id.event_details_content);
        progress = findViewById(R.id.event_details_progress);
        retryButton = findViewById(R.id.event_details_retry_button);
        subscribeButton = findViewById(R.id.event_details_subscribe_button);
        favoriteButton = findViewById(R.id.event_details_favorite_button);
        subscribeProgress = findViewById(R.id.event_details_subscribe_progress);

        commentInput = findViewById(R.id.add_comment_input);
        sendCommentButton = findViewById(R.id.send_comment_button);
        commentsEmptyMessage = findViewById(R.id.event_details_comments_empty);
        commentsRecyclerView = findViewById(R.id.event_details_comments_list);
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
                    checkFavoriteStatus();
                    loadComments();
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
                                if (comment != null) {
                                    comment.setId(doc.getId());
                                    comments.add(comment);
                                }
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

        nameText.setText(valueOrFallback(event.getName()));
        if (event.getCategory() != null && !event.getCategory().trim().isEmpty()) {
            categoryText.setText(event.getCategory().trim());
            categoryText.setVisibility(View.VISIBLE);
        } else {
            categoryText.setVisibility(View.GONE);
        }
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
