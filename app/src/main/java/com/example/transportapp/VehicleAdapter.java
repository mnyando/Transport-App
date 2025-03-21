package com.example.transportapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class VehicleAdapter extends RecyclerView.Adapter<VehicleAdapter.VehicleViewHolder> {

    private List<Vehicle> vehicleList;
    private OnVehicleClickListener onVehicleClickListener;

    public interface OnVehicleClickListener {
        void onVehicleClick(Vehicle vehicle);
    }

    public VehicleAdapter(List<Vehicle> vehicleList, OnVehicleClickListener listener) {
        this.vehicleList = vehicleList;
        this.onVehicleClickListener = listener;
    }

    @NonNull
    @Override
    public VehicleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_vehicle, parent, false);
        return new VehicleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VehicleViewHolder holder, int position) {
        Vehicle vehicle = vehicleList.get(position);
        holder.vehicleName.setText(vehicle.getVehicleName());
        holder.vehicleNumber.setText(vehicle.getVehicleNumber());
        holder.capacity.setText("Capacity: " + vehicle.getCapacity());
        holder.status.setText("Status: " + (vehicle.getStatus() != null ? vehicle.getStatus() : "Unknown")); // Bind status

        holder.itemView.setOnClickListener(v -> onVehicleClickListener.onVehicleClick(vehicle));
    }

    @Override
    public int getItemCount() {
        return vehicleList.size();
    }

    public static class VehicleViewHolder extends RecyclerView.ViewHolder {
        TextView vehicleName, vehicleNumber, capacity, status; // Added status TextView

        public VehicleViewHolder(@NonNull View itemView) {
            super(itemView);
            vehicleName = itemView.findViewById(R.id.vehicleName);
            vehicleNumber = itemView.findViewById(R.id.vehicleNumber);
            capacity = itemView.findViewById(R.id.vehicleCapacity);
            status = itemView.findViewById(R.id.vehicleStatus); // Initialize status TextView
        }
    }
}