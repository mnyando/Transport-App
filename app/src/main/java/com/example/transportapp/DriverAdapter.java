package com.example.transportapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DriverAdapter extends RecyclerView.Adapter<DriverAdapter.DriverViewHolder> {

    private List<Driver> driverList;
    private OnDriverClickListener listener;

    public DriverAdapter(List<Driver> driverList, OnDriverClickListener listener) {
        this.driverList = driverList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DriverViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_driver, parent, false); // Use custom layout
        return new DriverViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DriverViewHolder holder, int position) {
        Driver driver = driverList.get(position);
        holder.driverNameTextView.setText(driver.getDriverName());
        holder.driverStatusTextView.setText("Status: " + (driver.getStatus() != null ? driver.getStatus() : "Unknown"));
        holder.itemView.setOnClickListener(v -> listener.onDriverClick(driver));
    }

    @Override
    public int getItemCount() {
        return driverList.size();
    }

    static class DriverViewHolder extends RecyclerView.ViewHolder {
        TextView driverNameTextView, driverStatusTextView;

        DriverViewHolder(@NonNull View itemView) {
            super(itemView);
            driverNameTextView = itemView.findViewById(R.id.driverNameTextView);
            driverStatusTextView = itemView.findViewById(R.id.driverStatusTextView);
        }
    }

    public interface OnDriverClickListener {
        void onDriverClick(Driver driver);
    }
}