package com.example.application;

import android.media.MediaPlayer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AudioAdapter extends RecyclerView.Adapter<AudioAdapter.AudioViewHolder> {
    private static final String TAG = "AudioAdapter";

    public interface OnAudioActionListener {
        void onDeleteAudio(EventAudio audio);
    }

    private final String currentUserId;
    private final OnAudioActionListener listener;
    private List<EventAudio> audios = new ArrayList<>();
    private String playingAudioId = null;
    private MediaPlayer mediaPlayer;

    public AudioAdapter(String currentUserId, OnAudioActionListener listener) {
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    public void setAudios(List<EventAudio> newAudios) {
        stopPlayback();
        this.audios = newAudios != null ? newAudios : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void stopPlayback() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            } catch (Exception e) {
                Log.e(TAG, "Erro ao liberar MediaPlayer", e);
            }
            mediaPlayer = null;
        }
        playingAudioId = null;
    }

    @NonNull
    @Override
    public AudioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_audio, parent, false);
        return new AudioViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AudioViewHolder holder, int position) {
        EventAudio audio = audios.get(position);
        holder.bind(audio);
    }

    @Override
    public int getItemCount() {
        return audios.size();
    }

    class AudioViewHolder extends RecyclerView.ViewHolder {
        private final ImageButton playButton;
        private final TextView authorNameText;
        private final TextView dateText;
        private final TextView statusText;
        private final ImageButton deleteButton;

        public AudioViewHolder(@NonNull View itemView) {
            super(itemView);
            playButton = itemView.findViewById(R.id.audio_item_play_button);
            authorNameText = itemView.findViewById(R.id.audio_item_author_name);
            dateText = itemView.findViewById(R.id.audio_item_date);
            statusText = itemView.findViewById(R.id.audio_item_status);
            deleteButton = itemView.findViewById(R.id.audio_item_delete_button);
        }

        public void bind(EventAudio audio) {
            String author = audio.getAuthorName() != null && !audio.getAuthorName().trim().isEmpty()
                    ? audio.getAuthorName().trim()
                    : "Aluno";
            authorNameText.setText(author);

            dateText.setText(formatDate(audio.getCreatedAt()));

            boolean isPlaying = audio.getId() != null && audio.getId().equals(playingAudioId);
            playButton.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play_arrow);
            statusText.setText(isPlaying ? "Reproduzindo..." : "Toque para ouvir");

            playButton.setOnClickListener(v -> togglePlay(audio));

            if (currentUserId != null && currentUserId.equals(audio.getUserId())) {
                deleteButton.setVisibility(View.VISIBLE);
                deleteButton.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onDeleteAudio(audio);
                    }
                });
            } else {
                deleteButton.setVisibility(View.GONE);
                deleteButton.setOnClickListener(null);
            }
        }

        private void togglePlay(EventAudio audio) {
            if (audio.getAudioPath() == null || audio.getAudioPath().isEmpty()) {
                Toast.makeText(itemView.getContext(), "Arquivo de áudio não encontrado.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (audio.getId() != null && audio.getId().equals(playingAudioId)) {
                // Se já está tocando esse mesmo áudio, para a reprodução
                stopPlayback();
                notifyDataSetChanged();
            } else {
                // Toca um novo áudio
                stopPlayback();
                try {
                    mediaPlayer = new MediaPlayer();
                    mediaPlayer.setDataSource(audio.getAudioPath());
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                    playingAudioId = audio.getId();
                    notifyDataSetChanged();

                    mediaPlayer.setOnCompletionListener(mp -> {
                        stopPlayback();
                        notifyDataSetChanged();
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Erro ao reproduzir áudio: " + audio.getAudioPath(), e);
                    Toast.makeText(itemView.getContext(), "Não foi possível reproduzir o áudio.", Toast.LENGTH_SHORT).show();
                    stopPlayback();
                    notifyDataSetChanged();
                }
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
