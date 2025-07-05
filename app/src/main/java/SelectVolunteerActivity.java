package com.example.mealbridge;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;  // Required for ViewGroup
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SelectVolunteerActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private VolunteerAdapter adapter;
    private List<Volunteer> volunteerList = new ArrayList<>();
    private FirebaseFirestore db;

    // Donation details passed via intent extras
    private String donationId;
    private double donationLat, donationLon;
    private String donationFoodType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_volunteers);

        recyclerView = findViewById(R.id.recyclerViewVolunteers);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new VolunteerAdapter(volunteerList);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        // Get donation details from intent extras
        donationId = getIntent().getStringExtra("donationId");
        donationFoodType = getIntent().getStringExtra("donationFoodType");
        String donationLocationStr = getIntent().getStringExtra("donationLocation");
        if (donationLocationStr != null) {
            double[] latLon = parseLocationString(donationLocationStr);
            donationLat = latLon[0];
            donationLon = latLon[1];
        } else {
            donationLat = 0.0;
            donationLon = 0.0;
        }

        fetchVolunteerData();
    }

    // Fetch volunteer data from "food_collections"
    private void fetchVolunteerData() {
        db.collection("food_collections").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                volunteerList.clear();
                for (DocumentSnapshot doc : task.getResult()) {
                    String foodType = doc.getString("foodType");
                    String pickupLocationStr = doc.getString("pickupLocation");
                    String pickupTime = doc.getString("pickupTime");
                    String volunteerUserId = doc.getString("userId");

                    double[] latLon = parseLocationString(pickupLocationStr);
                    double distance = calculateDistance(donationLat, donationLon, latLon[0], latLon[1]);

                    // Create a Volunteer object with an empty volunteerName initially
                    Volunteer volunteer = new Volunteer(foodType, pickupLocationStr, pickupTime,
                            volunteerUserId, latLon[0], latLon[1], distance, "");
                    volunteerList.add(volunteer);

                    // Now fetch the volunteer's document from the "users" collection using volunteerUserId
                    db.collection("users").document(volunteerUserId).get().addOnSuccessListener(userDoc -> {
                        if (userDoc.exists()) {
                            String name = userDoc.getString("name");
                            String orgName = userDoc.getString("organizationName");
                            volunteer.volunteerName = name != null ? name : "";
                            volunteer.organizationName = orgName != null ? orgName : "";
                            adapter.notifyDataSetChanged();
                        }
                    });
                }
                // Sort volunteers by distance (nearest first)
                Collections.sort(volunteerList, new Comparator<Volunteer>() {
                    @Override
                    public int compare(Volunteer v1, Volunteer v2) {
                        return Double.compare(v1.distance, v2.distance);
                    }
                });
                adapter.notifyDataSetChanged();
            } else {
                Toast.makeText(SelectVolunteerActivity.this, "Failed to fetch volunteers", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Calculate distance (in meters) between two coordinates
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        float[] results = new float[1];
        Location.distanceBetween(lat1, lon1, lat2, lon2, results);
        return results[0];
    }

    // Parse a location string (e.g., "Lat: 37.42200, Long: -122.08400" or "Latitude: 37.421998, Longitude: -122.084000")
    private double[] parseLocationString(String locationStr) {
        double[] latLon = new double[2];
        if (locationStr != null) {
            try {
                locationStr = locationStr.replace("Lat:", "")
                        .replace("Latitude:", "")
                        .replace("Long:", "")
                        .replace("Longitude:", "").trim();
                String[] parts = locationStr.split(",");
                if (parts.length >= 2) {
                    latLon[0] = Double.parseDouble(parts[0].trim());
                    latLon[1] = Double.parseDouble(parts[1].trim());
                }
            } catch (Exception e) {
                Log.e("SelectVolunteer", "Error parsing location: " + e.getMessage());
            }
        }
        return latLon;
    }

    // Volunteer model class
    public static class Volunteer {
        public String foodType;
        public String pickupLocation;
        public String pickupTime;
        public String userId;
        public double lat;
        public double lon;
        public double distance;
        public String volunteerName; // Volunteer name
        public String organizationName; // Organization name

        public Volunteer(String foodType, String pickupLocation, String pickupTime, String userId,
                         double lat, double lon, double distance, String volunteerName) {
            this.foodType = foodType;
            this.pickupLocation = pickupLocation;
            this.pickupTime = pickupTime;
            this.userId = userId;
            this.lat = lat;
            this.lon = lon;
            this.distance = distance;
            this.volunteerName = volunteerName;
            this.organizationName = "";
        }
    }

    // RecyclerView Adapter for Volunteer items
    public class VolunteerAdapter extends RecyclerView.Adapter<VolunteerAdapter.VolunteerViewHolder> {

        private List<Volunteer> volunteerList;

        public VolunteerAdapter(List<Volunteer> volunteerList) {
            this.volunteerList = volunteerList;
        }

        @NonNull
        @Override
        public VolunteerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_volunteer, parent, false);
            return new VolunteerViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull VolunteerViewHolder holder, int position) {
            Volunteer volunteer = volunteerList.get(position);
            holder.foodTypeTextView.setText("Food Type: " + volunteer.foodType);
            holder.pickupLocationTextView.setText("Location: " + volunteer.pickupLocation);
            holder.pickupTimeTextView.setText("Pickup Time: " + volunteer.pickupTime);
            holder.distanceTextView.setText("Distance: " + Math.round(volunteer.distance) + " m");
            holder.volunteerNameTextView.setText("Name: " + volunteer.volunteerName);
            holder.organizationNameTextView.setText("Org: " + volunteer.organizationName);

            // Set click listener to allow donor to select this volunteer
            holder.selectButton.setOnClickListener(v -> {
                // Ensure donationId is available
                if (donationId != null && !donationId.isEmpty()) {
                    String selectedVolunteerId = volunteer.userId;
                    // Update the donation document with the selected volunteer's id
                    db.collection("donations").document(donationId)
                            .update("status", "claimed", "collectorId", selectedVolunteerId)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(v.getContext(), "Volunteer selected successfully!", Toast.LENGTH_SHORT).show();
                                //Intent intent = new Intent(SelectVolunteerActivity.this, DonationProgressActivity.class);
                                //intent.putExtra("donationId", donationId);
                                //startActivity(intent);
                                finish(); // Optionally finish activity
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(v.getContext(), "Failed to select volunteer", Toast.LENGTH_SHORT).show();
                            });
                } else {
                    Toast.makeText(v.getContext(), "Donation ID not found", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return volunteerList.size();
        }

        public class VolunteerViewHolder extends RecyclerView.ViewHolder {
            public TextView foodTypeTextView, pickupLocationTextView, pickupTimeTextView, distanceTextView, volunteerNameTextView, organizationNameTextView;
            public Button selectButton;

            public VolunteerViewHolder(@NonNull View itemView) {
                super(itemView);
                foodTypeTextView = itemView.findViewById(R.id.textViewFoodType);
                pickupLocationTextView = itemView.findViewById(R.id.textViewPickupLocation);
                pickupTimeTextView = itemView.findViewById(R.id.textViewPickupTime);
                distanceTextView = itemView.findViewById(R.id.textViewDistance);
                volunteerNameTextView = itemView.findViewById(R.id.textViewVolunteerName);
                organizationNameTextView = itemView.findViewById(R.id.textViewOrganizationName);
                selectButton = itemView.findViewById(R.id.buttonSelectVolunteer);
            }
        }
    }
}
