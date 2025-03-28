package com.example.transportapp;

import android.text.TextUtils;
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

    public ChildAdapter(List<Student> studentList) {
        this.studentList = studentList != null ? studentList : new ArrayList<>();
    }

    @NonNull
    @Override
    public ChildViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.parent_child_info, parent, false);
        return new ChildViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChildViewHolder holder, int position) {
        holder.setData(studentList.get(position));
    }

    @Override
    public int getItemCount() {
        return studentList.size();
    }

    public void updateList(List<Student> newStudentList) {
        this.studentList = newStudentList != null ? newStudentList : new ArrayList<>();
        notifyDataSetChanged(); // Directly refresh the list
    }

    static class ChildViewHolder extends RecyclerView.ViewHolder {
        TextView tvChildName, tvGrade, tvRoute, tvVehicle, tvDriver, tvAttendant, tvPickupTime, tvDropOffTime;

        ChildViewHolder(View itemView) {
            super(itemView);
            tvChildName = itemView.findViewById(R.id.tvChildName);
            tvGrade = itemView.findViewById(R.id.tvGrade);
            tvRoute = itemView.findViewById(R.id.tvRoute);
            tvVehicle = itemView.findViewById(R.id.tvVehicle);
            tvDriver = itemView.findViewById(R.id.tvDriver);
            tvAttendant = itemView.findViewById(R.id.tvAttendant);
            tvPickupTime = itemView.findViewById(R.id.tvPickupTime);
            tvDropOffTime = itemView.findViewById(R.id.tvDropOffTime);
        }

        void setData(Student student) {
            tvChildName.setText(student.getName());
            tvGrade.setText(formatText("Grade", student.getGrade()));
            tvRoute.setText(formatText("Route", student.getRoute()));
            tvVehicle.setText(formatText("Vehicle", student.getVehicle()));
            tvDriver.setText(formatText("Driver", student.getDriver(), student.getDriverPhone()));
            tvAttendant.setText(formatText("Attendant", student.getAttendantName(), student.getAttendantPhone()));
            tvPickupTime.setText(formatText("Pickup", student.getPickupTime()));
            tvDropOffTime.setText(formatText("Drop-off", student.getDropOffTime()));
        }

        private String formatText(String label, String value) {
            return label + ": " + (TextUtils.isEmpty(value) ? "N/A" : value);
        }

        private String formatText(String label, String name, String phone) {
            return label + ": " + (TextUtils.isEmpty(name) ? "N/A" : name) +
                    (!TextUtils.isEmpty(phone) ? " (" + phone + ")" : "");
        }
    }
}
