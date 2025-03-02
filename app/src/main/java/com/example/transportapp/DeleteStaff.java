package com.example.transportapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.app.AlertDialog;
import android.content.DialogInterface;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DeleteStaff extends AppCompatActivity implements DriverAdapter.OnDriverClickListener {

    private EditText driverNameInput, driverPhoneInput, licenseNumberInput;
    private Button updateDriverButton;
    private RecyclerView driverRecyclerView;
    private DriverAdapter driverAdapter;
    private ArrayList<Driver> driverList;
    private FirebaseFirestore firestore;
    private String selectedDriverId = null; // Holds the selected driver's ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_staff);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Initialize Views
        driverNameInput = findViewById(R.id.driverNameInput);
        driverPhoneInput = findViewById(R.id.driverPhoneInput);
        licenseNumberInput = findViewById(R.id.licenseNumberInput);
        updateDriverButton = findViewById(R.id.saveDriverButton);
        driverRecyclerView = findViewById(R.id.driverRecyclerView);

        // Setup RecyclerView
        driverList = new ArrayList<>();
        driverAdapter = new DriverAdapter(driverList, this); // Pass this as listener
        driverRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        driverRecyclerView.setAdapter(driverAdapter);

        // Load drivers from Firestore
        fetchDrivers();

        // Handle driver updates
        updateDriverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateDriver();
            }
        });
    }

    // Fetch driver list from Firestore
    private void fetchDrivers() {
        firestore.collection("drivers").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(QuerySnapshot queryDocumentSnapshots, FirebaseFirestoreException e) {
                if (e != null) {
                    Toast.makeText(DeleteStaff.this, "Error fetching drivers", Toast.LENGTH_SHORT).show();
                    return;
                }

                driverList.clear();
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    String id = doc.getId();
                    String name = doc.getString("name");
                    String phone = doc.getString("phone");
                    String license = doc.getString("licenseNumber");

                    driverList.add(new Driver(id, name, phone, license));
                }
                driverAdapter.notifyDataSetChanged();
            }
        });
    }

    // Handle driver selection from RecyclerView
    @Override
    public void onDriverClick(Driver driver) {
        selectedDriverId = driver.getId();
        driverNameInput.setText(driver.getName());
        driverPhoneInput.setText(driver.getPhone());
        licenseNumberInput.setText(driver.getLicenseNumber());
    }

    // Handle driver long press for deletion
    @Override
    public void onDriverLongClick(final Driver driver) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Driver")
                .setMessage("Are you sure you want to delete " + driver.getName() + "?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteDriver(driver.getId());
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Delete the driver from Firestore
    private void deleteDriver(String driverId) {
        firestore.collection("drivers").document(driverId)
                .delete()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(DeleteStaff.this, "Driver deleted successfully", Toast.LENGTH_SHORT).show();
                            fetchDrivers(); // Refresh the list
                        } else {
                            Toast.makeText(DeleteStaff.this, "Failed to delete driver", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // Update the driver details in Firestore
    private void updateDriver() {
        if (selectedDriverId == null) {
            Toast.makeText(this, "Select a driver first", Toast.LENGTH_SHORT).show();
            return;
        }

        String updatedName = driverNameInput.getText().toString().trim();
        String updatedPhone = driverPhoneInput.getText().toString().trim();
        String updatedLicense = licenseNumberInput.getText().toString().trim();

        if (TextUtils.isEmpty(updatedName) || TextUtils.isEmpty(updatedPhone) || TextUtils.isEmpty(updatedLicense)) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updatedData = new HashMap<>();
        updatedData.put("name", updatedName);
        updatedData.put("phone", updatedPhone);
        updatedData.put("licenseNumber", updatedLicense);

        firestore.collection("drivers").document(selectedDriverId)
                .update(updatedData)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(DeleteStaff.this, "Driver updated successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(DeleteStaff.this, "Failed to update driver", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
