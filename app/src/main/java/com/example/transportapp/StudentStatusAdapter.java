package com.example.transportapp;

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

/**
 * Pick-up / drop-off checklist for one trip.
 *
 * Every child starts unmarked for the trip. The status the driver sets is kept in
 * {@link #statusByStudent} (keyed by the student's document ID), not read from an earlier
 * trip, so a child picked up this morning is not pre-ticked as dropped off.
 *
 * Rows are recycled while scrolling, so listeners are detached before the checkboxes are set
 * and the listener looks up the row's current position when it fires. That stops a recycled
 * row from writing one child's status against another child.
 */
public class StudentStatusAdapter extends RecyclerView.Adapter<StudentStatusAdapter.StudentStatusViewHolder> {
    private static final String TAG = "StudentStatusAdapter";

    private final List<Student> studentList;
    private final FirebaseFirestore db;
    private String tripId;
    private final boolean isPickup;
    private final Map<String, Boolean> statusByStudent = new HashMap<>();

    public StudentStatusAdapter(List<Student> studentList, String tripId, boolean isPickup) {
        this.studentList = studentList;
        this.db = FirebaseFirestore.getInstance();
        this.tripId = tripId;
        this.isPickup = isPickup;
    }

    public void setTripId(String tripId) {
        this.tripId = tripId;
    }

    /** TRUE = picked/dropped, FALSE = marked not picked/not dropped, null = not marked yet. */
    public Boolean getStatus(Student student) {
        return student.getId() == null ? null : statusByStudent.get(student.getId());
    }

    @NonNull
    @Override
    public StudentStatusViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_student_status, parent, false);
        StudentStatusViewHolder holder = new StudentStatusViewHolder(view);

        // Listeners are attached once and always resolve the child from the row's *current*
        // position, so a recycled row can never act on the child it showed before.
        holder.pickedCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (holder.settingState) return; // ignore changes made by showStatus()
            onStatusChosen(holder, isChecked ? Boolean.TRUE : null);
        });
        holder.notPickedCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (holder.settingState) return;
            onStatusChosen(holder, isChecked ? Boolean.FALSE : null);
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull StudentStatusViewHolder holder, int position) {
        Student student = studentList.get(position);
        holder.studentNameTextView.setText(student.getName());

        if (isPickup) {
            holder.pickedCheckBox.setText("Picked Up");
            holder.notPickedCheckBox.setText("Not Picked");
        } else {
            holder.pickedCheckBox.setText("Dropped Off");
            holder.notPickedCheckBox.setText("Not Dropped");
        }
        showStatus(holder, getStatus(student));
    }

    private void showStatus(StudentStatusViewHolder holder, Boolean status) {
        holder.settingState = true;
        holder.pickedCheckBox.setChecked(Boolean.TRUE.equals(status));
        holder.notPickedCheckBox.setChecked(Boolean.FALSE.equals(status));
        holder.settingState = false;
    }

    private void onStatusChosen(StudentStatusViewHolder holder, Boolean status) {
        int position = holder.getBindingAdapterPosition();
        if (position == RecyclerView.NO_POSITION || position >= studentList.size()) return;
        Student student = studentList.get(position);
        if (student.getId() == null) return;

        if (status == null) {
            // Driver unticked the box: back to "not marked". Nothing is written, and the trip
            // summary will treat the child as unconfirmed.
            statusByStudent.remove(student.getId());
        } else {
            statusByStudent.put(student.getId(), status);
            if (isPickup) {
                student.updatePickupStatus(status, tripId);
            }
            updateStudentInFirestore(student, status);
        }
        showStatus(holder, status);
    }

    private void updateStudentInFirestore(Student student, boolean isCompleted) {
        if (student.getGrade() == null || student.getId() == null) {
            Log.e(TAG, "Student record is missing its grade or ID; status not saved");
            return;
        }
        Map<String, Object> updates = new HashMap<>();
        long currentTime = System.currentTimeMillis();

        if (isPickup) {
            Map<String, Object> pickupStatus = new HashMap<>();
            pickupStatus.put("isPicked", isCompleted);
            pickupStatus.put("timestamp", currentTime);
            pickupStatus.put("tripId", tripId);
            pickupStatus.put("type", "pickup");
            updates.put("pickupStatus", pickupStatus);
            if (isCompleted) {
                updates.put("pickupTime", formatTimestamp(currentTime));
            }
            updates.put("currentTripId", tripId);
        } else {
            Map<String, Object> dropoffStatus = new HashMap<>();
            dropoffStatus.put("isDropped", isCompleted);
            dropoffStatus.put("timestamp", currentTime);
            dropoffStatus.put("tripId", tripId);
            dropoffStatus.put("type", "dropoff");
            updates.put("dropoffStatus", dropoffStatus);
            if (isCompleted) {
                updates.put("dropOffTime", formatTimestamp(currentTime));
                updates.put("currentTripId", null);
            }
            if (student.getPickupStatus() != null) {
                updates.put("lastPickupTripId", student.getPickupStatus().getTripId());
            }
        }

        db.collection("students")
                .document(student.getGrade())
                .collection("studentList")
                .document(student.getId())
                .update(updates)
                .addOnFailureListener(e -> Log.e(TAG, "Error updating student document", e));

        if (tripId != null && !tripId.isEmpty()) {
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
                    .addOnFailureListener(e -> Log.e(TAG, "Error updating trip status collection", e));
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
        boolean settingState;

        public StudentStatusViewHolder(@NonNull View itemView) {
            super(itemView);
            studentNameTextView = itemView.findViewById(R.id.studentNameTextView);
            pickedCheckBox = itemView.findViewById(R.id.pickedCheckBox);
            notPickedCheckBox = itemView.findViewById(R.id.notPickedCheckBox);
        }
    }
}
