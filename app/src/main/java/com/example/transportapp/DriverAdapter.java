package com.example.transportapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class DriverAdapter extends RecyclerView.Adapter<DriverAdapter.DriverViewHolder> {

    private ArrayList<Driver> driverList;
    private OnDriverClickListener listener;

    public interface OnDriverClickListener {
        void onDriverClick(Driver driver);

        // Handle driver long press for deletion
        void onDriverLongClick(Driver driver);
    }

    public DriverAdapter(ArrayList<Driver> driverList, OnDriverClickListener listener) {
        this.driverList = driverList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DriverViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_driver, parent, false);
        return new DriverViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DriverViewHolder holder, int position) {
        Driver driver = driverList.get(position);
        holder.driverName.setText(driver.getName());
        holder.itemView.setOnClickListener(v -> listener.onDriverClick(driver));
    }

    @Override
    public int getItemCount() {
        return driverList.size();
    }

    public static class DriverViewHolder extends RecyclerView.ViewHolder {
        TextView driverName;

        public DriverViewHolder(@NonNull View itemView) {
            super(itemView);
            driverName = itemView.findViewById(R.id.driverName);
        }
    }
}
