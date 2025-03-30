package com.example.transportapp;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class ChildAdapter extends RecyclerView.Adapter<ChildAdapter.ChildViewHolder> {

    private List<Student> studentList;
    private static final String TAG = "ChildAdapter";

    public ChildAdapter(List<Student> studentList) {
        this.studentList = studentList != null ? studentList : new ArrayList<>();
    }

    @NonNull
    @Override
    public ChildViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.parent_child_info, parent, false);
        return new ChildViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChildViewHolder holder, int position) {
        Student student = studentList.get(position);
        Log.d(TAG, "Binding student: " + student.getName() + " | Grade: " + student.getGrade());
        holder.bindStudentData(student);
    }

    @Override
    public int getItemCount() {
        return studentList.size();
    }

    public void updateList(List<Student> newStudentList) {
        this.studentList = newStudentList != null ? newStudentList : new ArrayList<>();
        notifyDataSetChanged();
    }

    static class ChildViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvChildName, tvGrade, tvRoute, tvVehicle,
                tvDriver, tvAttendant, tvPickup, tvDropoff;

        ChildViewHolder(View itemView) {
            super(itemView);
            tvChildName = itemView.findViewById(R.id.tvChildName);
            tvGrade = itemView.findViewById(R.id.tvGrade);
            tvRoute = itemView.findViewById(R.id.tvRoute);
            tvVehicle = itemView.findViewById(R.id.tvVehicle);
            tvDriver = itemView.findViewById(R.id.tvDriver);
            tvAttendant = itemView.findViewById(R.id.tvAttendant);
            tvPickup = itemView.findViewById(R.id.tvPickupTime);
            tvDropoff = itemView.findViewById(R.id.tvDropOffTime);
        }

        void bindStudentData(Student student) {
            // Basic student info
            tvChildName.setText(formatValue(student.getName()));
            tvGrade.setText(formatLabelValue("", student.getGrade()));

            // Transportation info
            tvRoute.setText(formatLabelValue("Route", student.getRoute()));
            tvVehicle.setText(formatLabelValue("Vehicle", student.getVehicle()));
            tvDriver.setText(formatStaffInfo("Driver",
                    student.getDriverName(), student.getDriverPhone()));
            tvAttendant.setText(formatStaffInfo("Attendant",
                    student.getAttendantName(), student.getAttendantPhone()));
            tvPickup.setText(formatLabelValue("Pickup", student.getPickupTime()));
            tvDropoff.setText(formatLabelValue("Drop-off", student.getDropOffTime()));
        }

        private String formatValue(String value) {
            return TextUtils.isEmpty(value) ? "N/A" : value;
        }

        private String formatLabelValue(String label, String value) {
            return String.format("%s: %s", label, formatValue(value));
        }

        private String formatStaffInfo(String role, String name, String phone) {
            if (TextUtils.isEmpty(name) && TextUtils.isEmpty(phone)) {
                return String.format("%s: N/A", role);
            }
            return String.format("%s: %s (%s)",
                    role,
                    formatValue(name),
                    formatValue(phone));
        }
    }
}