package com.example.guysapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private ImageView imageViewProfile;
    private Bitmap selectedBitmap;

    private EditText firstNameEditText, lastNameEditText, emailEditText, passwordEditText;
    private CheckBox notificationsCheckBox;
    private Button chooseImageButton, registerButton;

    // FirebaseAuth instance for user authentication
    private FirebaseAuth mAuth;

    // ActivityResultLauncher for choosing an image
    private final ActivityResultLauncher<Intent> imageResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            selectedBitmap = MediaStore.Images.Media.getBitmap(RegisterActivity.this.getContentResolver(), imageUri);
                            imageViewProfile.setImageBitmap(selectedBitmap);
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(RegisterActivity.this, "Failed to load image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize FirebaseAuth
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        initViews();
    }

    private void initViews() {
        // UI elements initialization
        imageViewProfile = findViewById(R.id.imageview_profile);
        firstNameEditText = findViewById(R.id.edittext_first_name);
        lastNameEditText = findViewById(R.id.edittext_last_name);
        emailEditText = findViewById(R.id.edittext_email);
        passwordEditText = findViewById(R.id.edittext_password);
        notificationsCheckBox = findViewById(R.id.checkbox_notifications);
        chooseImageButton = findViewById(R.id.button_choose_image);
        registerButton = findViewById(R.id.button_register);

        // Set onClick listeners for the buttons
        chooseImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseImage();
            }
        });

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });
    }

    private void chooseImage() {
        // Open the gallery to choose an image
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imageResultLauncher.launch(intent);
    }

    private void registerUser() {
        // Get input values from EditText fields
        String firstName = firstNameEditText.getText().toString();
        String lastName = lastNameEditText.getText().toString();
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        boolean allowNotifications = notificationsCheckBox.isChecked();

        // Validate input
        if (email.isEmpty() || password.isEmpty() || firstName.isEmpty() || lastName.isEmpty()) {
            Toast.makeText(this, "All fields must be filled", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if an image has been chosen
        if (selectedBitmap == null) {
            Toast.makeText(this, "Please choose a profile image", Toast.LENGTH_SHORT).show();
            return;
        }

        // Register the user with Firebase Authentication
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // If registration is successful, get the FirebaseUser
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // After successful authentication, upload the image and user data to Firestore
                            String userId = user.getUid();
                            uploadImageAndAddUserData(userId, firstName, lastName, email, allowNotifications);
                        }
                    } else {
                        // If registration fails, show an error message
                        Toast.makeText(RegisterActivity.this, "Registration failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void uploadImageAndAddUserData(String userId, String firstName, String lastName, String email, boolean allowNotifications) {
        // Convert image to byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        selectedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] bytes = baos.toByteArray();

        // Convert byte[] to List<Integer> for Firestore compatibility
        List<Integer> byteList = new ArrayList<>();
        for (byte b : bytes) {
            byteList.add((int) b);  // Cast byte to Integer
        }

        // Create a map for user data
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("firstName", firstName);
        userMap.put("lastName", lastName);
        userMap.put("email", email);
        userMap.put("allowNotifications", allowNotifications);
        userMap.put("userId", userId);  // Store Firebase User ID
        userMap.put("imageData", byteList);  // Store image as List<Integer>

        // Add user data to Firestore (assuming FBRef.refUsers is your Firestore reference)
        FBRef.refUsers.document(userId)
                .set(userMap)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(RegisterActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                    // After successful registration, navigate to the login screen
                    startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RegisterActivity.this, "Error adding user data", Toast.LENGTH_SHORT).show();
                });
    }


}
