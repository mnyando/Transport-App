package com.example.transportapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TripAdapter extends RecyclerView.Adapter<TripAdapter.TripViewHolder> {
    private final List<Map<String, Object>> trips;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());

    public TripAdapter(List<Map<String, Object>> trips) {
        this.trips = trips;
    }

    @NonNull
    @Override
    public TripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trip, parent, false);
        return new TripViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TripViewHolder holder, int position) {
        Map<String, Object> trip = trips.get(position);
        android.content.Context ctx = holder.itemView.getContext();

        // Set route
        holder.routeText.setText(trip.containsKey("route") ? trip.get("route").toString() : ctx.getString(R.string.trip_unknown_route));

        // Set date/time
        if (trip.containsKey("date")) {
            holder.timeText.setText(trip.get("date").toString());
        } else if (trip.containsKey("startTimeMillis")) {
            holder.timeText.setText(dateFormat.format(new Date((Long)trip.get("startTimeMillis"))));
        } else {
            holder.timeText.setText(R.string.trip_date_unavailable);
        }

        // UPDATED TRIP TYPE SECTION - This is where you should add the new logic
        if (trip.containsKey("tripType")) {
            // Use the tripType we set in processTripDocument
            String type = trip.get("tripType").toString();
            holder.tripType.setText(
                    type.equalsIgnoreCase("pickup") ? R.string.trip_type_pickup :
                            type.equalsIgnoreCase("dropoff") ? R.string.trip_type_dropoff : R.string.trip_type_regular);
        }
        // Keep the old type check as fallback
        else if (trip.containsKey("type")) {
            String type = trip.get("type").toString();
            holder.tripType.setText(type.equalsIgnoreCase("pickup") ? R.string.trip_type_pickup : R.string.trip_type_dropoff);
        } else {
            holder.tripType.setText(R.string.trip_type_na);
        }

        // Set duration
        if (trip.containsKey("duration")) {
            holder.durationText.setText(trip.get("duration").toString());
        } else if (trip.containsKey("startTimeMillis") && trip.containsKey("endTimeMillis")) {
            long duration = (Long)trip.get("endTimeMillis") - (Long)trip.get("startTimeMillis");
            holder.durationText.setText(formatDuration(duration));
        } else {
            holder.durationText.setText(R.string.trip_duration_na);
        }

        // Set student count
        if (trip.containsKey("studentCount")) {
            int count = Integer.parseInt(trip.get("studentCount").toString());
            holder.studentsText.setText(ctx.getResources().getQuantityString(R.plurals.trip_student_count, count, count));
        } else if (trip.containsKey("studentIds")) {
            int count = ((List<?>) trip.get("studentIds")).size();
            holder.studentsText.setText(ctx.getResources().getQuantityString(R.plurals.trip_student_count, count, count));
        } else {
            holder.studentsText.setText(R.string.trip_students_na);
        }
    }
    private String formatDuration(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;

        if (hours > 0) {
            return String.format(Locale.getDefault(), "%dh %02dm", hours, minutes);
        }
        return String.format(Locale.getDefault(), "%dm", minutes);
    }

    @Override
    public int getItemCount() {
        return trips.size();
    }

    static class TripViewHolder extends RecyclerView.ViewHolder {
        TextView routeText, timeText, durationText, studentsText, tripType;

        public TripViewHolder(@NonNull View itemView) {
            super(itemView);
            routeText = itemView.findViewById(R.id.routeText);
            timeText = itemView.findViewById(R.id.timeText);
            durationText = itemView.findViewById(R.id.durationText);
            studentsText = itemView.findViewById(R.id.studentsText);
            tripType = itemView.findViewById(R.id.tripTypeText); // Make sure this ID exists in your layout
        }
    }
}