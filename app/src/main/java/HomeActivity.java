package com.example.mealbridge;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.example.mealbridge.databinding.ActivityHomeBinding;

public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;
    private Button donateFoodButton, collectFoodButton, profileButton, settingsButton;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);

        // Initialize buttons from binding
        donateFoodButton = binding.donateFoodButton;
        collectFoodButton = binding.collectFoodButton;
        profileButton = binding.profileButton;
        settingsButton = binding.settingsButton;
        //activeDonationButton = binding.activeDonationButton;  // New button for active donation

        // Set up onClick listeners for navigation
        donateFoodButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, DonateFoodActivity.class);
            startActivity(intent);
        });

        collectFoodButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, CollectFoodActivity.class);
            startActivity(intent);
        });

        profileButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

    }
}
