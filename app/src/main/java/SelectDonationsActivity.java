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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SelectDonationsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private DonationAdapter adapter;
    private List<Donation> donationList = new ArrayList<>();
    private FirebaseFirestore db;

    // Volunteer location passed via Intent extras (format: "Lat: 37.42100, Long: -122.08300")
    private double volunteerLat, volunteerLon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_donations);

        recyclerView = findViewById(R.id.recyclerViewDonations);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DonationAdapter(donationList);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        // Get volunteer location from extras
        String volunteerLocationStr = getIntent().getStringExtra("volunteerLocation");
        if (volunteerLocationStr != null) {
            double[] latLon = parseLocationString(volunteerLocationStr);
            volunteerLat = latLon[0];
            volunteerLon = latLon[1];
        } else {
            volunteerLat = 0.0;
            volunteerLon = 0.0;
        }

        fetchDonationData();
    }

    // Query Firestore to fetch donation data from "donations"
    private void fetchDonationData() {
        db.collection("donations").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                donationList.clear();
                for (DocumentSnapshot doc : task.getResult()) {
                    String docId = doc.getId();
                    String foodName = doc.getString("foodName");
                    String foodType = doc.getString("foodType");
                    String locationStr = doc.getString("location"); // e.g., "Lat: 37.42200, Long: -122.08400"
                    String quantity = doc.getString("quantity");
                    String donorUserId = doc.getString("userId");
                    String status = doc.getString("status");
                    if (status == null) {
                        status = "available";
                    }
                    double[] latLon = parseLocationString(locationStr);
                    double distance = calculateDistance(volunteerLat, volunteerLon, latLon[0], latLon[1]);

                    // Create Donation object with document ID for future updates.
                    Donation donation = new Donation(docId, foodName, foodType, locationStr, quantity,
                            donorUserId, latLon[0], latLon[1], distance, "", status);
                    donationList.add(donation);

                    // Now fetch donor's document from "users" collection using donorUserId
                    db.collection("users").document(donorUserId).get().addOnSuccessListener(userDoc -> {
                        if (userDoc.exists()) {
                            String donorName = userDoc.getString("name");
                            donation.donorName = donorName != null ? donorName : "";
                            adapter.notifyDataSetChanged();
                        }
                    });
                }
                // Sort donations by distance (nearest first)
                Collections.sort(donationList, new Comparator<Donation>() {
                    @Override
                    public int compare(Donation d1, Donation d2) {
                        return Double.compare(d1.distance, d2.distance);
                    }
                });
                adapter.notifyDataSetChanged();
            } else {
                Toast.makeText(SelectDonationsActivity.this, "Failed to fetch donations", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Calculate distance (in meters) between two coordinates
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        float[] results = new float[1];
        Location.distanceBetween(lat1, lon1, lat2, lon2, results);
        return results[0];
    }

    // Parse a location string (e.g., "Lat: 37.42200, Long: -122.08400")
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
                Log.e("SelectDonations", "Error parsing location: " + e.getMessage());
            }
        }
        return latLon;
    }

    // Donation data model
    public static class Donation {
        public String docId;
        public String foodName;
        public String foodType;
        public String location;
        public String quantity;
        public String userId;
        public double lat;
        public double lon;
        public double distance;
        public String donorName;
        public String status; // e.g., "available", "claimed"

        public Donation(String docId, String foodName, String foodType, String location, String quantity,
                        String userId, double lat, double lon, double distance, String donorName, String status) {
            this.docId = docId;
            this.foodName = foodName;
            this.foodType = foodType;
            this.location = location;
            this.quantity = quantity;
            this.userId = userId;
            this.lat = lat;
            this.lon = lon;
            this.distance = distance;
            this.donorName = donorName;
            this.status = status;
        }
    }

    // RecyclerView Adapter for Donation items
    public class DonationAdapter extends RecyclerView.Adapter<DonationAdapter.DonationViewHolder> {

        private List<Donation> donationList;

        public DonationAdapter(List<Donation> donationList) {
            this.donationList = donationList;
        }

        @NonNull
        @Override
        public DonationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_donation, parent, false);
            return new DonationViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull DonationViewHolder holder, int position) {
            Donation donation = donationList.get(position);
            holder.foodNameTextView.setText("Food Name: " + donation.foodName);
            holder.foodTypeTextView.setText("Food Type: " + donation.foodType);
            holder.locationTextView.setText("Location: " + donation.location);
            holder.quantityTextView.setText("Quantity: " + donation.quantity);
            holder.donorNameTextView.setText("Donor: " + donation.donorName);
            holder.distanceTextView.setText("Distance: " + Math.round(donation.distance) + " m");
            holder.statusTextView.setText("Status: " + donation.status);

            // Show claim button only if donation is available
            if (donation.status.equals("available")) {
                holder.claimButton.setVisibility(View.VISIBLE);
                holder.claimButton.setOnClickListener(v -> {
                    String volunteerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    db.collection("donations").document(donation.docId)
                            .update("status", "claimed", "collectorId", volunteerId)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(v.getContext(), "Donation claimed successfully", Toast.LENGTH_SHORT).show();
                                donation.status = "claimed";
                                notifyItemChanged(position);
                                //Intent intent = new Intent(SelectDonationsActivity.this, DonationProgressActivity.class);
                                //intent.putExtra("volunteerId", volunteerId);
                                //startActivity(intent);
                                //finish(); // Optionally finish activity
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(v.getContext(), "Failed to claim donation", Toast.LENGTH_SHORT).show();
                            });
                });
            } else {
                holder.claimButton.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return donationList.size();
        }

        public class DonationViewHolder extends RecyclerView.ViewHolder {
            public TextView foodNameTextView, foodTypeTextView, locationTextView, quantityTextView, donorNameTextView, distanceTextView, statusTextView;
            public Button claimButton;

            public DonationViewHolder(@NonNull View itemView) {
                super(itemView);
                foodNameTextView = itemView.findViewById(R.id.textViewFoodName);
                foodTypeTextView = itemView.findViewById(R.id.textViewFoodType);
                locationTextView = itemView.findViewById(R.id.textViewLocation);
                quantityTextView = itemView.findViewById(R.id.textViewQuantity);
                donorNameTextView = itemView.findViewById(R.id.textViewDonorName);
                distanceTextView = itemView.findViewById(R.id.textViewDistance);
                statusTextView = itemView.findViewById(R.id.textViewStatus);
                claimButton = itemView.findViewById(R.id.buttonClaimDonation);
            }
        }
    }
}
