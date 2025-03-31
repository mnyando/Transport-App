package com.example.transportapp;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StudentStatusAdapter extends RecyclerView.Adapter<StudentStatusAdapter.StudentStatusViewHolder> {
    private List<Student> studentList;
    private FirebaseFirestore db;
    private String tripId;
    private boolean isPickup; // Added to distinguish between pickup/dropoff

    public StudentStatusAdapter(List<Student> studentList, String tripId, boolean isPickup) {
        this.studentList = studentList;
        this.db = FirebaseFirestore.getInstance();
        this.tripId = tripId;
        this.isPickup = isPickup; // Store the mode
    }

    public void setTripId(String tripId) {
        this.tripId = tripId;
    }

    @NonNull
    @Override
    public StudentStatusViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_student_status, parent, false);
        return new StudentStatusViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentStatusViewHolder holder, int position) {
        Student student = studentList.get(position);
        holder.studentNameTextView.setText(student.getName());

        // Update checkbox labels based on mode
        if (isPickup) {
            holder.pickedCheckBox.setText("Picked Up");
            holder.notPickedCheckBox.setText("Not Picked");
        } else {
            holder.pickedCheckBox.setText("Dropped Off");
            holder.notPickedCheckBox.setText("Not Dropped");
        }

        // Initialize checkboxes based on current status
        boolean isCompleted = student.getPickupStatus() != null &&
                student.getPickupStatus().isPicked();
        holder.pickedCheckBox.setChecked(isCompleted);
        holder.notPickedCheckBox.setChecked(!isCompleted);

        holder.pickedCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                student.updatePickupStatus(true, tripId);
                holder.notPickedCheckBox.setChecked(false);
                updateStudentInFirestore(student, true);
            }
        });

        holder.notPickedCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                student.updatePickupStatus(false, tripId);
                holder.pickedCheckBox.setChecked(false);
                updateStudentInFirestore(student, false);
            }
        });
    }

    private void updateStudentInFirestore(Student student, boolean isCompleted) {
        Map<String, Object> updates = new HashMap<>();
        long currentTime = System.currentTimeMillis();

        if (isPickup) {
            // PICKUP UPDATES
            updates.put("pickupStatus", new HashMap<String, Object>() {{
                put("isPicked", isCompleted);
                put("timestamp", currentTime);
                put("tripId", tripId);
                put("type", "pickup");
            }});
            updates.put("pickupTime", formatTimestamp(currentTime));
            updates.put("currentTripId", tripId);  // Critical for tracking active trips


        } else {
            // DROPOFF UPDATES
            updates.put("dropoffStatus", new HashMap<String, Object>() {{
                put("isDropped", isCompleted);
                put("timestamp", currentTime);
                put("tripId", tripId);
                put("type", "dropoff");
            }});
            updates.put("dropOffTime", formatTimestamp(currentTime));
            updates.put("currentTripId", null);  // Clear trip reference

            // Maintain pickup reference for history
            if (student.getPickupStatus() != null) {
                updates.put("lastPickupTripId", student.getPickupStatus().getTripId());
            }
        }

        // Update student document
        db.collection("students")
                .document(student.getGrade())
                .collection("studentList")
                .document(student.getId())
                .update(updates)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating student document", e);
                });

        // Update trip's student status (separate collections for clarity)
        if (tripId != null) {
            String statusCollection = isPickup ? "pickupStatus" : "dropoffStatus";

            Map<String, Object> statusData = new HashMap<>();
            statusData.put("studentId", student.getId());
            statusData.put("studentName", student.getName());
            statusData.put("grade", student.getGrade());
            statusData.put("status", isCompleted);
            statusData.put("timestamp", currentTime);


            db.collection("trips")
                    .document(tripId)
                    .collection(statusCollection)
                    .document(student.getId())
                    .set(statusData)
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error updating trip status collection", e);
                    });
        }
    }

    private String formatTimestamp(long millis) {
        return new SimpleDateFormat("EEE, MMM d yyyy 'at' hh:mm a", Locale.getDefault())
                .format(new Date(millis));
    }

    @Override
    public int getItemCount() {
        return studentList.size();
    }

    static class StudentStatusViewHolder extends RecyclerView.ViewHolder {
        TextView studentNameTextView;
        CheckBox pickedCheckBox, notPickedCheckBox;

        public StudentStatusViewHolder(@NonNull View itemView) {
            super(itemView);
            studentNameTextView = itemView.findViewById(R.id.studentNameTextView);
            pickedCheckBox = itemView.findViewById(R.id.pickedCheckBox);
            notPickedCheckBox = itemView.findViewById(R.id.notPickedCheckBox);
        }
    }
}