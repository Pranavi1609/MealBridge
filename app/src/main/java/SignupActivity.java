package com.example.mealbridge;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    private TextInputEditText nameEditText, orgnameEditText, emailEditText, passwordEditText, confirmpasswordEditText, phoneEditText;
    private TextInputLayout orgnameInputLayout;
    private MaterialButton signupButton;
    private MaterialButton googleSignup, facebookSignup;
    private MaterialButton donorButton, ngoButton;
    private TextView donorTypeLabel, volunteerTypelabel;
    private TextView loginRedirectText;
    private MaterialButtonToggleGroup userTypeToggleGroup;
    private String userType = "Donor"; // Default user type
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize UI elements
        nameEditText = findViewById(R.id.nameEditText);
        orgnameEditText = findViewById(R.id.orgnameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmpasswordEditText = findViewById(R.id.confirmpasswordEditText);
        phoneEditText = findViewById(R.id.phoneEditText);  // New phone number field

        signupButton = findViewById(R.id.signupButton);
        googleSignup = findViewById(R.id.googleSignup);
        facebookSignup = findViewById(R.id.facebookSignup);
        donorButton = findViewById(R.id.donorButton);
        ngoButton = findViewById(R.id.ngoButton);
        donorTypeLabel = findViewById(R.id.donorTypeLabel);
        volunteerTypelabel = findViewById(R.id.volunteerTypelabel);
        loginRedirectText = findViewById(R.id.loginRedirectText);
        userTypeToggleGroup = findViewById(R.id.userTypeToggleGroup);
        orgnameInputLayout = findViewById(R.id.orgnameInputLayout);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Underline login text
        loginRedirectText.setPaintFlags(loginRedirectText.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        // Toggle button selection listener
        userTypeToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.donorButton) {
                    userType = "Donor";
                    orgnameInputLayout.setVisibility(View.GONE);
                    updateToggleUI(donorButton, ngoButton, donorTypeLabel, volunteerTypelabel);
                } else if (checkedId == R.id.ngoButton) {
                    userType = "Volunteer";
                    orgnameInputLayout.setVisibility(View.VISIBLE);
                    updateToggleUI(ngoButton, donorButton, volunteerTypelabel, donorTypeLabel);
                }
            }
        });

        // Signup button click event
        signupButton.setOnClickListener(v -> registerUser());

        // Google Signup Click
        googleSignup.setOnClickListener(v -> Toast.makeText(SignupActivity.this, "Google Signup Coming Soon!", Toast.LENGTH_SHORT).show());

        // Facebook Signup Click
        facebookSignup.setOnClickListener(v -> Toast.makeText(SignupActivity.this, "Facebook Signup Coming Soon!", Toast.LENGTH_SHORT).show());

        // Redirect to login
        loginRedirectText.setOnClickListener(v -> {
            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
            startActivity(intent);
        });
    }

    private void updateToggleUI(MaterialButton selected, MaterialButton unselected, TextView selectedDesc, TextView unselectedDesc) {
        selected.setBackgroundColor(getResources().getColor(R.color.highlightbutton, getTheme()));
        selected.setTextColor(Color.WHITE);
        unselected.setBackgroundColor(getResources().getColor(R.color.primary, getTheme()));
        unselected.setTextColor(Color.GRAY);
        selectedDesc.setVisibility(View.VISIBLE);
        unselectedDesc.setVisibility(View.GONE);
    }

    private void registerUser() {
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim(); // Get phone number
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmpasswordEditText.getText().toString().trim();
        String orgName = orgnameEditText.getText().toString().trim();

        // Validate inputs
        if (TextUtils.isEmpty(name)) {
            nameEditText.setError("Name is required!");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required!");
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Enter a valid email!");
            return;
        }
        if (TextUtils.isEmpty(phone)) {
            phoneEditText.setError("Phone number is required!");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Password is required!");
            return;
        }
        if (TextUtils.isEmpty(confirmPassword)) {
            confirmpasswordEditText.setError("Confirm Password is required!");
            return;
        }
        if (password.length() < 8) {
            passwordEditText.setError("Password must be at least 8 characters!");
            return;
        }
        if (!confirmPassword.equals(password)) {
            confirmpasswordEditText.setError("Passwords do not match!");
            return;
        }

        // Create user with Firebase Authentication
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String userId = auth.getCurrentUser().getUid();
                        Map<String, Object> user = new HashMap<>();
                        user.put("name", name);
                        user.put("email", email);
                        user.put("userType", userType);
                        user.put("phone", phone);  // Store phone number
                        if (userType.equals("Volunteer")) {
                            user.put("organizationName", orgName);
                        }

                        db.collection("users").document(userId).set(user)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(SignupActivity.this, "User Registered as " + userType, Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                                    finish();
                                })
                                .addOnFailureListener(e -> Toast.makeText(SignupActivity.this, "Firestore Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    } else {
                        Toast.makeText(SignupActivity.this, "Signup Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
