package com.example.guysapp;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.auth.UserProfileChangeRequest;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseUser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    // UI
    private ImageView imageViewProfile;

    private EditText firstNameEditText;
    private EditText lastNameEditText;
    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText confirmPasswordEditText;

    private Button chooseImageButton;
    private Button takePhotoButton;
    private Button registerButton;

    private TextView goToLoginText;

    // Image state
    private Bitmap selectedBitmap = null;
    private Uri cameraImageUri = null;

    // Launchers
    private ActivityResultLauncher<Intent> imageResultLauncher;
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initViews();
        setupListeners();
        initActivityResultLaunchers();
    }

    private void initViews() {
        imageViewProfile = findViewById(R.id.imageview_profile);
        firstNameEditText = findViewById(R.id.edittext_first_name);
        lastNameEditText = findViewById(R.id.edittext_last_name);
        emailEditText = findViewById(R.id.edittext_email);
        passwordEditText = findViewById(R.id.edittext_password);
        confirmPasswordEditText = findViewById(R.id.edittext_confirm_password);
        chooseImageButton = findViewById(R.id.button_choose_image);
        takePhotoButton = findViewById(R.id.button_take_photo);
        registerButton = findViewById(R.id.button_register);
        goToLoginText = findViewById(R.id.text_login);
    }

    private void setupListeners() {

        chooseImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseImage();
            }
        });

        takePhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto();
            }
        });

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });

        // Navigate to Login screen
        goToLoginText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                finish();
            }
        });
    }
    // ׳׳×׳—׳•׳ ׳”׳׳ ׳’׳ ׳•׳ ׳™׳ ׳©׳׳§׳‘׳׳™׳ ׳׳™׳“׳¢ ׳׳’׳׳¨׳™׳” ׳׳• ׳׳¦׳׳׳”)
    private void initActivityResultLaunchers() {

        imageResultLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        new ActivityResultCallback<ActivityResult>() {
                            @Override
                            public void onActivityResult(ActivityResult result) {
                                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                                    Uri imageUri = result.getData().getData();
                                    loadBitmapFromUri(imageUri);
                                }
                            }
                        }
                );

        requestCameraPermissionLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.RequestPermission(),
                        new ActivityResultCallback<Boolean>() {
                            @Override
                            public void onActivityResult(Boolean isGranted) {
                                if (isGranted) {
                                    openCamera();
                                } else {
                                    Toast.makeText(RegisterActivity.this,
                                            "Camera permission is required to take a photo",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                );

        cameraLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.TakePicture(),
                        new ActivityResultCallback<Boolean>() {
                            @Override
                            public void onActivityResult(Boolean result) {
                                if (result) {
                                    loadBitmapFromUri(cameraImageUri);
                                }
                            }
                        }
                );
    }

    private void chooseImage() {
        Intent intent = new Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        );
        imageResultLauncher.launch(intent);
    }

    private void takePhoto() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {

            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            return;
        }
        openCamera();
    }

    private void openCamera() {
        cameraImageUri = ImageHelper.createImageUri(this, "Profile Picture", "User profile photo during registration");
        if (cameraImageUri != null) {
            cameraLauncher.launch(cameraImageUri);
        } else {
            Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadBitmapFromUri(Uri uri) {

        if (uri == null) {
            Toast.makeText(this, "Invalid image", Toast.LENGTH_SHORT).show();
            return;
        }

        selectedBitmap = ImageHelper.loadBitmapFromUri(this, uri);

        if (selectedBitmap != null) {
            imageViewProfile.setImageBitmap(selectedBitmap);
        } else {
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
        }
    }

    private void registerUser() {
        String firstName = firstNameEditText.getText().toString().trim();
        String lastName = lastNameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();
        String confirmPassword = confirmPasswordEditText.getText().toString();

        if (firstName.isEmpty() || lastName.isEmpty()
                || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {

            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            clearPasswords();
            return;
        }

        if (selectedBitmap == null) {
            Toast.makeText(this, "Please select a profile image", Toast.LENGTH_SHORT).show();
            return;
        }

        //TODO
        registerButton.setEnabled(false);
        FBRef.mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = FBRef.mAuth.getCurrentUser();
                            if (user != null) {
                                RegisterActivity.this.saveUserWithImage(
                                        user.getUid(),
                                        firstName,
                                        lastName,
                                        email
                                );
                            }
                        } else {
                            String errorMsg = "Registration failed";
                            if (task.getException() != null) {
                                errorMsg = task.getException().getMessage();
                            }
                            Toast.makeText(RegisterActivity.this,
                                    errorMsg,
                                    Toast.LENGTH_LONG).show();
                            //TODO
                            registerButton.setEnabled(true);
                        }
                    }
                });
    }




    private void saveUserWithImage(String userId, String firstName, String lastName, String email) {
        try {
            if (selectedBitmap == null) {
                handleRollback("Please select a profile image");
                return;
            }

            // ---  ׳©׳™׳׳•׳© ׳‘-Blob---
            com.google.firebase.firestore.Blob imageDataBlob = ImageHelper.bitmapToBlob(this, selectedBitmap, ImageHelper.SMALL_IMAGE);

            if (imageDataBlob == null) {
                handleRollback("Failed to process image");
                return;
            }

            Map<String, Object> userMap = new HashMap<>();
            userMap.put("userId", userId);
            userMap.put("firstName", firstName);
            userMap.put("lastName", lastName);
            userMap.put("email", email);
            userMap.put("imageData", imageDataBlob); // ׳©׳׳™׳¨׳” ׳›-Blob

            final String fullName = firstName + " " + lastName;

            FBRef.refUsers.document(userId)
                    .set(userMap)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                FirebaseUser user = FBRef.mAuth.getCurrentUser();
                                if (user != null) {
                                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                            .setDisplayName(fullName)
                                            .build();

                                    user.updateProfile(profileUpdates)
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> updateTask) {

                                                    // ׳©׳—׳¨׳•׳¨ ׳–׳™׳›׳¨׳•׳
                                                    selectedBitmap = null;

                                                    registerButton.setEnabled(true);
                                                    Toast.makeText(RegisterActivity.this, "Registration Successful", Toast.LENGTH_SHORT).show();
                                                    RegisterActivity.this.startActivity(new Intent(RegisterActivity.this, HomeActivity.class));
                                                    RegisterActivity.this.finish();
                                                }
                                            });
                                }
                            } else {
                                String error = task.getException() != null ? task.getException().getMessage() : "Database error";
                                RegisterActivity.this.handleRollback(error);
                            }
                        }
                    });

        } catch (Exception e) {
            handleRollback("Image error: " + e.getMessage());
        }
    }



    // ׳₪׳•׳ ׳§׳¦׳™׳™׳× ׳”׳¢׳–׳¨ ׳©׳“׳•׳׳’׳× ׳׳׳—׳•׳§ ׳׳× ׳”׳׳©׳×׳׳© ׳׳”-Auth ׳׳ ׳׳©׳”׳• ׳ ׳›׳©׳
    private void handleRollback(String errorMsg) {
        Log.d("handleRollback",errorMsg);
        Toast.makeText(RegisterActivity.this, errorMsg, Toast.LENGTH_LONG).show();

        FirebaseUser currentUser = FBRef.mAuth.getCurrentUser();


        String email = null;

        if (currentUser != null) {
            email = currentUser.getEmail();
        }

        // if (email != mull) return email else return "no email"
        Log.d("handleRollback", email != null ? email : "no email");

        if (currentUser != null) {
            // ׳׳—׳™׳§׳× ׳”׳׳©׳×׳׳© ׳׳”-Authentication
            currentUser.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    //TODO
                    registerButton.setEnabled(true);
                    if (task.isSuccessful()) {
                        // ׳”׳׳©׳×׳׳© ׳ ׳׳—׳§
                        Toast.makeText(RegisterActivity.this, "OK", Toast.LENGTH_SHORT).show();
                        Log.d("handleRollback", "׳”׳׳©׳×׳׳© ׳ ׳׳—׳§");
                    }
                    else {
                        Exception e = task.getException();
                        // ׳”׳׳©׳×׳׳© ׳׳ ׳ ׳׳—׳§
                        Toast.makeText(RegisterActivity.this, "ERROR", Toast.LENGTH_SHORT).show();
                        Log.d("handleRollback",  "׳”׳׳©׳×׳׳© ׳׳ ׳ ׳׳—׳§" + " " + e.getMessage().toString());
                    }
                }
            });
        }
        else {
            //TODO
            registerButton.setEnabled(true);
        }
    }

    private void clearPasswords() {
        passwordEditText.setText("");
        confirmPasswordEditText.setText("");
        passwordEditText.requestFocus();
    }
}