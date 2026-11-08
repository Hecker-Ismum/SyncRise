package com.example.syncrise;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.syncrise.databinding.ActivityProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;
    private Uri selectedImageUri;
    private FirebaseAuth mAuth;
    private StorageReference storageRef;

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    binding.ivProfilePicture.setImageURI(uri);
                    binding.btnUploadImage.setVisibility(View.VISIBLE);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        storageRef = FirebaseStorage.getInstance().getReference();

        setSupportActionBar(binding.toolbarProfile);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbarProfile.setNavigationOnClickListener(v -> finish());

        if (user != null) {
            binding.tvUserEmail.setText(user.getEmail());
            loadProfilePicture(user.getUid());
        }

        binding.btnSelectImage.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        binding.btnUploadImage.setOnClickListener(v -> uploadImageToFirebase(user));
        binding.btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadProfilePicture(String uid) {
        StorageReference profileRef = storageRef.child("profile_pictures/" + uid + ".jpg");
        profileRef.getDownloadUrl().addOnSuccessListener(uri -> {
            Glide.with(this)
                    .load(uri)
                    .centerCrop()
                    .into(binding.ivProfilePicture);
        }).addOnFailureListener(e -> {
            // No profile picture yet, ignore
        });
    }

    private void uploadImageToFirebase(FirebaseUser user) {
        if (selectedImageUri == null || user == null) return;

        binding.progressBarProfile.setVisibility(View.VISIBLE);
        StorageReference profileRef = storageRef.child("profile_pictures/" + user.getUid() + ".jpg");

        profileRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    binding.progressBarProfile.setVisibility(View.GONE);
                    binding.btnUploadImage.setVisibility(View.GONE);
                    Toast.makeText(this, "Profile picture updated!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    binding.progressBarProfile.setVisibility(View.GONE);
                    Toast.makeText(this, "Upload failed.", Toast.LENGTH_SHORT).show();
                });
    }
}