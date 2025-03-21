package com.example.transportapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;


// StaffAdapter
class StaffAdapter extends RecyclerView.Adapter<StaffAdapter.StaffViewHolder> {
    private List<Staff> staffList;
    private OnStaffClickListener listener;

    public StaffAdapter(List<Staff> staffList, OnStaffClickListener listener) {
        this.staffList = staffList;
        this.listener = listener;
    }

    @Override
    public StaffViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new StaffViewHolder(view);
    }

    @Override
    public void onBindViewHolder(StaffViewHolder holder, int position) {
        Staff staff = staffList.get(position);
        holder.textView.setText(staff.getStaffName() != null ? staff.getStaffName() : "Unnamed Staff");
        holder.itemView.setOnClickListener(v -> listener.onStaffClick(staff));
    }

    @Override
    public int getItemCount() {
        return staffList.size();
    }

    static class StaffViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        StaffViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
        }
    }

    interface OnStaffClickListener {
        void onStaffClick(Staff staff);
    }
}