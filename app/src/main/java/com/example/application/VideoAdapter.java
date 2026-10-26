package com.example.application;

import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {
    private static final String TAG = "VideoAdapter";

    public interface OnVideoActionListener {
        void onDeleteVideo(EventVideo video);
    }

    private final String currentUserId;
    private final OnVideoActionListener listener;
    private List<EventVideo> videos = new ArrayList<>();

    public VideoAdapter(String currentUserId, OnVideoActionListener listener) {
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    public void setVideos(List<EventVideo> newVideos) {
        this.videos = newVideos != null ? newVideos : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_video, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        EventVideo video = videos.get(position);
        holder.bind(video);
    }

    @Override
    public int getItemCount() {
        return videos.size();
    }

    class VideoViewHolder extends RecyclerView.ViewHolder {
        private final VideoView videoView;
        private final ImageButton playOverlayButton;
        private final TextView authorNameText;
        private final TextView dateText;
        private final ImageButton deleteButton;

        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            videoView = itemView.findViewById(R.id.video_item_videoview);
            playOverlayButton = itemView.findViewById(R.id.video_item_play_button);
            authorNameText = itemView.findViewById(R.id.video_item_author_name);
            dateText = itemView.findViewById(R.id.video_item_date);
            deleteButton = itemView.findViewById(R.id.video_item_delete_button);
        }

        public void bind(EventVideo video) {
            String author = video.getAuthorName() != null && !video.getAuthorName().trim().isEmpty()
                    ? video.getAuthorName().trim()
                    : "Aluno";
            authorNameText.setText(author);

            dateText.setText(formatDate(video.getCreatedAt()));

            if (video.getVideoPath() != null) {
                File file = new File(video.getVideoPath());
                if (file.exists()) {
                    videoView.setVideoPath(file.getAbsolutePath());
                } else {
                    try {
                        videoView.setVideoURI(Uri.parse(video.getVideoPath()));
                    } catch (Exception e) {
                        Log.e(TAG, "Erro ao carregar URI do vídeo", e);
                    }
                }
            }

            playOverlayButton.setVisibility(View.VISIBLE);
            playOverlayButton.setImageResource(R.drawable.ic_play_arrow);

            playOverlayButton.setOnClickListener(v -> {
                if (videoView.isPlaying()) {
                    videoView.pause();
                    playOverlayButton.setImageResource(R.drawable.ic_play_arrow);
                    playOverlayButton.setVisibility(View.VISIBLE);
                } else {
                    videoView.start();
                    playOverlayButton.setVisibility(View.GONE);
                }
            });

            videoView.setOnClickListener(v -> {
                if (videoView.isPlaying()) {
                    videoView.pause();
                    playOverlayButton.setImageResource(R.drawable.ic_play_arrow);
                    playOverlayButton.setVisibility(View.VISIBLE);
                } else {
                    videoView.start();
                    playOverlayButton.setVisibility(View.GONE);
                }
            });

            videoView.setOnCompletionListener(mp -> {
                playOverlayButton.setImageResource(R.drawable.ic_play_arrow);
                playOverlayButton.setVisibility(View.VISIBLE);
            });

            videoView.setOnErrorListener((mp, what, extra) -> {
                Toast.makeText(itemView.getContext(), "Não foi possível reproduzir o vídeo.", Toast.LENGTH_SHORT).show();
                playOverlayButton.setVisibility(View.VISIBLE);
                return true;
            });

            if (currentUserId != null && currentUserId.equals(video.getUserId())) {
                deleteButton.setVisibility(View.VISIBLE);
                deleteButton.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onDeleteVideo(video);
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
