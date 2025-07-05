package com.example.mealbridge;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class EditProfileActivity extends AppCompatActivity {

    private EditText editName;
    private ImageView profileImage;
    private Button saveButton;
    private Uri imageUri;

    private FirebaseAuth auth;
    private DatabaseReference databaseReference;
    private StorageReference storageReference;
    private FirebaseUser user;

    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData();
                    profileImage.setImageURI(imageUri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        databaseReference = FirebaseDatabase.getInstance().getReference("Users");
        storageReference = FirebaseStorage.getInstance().getReference("profile_pictures");

        editName = findViewById(R.id.editUserName);
        profileImage = findViewById(R.id.editProfileImage);
        saveButton = findViewById(R.id.saveProfileButton);

        profileImage.setOnClickListener(v -> openGallery());
        saveButton.setOnClickListener(v -> saveProfile());
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    private void saveProfile() {
        String name = editName.getText().toString().trim();
        if (name.isEmpty()) {
            editName.setError("Name is required");
            return;
        }

        databaseReference.child(user.getUid()).child("name").setValue(name);

        if (imageUri != null) {
            StorageReference fileRef = storageReference.child(user.getUid() + ".jpg");
            fileRef.putFile(imageUri).addOnSuccessListener(taskSnapshot ->
                    fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        databaseReference.child(user.getUid()).child("profileImageUrl").setValue(uri.toString());
                        Toast.makeText(EditProfileActivity.this, "Profile Updated", Toast.LENGTH_SHORT).show();
                        finish();
                    })
            ).addOnFailureListener(e ->
                    Toast.makeText(EditProfileActivity.this, "Upload Failed", Toast.LENGTH_SHORT).show()
            );
        } else {
            Toast.makeText(EditProfileActivity.this, "Profile Updated", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}
