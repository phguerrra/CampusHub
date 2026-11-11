package com.example.application;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AttendeeAdapter extends RecyclerView.Adapter<AttendeeAdapter.AttendeeViewHolder> {
    private final List<Attendee> attendees = new ArrayList<>();

    public AttendeeAdapter() {
    }

    public void setAttendees(List<Attendee> newAttendees) {
        attendees.clear();
        if (newAttendees != null) {
            attendees.addAll(newAttendees);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AttendeeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_attendee, parent, false);
        return new AttendeeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AttendeeViewHolder holder, int position) {
        holder.bind(attendees.get(position));
    }

    @Override
    public int getItemCount() {
        return attendees.size();
    }

    static class AttendeeViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameText;
        private final TextView emailText;
        private final TextView subscribedAtText;
        private final TextView checkinBadge;

        public AttendeeViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.attendee_name);
            emailText = itemView.findViewById(R.id.attendee_email);
            subscribedAtText = itemView.findViewById(R.id.attendee_subscribed_at);
            checkinBadge = itemView.findViewById(R.id.attendee_checkin_badge);
        }

        public void bind(Attendee attendee) {
            nameText.setText(attendee.getName() != null ? attendee.getName() : "Aluno");
            emailText.setText(attendee.getEmail() != null ? attendee.getEmail() : "");

            if (attendee.getSubscribedAt() != null) {
                DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.getDefault());
                String formattedDate = formatter.format(attendee.getSubscribedAt().toDate());
                subscribedAtText.setText(itemView.getContext().getString(R.string.attendee_subscribed_at_format, formattedDate));
                subscribedAtText.setVisibility(View.VISIBLE);
            } else {
                subscribedAtText.setVisibility(View.GONE);
            }

            if (attendee.isCheckedIn()) {
                checkinBadge.setText(R.string.attendee_status_checkedin);
                checkinBadge.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(itemView.getContext(), R.color.badge_upcoming_bg)
                ));
                checkinBadge.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.badge_upcoming_text));
            } else {
                checkinBadge.setText(R.string.attendee_status_subscribed);
                checkinBadge.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(itemView.getContext(), R.color.badge_category_bg)
                ));
                checkinBadge.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.badge_category_text));
            }
        }
    }
}
