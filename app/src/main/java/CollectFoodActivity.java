package com.example.mealbridge;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.mealbridge.databinding.ActivityCollectFoodBinding;
import com.google.android.gms.location.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CollectFoodActivity extends AppCompatActivity {

    private ActivityCollectFoodBinding binding;
    private FusedLocationProviderClient fusedLocationClient;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance(); // Firestore instance

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCollectFoodBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Set up the food type dropdown (Spinner)
        setupFoodTypeSpinner();

        // Set up click listener for Pickup Time
        binding.pickupTimeEditText.setOnClickListener(v -> showTimePicker());

        // Get Location Button Click Listener
        binding.getLocationButton.setOnClickListener(v -> getCurrentLocation());

        // Submit Button Click Listener
        binding.collectFoodButton.setOnClickListener(v -> handleFoodCollection());

        // Back to Home Button
        binding.backToHomeButton.setOnClickListener(v -> finish());
    }

    // ✅ Set up Spinner for Food Type
    private void setupFoodTypeSpinner() {
        String[] foodTypes = {"Select Food Type", "Cooked", "Raw", "Packaged", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, foodTypes);
        binding.foodTypeSpinner.setAdapter(adapter);
    }

    // ✅ Show Time Picker Dialog
    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (TimePicker view, int selectedHour, int selectedMinute) -> {
                    String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute);
                    binding.pickupTimeEditText.setText(formattedTime);
                }, hour, minute, true);
        timePickerDialog.show();
    }

    // ✅ Get Current Location
    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            return;
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        updateLocationUI(location);
                    } else {
                        requestNewLocation(); // Fallback if location is null
                    }
                }).addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to get location!", Toast.LENGTH_SHORT).show()
                );
    }

    // ✅ Fallback Method for GPS Issues
    private void requestNewLocation() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(2000)
                .build();

        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (!locationResult.getLocations().isEmpty()) {
                    updateLocationUI(locationResult.getLastLocation());
                } else {
                    Toast.makeText(getApplicationContext(), "Location not found", Toast.LENGTH_SHORT).show();
                }
            }
        };

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());
        }
    }

    private void updateLocationUI(Location location) {
        if (location != null) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            String locationText = getString(R.string.location_format, latitude, longitude);
            binding.pickupLocationEditText.setText(locationText);
        }
    }

    // ✅ Handle Food Collection Submission
    // Handle Food Collection Submission
    private void handleFoodCollection() {
        String foodType = binding.foodTypeSpinner.getSelectedItem().toString();
        String pickupLocation = binding.pickupLocationEditText.getText().toString().trim();
        String pickupTime = binding.pickupTimeEditText.getText().toString().trim();

        if (foodType.equals("Select Food Type") || pickupLocation.isEmpty() || pickupTime.isEmpty()) {
            Toast.makeText(this, "Please enter all details!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable button to prevent multiple clicks
        binding.collectFoodButton.setEnabled(false);
        binding.collectFoodButton.setText("Submitting...");

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please sign in before collecting food!", Toast.LENGTH_SHORT).show();
            binding.collectFoodButton.setEnabled(true);
            binding.collectFoodButton.setText("Collect Food");
            return;
        }

        String userId = currentUser.getUid();

        // Create collection data object
        FoodCollection foodCollection = new FoodCollection(
                userId,
                foodType,
                pickupLocation,
                pickupTime
        );

        // Save to Firestore
        db.collection("food_collections")
                .add(foodCollection)
                .addOnSuccessListener(documentReference -> {
                    // Show an engaging message
                    Toast.makeText(this, "Food collection request submitted!\nConnecting you to available donations...", Toast.LENGTH_LONG).show();

                    // Launch SelectDonationsActivity with volunteer's current location
                    Intent intent = new Intent(CollectFoodActivity.this, SelectDonationsActivity.class);
                    intent.putExtra("volunteerLocation", pickupLocation); // Pass pickupLocation as volunteerLocation
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to submit request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    binding.collectFoodButton.setEnabled(true);
                    binding.collectFoodButton.setText("Collect Food");
                });
    }

    // ✅ Request Permissions
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    getCurrentLocation();
                } else {
                    Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show();
                }
            });

    // ✅ Handle Permission Request Results
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    // ✅ FoodCollection Model
    public static class FoodCollection {
        public String userId;
        public String foodType;
        public String pickupLocation;
        public String pickupTime;

        // Default constructor required for Firestore
        public FoodCollection() {}

        public FoodCollection(String userId, String foodType, String pickupLocation, String pickupTime) {
            this.userId = userId;
            this.foodType = foodType;
            this.pickupLocation = pickupLocation;
            this.pickupTime = pickupTime;
        }
    }
}
