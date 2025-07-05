package com.example.mealbridge;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.mealbridge.databinding.ActivityDonateFoodBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class DonateFoodActivity extends AppCompatActivity {

    private ActivityDonateFoodBinding binding;
    private FusedLocationProviderClient fusedLocationClient;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance(); // Firestore instance
    private Uri selectedImageUri;
    private File imageFile;

    // TFLite Interpreter for MobileNetV2 model
    private Interpreter tflite;

    // PhotoPicker for Android 14+
    private final ActivityResultLauncher<androidx.activity.result.PickVisualMediaRequest> photoPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    handleImageSelection(uri);
                } else {
                    Toast.makeText(DonateFoodActivity.this, "No image selected", Toast.LENGTH_SHORT).show();
                }
            });

    // Image picker for older Android versions
    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    handleImageSelection(result.getData().getData());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize View Binding
        binding = ActivityDonateFoodBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Setup Food Type Spinner
        setupFoodTypeSpinner();

        // Get Current Location Button Click Listener
        binding.getLocationButton.setOnClickListener(v -> requestLocationPermission());

        // Upload Image Button Click Listener
        binding.uploadImageButton.setOnClickListener(v -> openImagePicker());

        // Submit Donation Button Click Listener
        binding.submitDonationButton.setOnClickListener(v -> handleFoodDonation());

        // Back to Home Button
        binding.backToHomeButton.setOnClickListener(v -> finish());

        // Load the TensorFlow Lite model from assets (model.tflite)
        try {
            tflite = new Interpreter(loadModelFile("model.tflite"));
            Log.d("TFLite", "Model loaded successfully");
        } catch (Exception e) {
            Log.e("TFLite", "Error loading model: " + e.getMessage());
        }
    }

    // Setup Spinner for Food Types
    private void setupFoodTypeSpinner() {
        List<String> foodTypes = Arrays.asList("Select Food Type", "Rice", "Desserts", "Vegetables", "Fruits", "Breads");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, foodTypes);
        binding.foodTypeSpinner.setAdapter(adapter);
    }

    // Open Image Picker based on Android version
    private void openImagePicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            photoPickerLauncher.launch(new androidx.activity.result.PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        } else {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        }
    }

    // Handle Image Selection and Save Locally
    private void handleImageSelection(Uri uri) {
        selectedImageUri = uri;
        imageFile = saveImageLocally(selectedImageUri);

        if (imageFile != null) {
            binding.foodImageView.setVisibility(View.VISIBLE);
            binding.foodImageView.setImageURI(Uri.fromFile(imageFile));
            binding.submitDonationButton.setEnabled(true);
        }
    }

    // Save Image Locally
    private File saveImageLocally(Uri imageUri) {
        try {
            Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

            if (storageDir != null && (!storageDir.exists() && !storageDir.mkdirs())) {
                Log.e("SaveImage", "Failed to create directory");
                Toast.makeText(this, "Failed to create storage directory", Toast.LENGTH_SHORT).show();
                return null;
            }

            imageFile = new File(storageDir, "donation_image_" + System.currentTimeMillis() + ".jpg");

            try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
                Log.d("SaveImage", "Image saved at: " + imageFile.getAbsolutePath());
            }

            Toast.makeText(this, "Image saved locally!", Toast.LENGTH_SHORT).show();

            // Trigger MobileNetV2 prediction
            predictFoodQuality(bitmap);

            return imageFile;

        } catch (IOException e) {
            Log.e("SaveImage", "Failed to save image: " + e.getMessage());
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    // Request Location Permission
    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        } else {
            getCurrentLocation();
        }
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    getCurrentLocation();
                } else {
                    Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show();
                }
            });

    // Get Current Location
    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Task<Location> locationTask = fusedLocationClient.getLastLocation();
        locationTask.addOnSuccessListener(location -> {
            if (location != null) {
                String locationText = String.format(Locale.getDefault(), "Lat: %.5f, Long: %.5f",
                        location.getLatitude(), location.getLongitude());
                binding.pickupLocationEditText.setText(locationText);
            } else {
                Toast.makeText(this, "Unable to get location", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Handle Food Donation Submission
    private void handleFoodDonation() {
        String foodType = binding.foodTypeSpinner.getSelectedItem().toString();
        String foodName = binding.foodNameEditText.getText().toString().trim();
        String quantity = binding.quantityEditText.getText().toString().trim();
        String location = binding.pickupLocationEditText.getText().toString().trim();

        if (foodType.equals("Select Food Type") || foodName.isEmpty() || quantity.isEmpty() || location.isEmpty() || imageFile == null) {
            Toast.makeText(this, "Please fill all details!", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.submitDonationButton.setEnabled(false);
        binding.submitDonationButton.setText("Submitting...");

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please sign in first!", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        Donation donation = new Donation(userId, foodName, foodType, quantity, location);

        db.collection("donations").add(donation)
                .addOnSuccessListener(documentReference -> {
                    // Get the donation document ID from Firestore.
                    String donationId = documentReference.getId();
                    Toast.makeText(this, "Donation submitted successfully!\nConnecting you to the nearest volunteers...", Toast.LENGTH_LONG).show();

                    // Launch SelectVolunteerActivity with donation details.
                    Intent intent = new Intent(DonateFoodActivity.this, SelectVolunteerActivity.class);
                    intent.putExtra("donationId", donationId);
                    intent.putExtra("donationLocation", location);
                    intent.putExtra("donationFoodType", foodType);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to submit donation", Toast.LENGTH_SHORT).show();
                    binding.submitDonationButton.setEnabled(true);
                    binding.submitDonationButton.setText("Submit Donation");
                });
    }

    // ================================================
    // TFLite Helper Methods for MobileNetV2
    // ================================================

    // Load the TFLite model from assets using try-with-resources
    private MappedByteBuffer loadModelFile(String modelPath) throws IOException {
        try (AssetFileDescriptor fileDescriptor = getAssets().openFd(modelPath);
             FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor())) {
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        }
    }

    // Run prediction on the provided bitmap using MobileNetV2
    private void predictFoodQuality(Bitmap bitmap) {
        // Resize to MobileNetV2 expected input size (224x224)
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true);
        ByteBuffer inputBuffer = convertBitmapToByteBuffer(resizedBitmap);
        float[][] output = new float[1][1];  // Adjust output dimensions if necessary

        if (tflite != null) {
            tflite.run(inputBuffer, output);
            float prediction = output[0][0];
            String result;
            int confidence;
            if (prediction > 0.5) {
                result = "Fresh";
                confidence = Math.round(prediction * 100);
            } else {
                result = "Rotten";
                confidence = Math.round((1 - prediction) * 100);
            }
            // Show an AlertDialog with the prediction and confidence score
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Food Quality Prediction")
                    .setMessage("The food is predicted to be " + result +"!"+"\nConfidence Score: " + confidence + "%")
                    .setPositiveButton("OK", null)
                    .show();
            // Update the TextView under the image preview with the prediction
            binding.predictionTextView.setText("Prediction: " + result + "\nConfidence: " + confidence + "%");
            binding.predictionTextView.setVisibility(View.VISIBLE);

            Log.d("TFLite", "Prediction: " + prediction + " (" + result + ", " + confidence + "% confidence)");
        } else {
            Log.e("TFLite", "TFLite interpreter not initialized");
        }
    }

    // Convert a bitmap into a ByteBuffer for TFLite input
    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(4 * 224 * 224 * 3);
        buffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[224 * 224];
        bitmap.getPixels(intValues, 0, 224, 0, 0, 224, 224);
        for (int pixel : intValues) {
            buffer.putFloat(((pixel >> 16) & 0xFF) / 255.0f); // R
            buffer.putFloat(((pixel >> 8) & 0xFF) / 255.0f);  // G
            buffer.putFloat((pixel & 0xFF) / 255.0f);         // B
        }
        return buffer;
    }

    @Override
    protected void onDestroy() {
        if (tflite != null) {
            tflite.close();
            tflite = null;
        }
        super.onDestroy();
    }

    public static class Donation {
        public String userId, foodName, foodType, quantity, location;

        public Donation() {}

        public Donation(String userId, String foodName, String foodType, String quantity, String location) {
            this.userId = userId;
            this.foodName = foodName;
            this.foodType = foodType;
            this.quantity = quantity;
            this.location = location;
        }
    }
}
