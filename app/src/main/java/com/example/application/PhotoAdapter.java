package com.example.application;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder> {

    public interface OnPhotoActionListener {
        void onDeletePhoto(EventPhoto photo);
    }

    private final String currentUserId;
    private final OnPhotoActionListener listener;
    private List<EventPhoto> photos = new ArrayList<>();

    public PhotoAdapter(String currentUserId, OnPhotoActionListener listener) {
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    public void setPhotos(List<EventPhoto> newPhotos) {
        this.photos = newPhotos != null ? newPhotos : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_photo, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        EventPhoto photo = photos.get(position);
        holder.bind(photo);
    }

    @Override
    public int getItemCount() {
        return photos.size();
    }

    class PhotoViewHolder extends RecyclerView.ViewHolder {
        private final ImageView photoImage;
        private final TextView authorNameText;
        private final TextView dateText;
        private final ImageButton deleteButton;

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            photoImage = itemView.findViewById(R.id.photo_item_image);
            authorNameText = itemView.findViewById(R.id.photo_item_author_name);
            dateText = itemView.findViewById(R.id.photo_item_date);
            deleteButton = itemView.findViewById(R.id.photo_item_delete_button);
        }

        public void bind(EventPhoto photo) {
            String author = photo.getAuthorName() != null && !photo.getAuthorName().trim().isEmpty()
                    ? photo.getAuthorName().trim()
                    : "Aluno";
            authorNameText.setText(author);

            dateText.setText(formatDate(photo.getCreatedAt()));

            if (photo.getPhotoPath() != null) {
                File file = new File(photo.getPhotoPath());
                if (file.exists()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                    if (bitmap != null) {
                        photoImage.setImageBitmap(bitmap);
                    } else {
                        photoImage.setImageResource(R.drawable.ic_event_empty);
                    }
                } else {
                    try {
                        photoImage.setImageURI(Uri.parse(photo.getPhotoPath()));
                    } catch (Exception e) {
                        photoImage.setImageResource(R.drawable.ic_event_empty);
                    }
                }
            } else {
                photoImage.setImageResource(R.drawable.ic_event_empty);
            }

            if (currentUserId != null && currentUserId.equals(photo.getUserId())) {
                deleteButton.setVisibility(View.VISIBLE);
                deleteButton.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onDeletePhoto(photo);
                    }
                });
            } else {
                deleteButton.setVisibility(View.GONE);
                deleteButton.setOnClickListener(null);
            }
        }

        private String formatDate(Timestamp timestamp) {
            if (timestamp == null) {
                return "";
            }
            DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.getDefault());
            return formatter.format(timestamp.toDate());
        }
    }
}
