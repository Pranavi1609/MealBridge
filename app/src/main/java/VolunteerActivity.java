package com.example.mealbridge;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class VolunteerActivity extends AppCompatActivity {

    private EditText volunteerNameEditText, volunteerContactEditText, volunteerAddressEditText;
    private Button volunteerSignUpButton;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_volunteer);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference("Volunteers");

        // Updated to match the correct XML IDs
        volunteerNameEditText = findViewById(R.id.volunteerName);
        volunteerContactEditText = findViewById(R.id.volunteerContact);
        volunteerAddressEditText = findViewById(R.id.volunteerAddress);
        volunteerSignUpButton = findViewById(R.id.volunteerSignUpButton);

        volunteerSignUpButton.setOnClickListener(v -> {
            String volunteerName = volunteerNameEditText.getText().toString().trim();
            String contactInfo = volunteerContactEditText.getText().toString().trim();
            String address = volunteerAddressEditText.getText().toString().trim();

            if (volunteerName.isEmpty() || contactInfo.isEmpty() || address.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            } else {
                Log.d("VolunteerActivity", "Saving volunteer: " + volunteerName);

                String volunteerId = databaseReference.push().getKey();
                Volunteer volunteer = new Volunteer(volunteerName, contactInfo, address);
                if (volunteerId != null) {
                    databaseReference.child(volunteerId).setValue(volunteer)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Volunteer details saved successfully!", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("VolunteerActivity", "Error saving data", e);
                                Toast.makeText(this, "Failed to save volunteer details", Toast.LENGTH_SHORT).show();
                            });
                }
            }
        });
    }

    public static class Volunteer {
        public String volunteerName;
        public String contactInfo;
        public String address;

        public Volunteer() {}

        public Volunteer(String volunteerName, String contactInfo, String address) {
            this.volunteerName = volunteerName;
            this.contactInfo = contactInfo;
            this.address = address;
        }
    }
}
