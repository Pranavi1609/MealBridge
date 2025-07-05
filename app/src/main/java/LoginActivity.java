package com.example.mealbridge;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {
    private TextInputEditText emailEditText, passwordEditText;
    private MaterialButton loginButton, signupButton;
    private ProgressBar progressBar;
    private MaterialButtonToggleGroup userTypeToggle;
    private MaterialButton toggleDonor, toggleVolunteer;
    private TextView donorDescription, volunteerDescription;
    private String selectedUserType = "Donor";
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize UI elements
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        signupButton = findViewById(R.id.signupButton);
        progressBar = findViewById(R.id.progressBar);
        userTypeToggle = findViewById(R.id.userTypeToggle);
        toggleDonor = findViewById(R.id.toggleDonor);
        toggleVolunteer = findViewById(R.id.toggleVolunteer);
        donorDescription = findViewById(R.id.donorDescription);
        volunteerDescription = findViewById(R.id.volunteerDescription);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Toggle button selection listener
        userTypeToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.toggleDonor) {
                    selectedUserType = "Donor";
                    updateToggleUI(toggleDonor, toggleVolunteer, donorDescription, volunteerDescription);
                } else if (checkedId == R.id.toggleVolunteer) {
                    selectedUserType = "Volunteer";
                    updateToggleUI(toggleVolunteer, toggleDonor, volunteerDescription, donorDescription);
                }
            }
        });

        // Login button click listener
        loginButton.setOnClickListener(view -> handleLogin());

        // Signup button click listener
        signupButton.setOnClickListener(view -> handleSignup());
    }

    private void updateToggleUI(MaterialButton selected, MaterialButton unselected, TextView selectedDesc, TextView unselectedDesc) {
        selected.setBackgroundColor(getResources().getColor(R.color.highlightbutton, getTheme()));
        selected.setTextColor(Color.WHITE);
        unselected.setBackgroundColor(getResources().getColor(R.color.primary, getTheme()));
        unselected.setTextColor(Color.GRAY);
        selectedDesc.setVisibility(View.VISIBLE);
        unselectedDesc.setVisibility(View.GONE);
    }

    private void handleLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Please enter email", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please enter password", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Enter a valid email!");
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        loginButton.setEnabled(false);

        //Check if email exists in Firestore before authentication**
        db.collection("users").whereEqualTo("email", email).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        // **Email exists, proceed with authentication**
                        authenticateUser(email, password);
                    } else {
                        // **Email does not exist, show error**
                        progressBar.setVisibility(View.GONE);
                        loginButton.setEnabled(true);
                        emailEditText.setError("No account found with this email. Please sign up!");
                        Toast.makeText(LoginActivity.this, "No account found with this email!", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    //Authenticate User with Firebase**
    private void authenticateUser(String email, String password) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    loginButton.setEnabled(true);

                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            checkUserType(user.getUid());
                        }
                    } else {
                        Exception exception = task.getException();
                        String errorMsg;

                        if (exception instanceof FirebaseAuthInvalidCredentialsException) {
                            errorMsg = "Incorrect password! Try again.";
                            passwordEditText.setError(errorMsg);
                        } else {
                            errorMsg = "Login failed! " + exception.getMessage();
                        }

                        Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void checkUserType(String userId) {
        db.collection("users").document(userId).get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    loginButton.setEnabled(true);
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String storedUserType = document.getString("userType");

                            if (storedUserType != null && storedUserType.equals(selectedUserType)) {
                                Toast.makeText(LoginActivity.this, "Login Successful as " + storedUserType, Toast.LENGTH_SHORT).show();
                                // Redirect user based on role
                                if (storedUserType.equals("Donor")) {
                                    startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                                } else {
                                    startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                                }
                                finish();
                            } else {
                                Toast.makeText(LoginActivity.this, "You are registered as " + storedUserType + ". Please log in with the correct role.", Toast.LENGTH_LONG).show();
                                auth.signOut(); // Sign out the user
                            }
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "Error fetching user type", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void handleSignup() {
        Toast.makeText(this, "Signup", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(LoginActivity.this, SignupActivity.class));
        finish();
    }
}
