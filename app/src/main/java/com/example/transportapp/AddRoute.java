package com.example.transportapp;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class AddRoute extends AppCompatActivity {

    private EditText routeName, routeDetails;
    private Button submitRoute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_route);

        // Link XML elements to Java
        routeName = findViewById(R.id.routeName);
        routeDetails = findViewById(R.id.routeDetails);
        submitRoute = findViewById(R.id.submitRoute);

        // Add functionality to submit the route (this can be linked to Firestore)
        submitRoute.setOnClickListener(view -> {
            String name = routeName.getText().toString();
            String details = routeDetails.getText().toString();

            if (name.isEmpty() || details.isEmpty()) {
                Toast.makeText(AddRoute.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            } else {
                // Logic to add the route to Firestore or another database
                // Example Firestore code
                /*
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                Map<String, Object> route = new HashMap<>();
                route.put("name", name);
                route.put("details", details);

                db.collection("routes")
                    .add(route)
                    .addOnSuccessListener(documentReference -> Toast.makeText(AddRoute.this, "Route added", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(AddRoute.this, "Failed to add route", Toast.LENGTH_SHORT).show());
                */
                Toast.makeText(AddRoute.this, "Route added successfully", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
