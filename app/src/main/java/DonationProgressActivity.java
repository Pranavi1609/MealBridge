package com.example.mealbridge;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.example.mealbridge.databinding.ActivityDonationProgressBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.firestore.FirebaseFirestore;

public class DonationProgressActivity extends FragmentActivity implements OnMapReadyCallback {

    private ActivityDonationProgressBinding binding;
    private FirebaseFirestore db;
    private GoogleMap mMap;

    // Donation details (passed via intent)
    private String donationId;
    private String donorUserId;
    private String volunteerUserId;
    private String donationStatus; // e.g., "Assigned", "In Transit", etc.
    private String volunteerPhone;
    // Optional: donorPhone, donorLat, donorLon if needed later
    private double volunteerLat, volunteerLon;

    // UI elements (via binding)
    private ProgressBar progressBar;
    // Use binding.statusTextView instead of textViewStatus:
    // private TextView statusTextView; // we'll use binding.statusTextView directly.
    private Button callButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDonationProgressBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();

        progressBar = binding.progressBarStatus;
        // Ensure your XML defines a TextView with id "statusTextView"
        // You can use binding.statusTextView directly.
        callButton = binding.buttonCall;

        // Get donation ID from intent extras
        donationId = getIntent().getStringExtra("donationId");
        if (donationId == null || donationId.isEmpty()) {
            Toast.makeText(this, "Donation ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapView);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        loadDonationDetails();

        // Set up call button (deep linking to dialer)
        callButton.setOnClickListener(v -> {
            if (volunteerPhone != null && !volunteerPhone.isEmpty()) {
                Intent dialIntent = new Intent(Intent.ACTION_DIAL);
                dialIntent.setData(Uri.parse("tel:" + volunteerPhone));
                startActivity(dialIntent);
            } else {
                Toast.makeText(DonationProgressActivity.this, "Phone number not available", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadDonationDetails() {
        db.collection("donations").document(donationId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                donationStatus = documentSnapshot.getString("status");
                donorUserId = documentSnapshot.getString("userId");
                volunteerUserId = documentSnapshot.getString("assignedVolunteerId"); // or "collectorId"
                // For this example, assume volunteer location (lat/lon) are stored in donation doc
                Double vLat = documentSnapshot.getDouble("volunteerLat");
                Double vLon = documentSnapshot.getDouble("volunteerLon");
                if (vLat != null && vLon != null) {
                    volunteerLat = vLat;
                    volunteerLon = vLon;
                }
                updateStatusUI(donationStatus);
                loadUserPhone(volunteerUserId, false);
            } else {
                Toast.makeText(DonationProgressActivity.this, "Donation details not found", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e ->
                Toast.makeText(DonationProgressActivity.this, "Error loading donation: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }

    // Loads phone number from "users" collection for volunteer (if isDonor is false)
    private void loadUserPhone(String userId, boolean isDonor) {
        if (userId == null || userId.isEmpty()) return;
        db.collection("users").document(userId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String phone = doc.getString("phone");
                if (!isDonor) {
                    volunteerPhone = phone;
                    // Optionally get volunteer location if not stored in donation doc:
                    Double lat = doc.getDouble("lat");
                    Double lon = doc.getDouble("lon");
                    if (lat != null && lon != null) {
                        volunteerLat = lat;
                        volunteerLon = lon;
                        updateMap();
                    }
                }
            }
        });
    }

    private void updateStatusUI(String status) {
        if (status == null) status = "Pending";
        binding.statusTextView.setText("Current Status: " + status);
        int progress;
        switch (status) {
            case "Assigned":
                progress = 33;
                break;
            case "In Transit":
                progress = 66;
                break;
            case "Arrived":
                progress = 80;
                break;
            case "Completed":
                progress = 100;
                break;
            default:
                progress = 0;
        }
        progressBar.setProgress(progress);
    }

    private void updateMap() {
        if (mMap != null && volunteerLat != 0.0 && volunteerLon != 0.0) {
            LatLng volunteerLatLng = new LatLng(volunteerLat, volunteerLon);
            mMap.clear();
            mMap.addMarker(new com.google.android.gms.maps.model.MarkerOptions()
                    .position(volunteerLatLng)
                    .title("Volunteer Location"));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(volunteerLatLng, 15));
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        updateMap();
    }
}
