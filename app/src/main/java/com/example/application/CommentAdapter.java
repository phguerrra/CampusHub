package com.example.application;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    public interface OnCommentActionListener {
        void onEditComment(Comment comment);
        void onDeleteComment(Comment comment);
    }

    private final List<Comment> comments = new ArrayList<>();
    private final String currentUserId;
    private final OnCommentActionListener listener;

    public CommentAdapter(String currentUserId, OnCommentActionListener listener) {
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    public void setComments(List<Comment> newComments) {
        comments.clear();
        if (newComments != null) {
            comments.addAll(newComments);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = comments.get(position);
        holder.bind(comment, currentUserId, listener);
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        private final TextView authorText;
        private final TextView dateText;
        private final TextView commentText;
        private final MaterialButton editButton;
        private final MaterialButton deleteButton;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            authorText = itemView.findViewById(R.id.comment_author);
            dateText = itemView.findViewById(R.id.comment_date);
            commentText = itemView.findViewById(R.id.comment_text);
            editButton = itemView.findViewById(R.id.comment_edit_button);
            deleteButton = itemView.findViewById(R.id.comment_delete_button);
        }

        public void bind(Comment comment, String currentUserId, OnCommentActionListener listener) {
            authorText.setText(comment.getAuthorName() != null && !comment.getAuthorName().trim().isEmpty()
                    ? comment.getAuthorName()
                    : "Aluno");
            commentText.setText(comment.getText());
            dateText.setText(formatDate(comment.getCreatedAt()));

            boolean isOwner = currentUserId != null && currentUserId.equals(comment.getUserId());
            editButton.setVisibility(isOwner ? View.VISIBLE : View.GONE);
            deleteButton.setVisibility(isOwner ? View.VISIBLE : View.GONE);

            if (isOwner && listener != null) {
                editButton.setOnClickListener(v -> listener.onEditComment(comment));
                deleteButton.setOnClickListener(v -> listener.onDeleteComment(comment));
            }
        }

        private String formatDate(Timestamp timestamp) {
            if (timestamp == null) {
                return "";
            }
            DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            return formatter.format(timestamp.toDate());
        }
    }
}
