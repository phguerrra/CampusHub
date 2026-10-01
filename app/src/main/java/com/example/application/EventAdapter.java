package com.example.application;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {
    private final List<Event> events = new ArrayList<>();

    public void setEvents(List<Event> updatedEvents) {
        events.clear();
        events.addAll(updatedEvents);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        holder.bind(events.get(position));
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameText;
        private final TextView dateTimeText;
        private final TextView locationText;
        private final TextView descriptionText;
        private final TextView availableSlotsText;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.event_name);
            dateTimeText = itemView.findViewById(R.id.event_date_time);
            locationText = itemView.findViewById(R.id.event_location);
            descriptionText = itemView.findViewById(R.id.event_description);
            availableSlotsText = itemView.findViewById(R.id.event_available_slots);
        }

        void bind(Event event) {
            nameText.setText(event.getName());
            dateTimeText.setText(itemView.getContext().getString(
                    R.string.event_date_time, formatDate(event), valueOrDash(event.getTime())));
            locationText.setText(event.getLocation());
            descriptionText.setText(event.getDescription());
            availableSlotsText.setText(itemView.getContext().getResources().getQuantityString(
                    R.plurals.event_available_slots,
                    (int) event.getAvailableSlots(),
                    event.getAvailableSlots()));
        }

        private String formatDate(Event event) {
            if (event.getDate() == null) {
                return itemView.getContext().getString(R.string.event_date_unavailable);
            }
            DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            return formatter.format(event.getDate().toDate());
        }

        private String valueOrDash(String value) {
            return value == null || value.trim().isEmpty() ? "—" : value;
        }
    }
}
