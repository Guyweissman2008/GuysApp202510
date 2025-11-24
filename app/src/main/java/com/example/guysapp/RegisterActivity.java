package com.example.guysapp;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private ImageView imageViewProfile;
    private Bitmap selectedBitmap;

    private EditText firstNameEditText, lastNameEditText, emailEditText, passwordEditText;
    private CheckBox notificationsCheckBox;
    private Button chooseImageButton, takePhotoButton, registerButton;

    private FirebaseAuth mAuth;
    private Uri cameraImageUri;

    // Launcher לבחירת תמונה מהגלריה
    private final ActivityResultLauncher<Intent> imageResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    try {
                        selectedBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                        imageViewProfile.setImageBitmap(selectedBitmap);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(RegisterActivity.this, "Failed to load image", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private ActivityResultLauncher<String> requestCameraPermissionLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        initViews();
        initCameraLaunchers();
    }

    private void initViews() {
        imageViewProfile = findViewById(R.id.imageview_profile);
        firstNameEditText = findViewById(R.id.edittext_first_name);
        lastNameEditText = findViewById(R.id.edittext_last_name);
        emailEditText = findViewById(R.id.edittext_email);
        passwordEditText = findViewById(R.id.edittext_password);
        notificationsCheckBox = findViewById(R.id.checkbox_notifications);
        chooseImageButton = findViewById(R.id.button_choose_image);
        takePhotoButton = findViewById(R.id.button_take_photo);
        registerButton = findViewById(R.id.button_register);

        chooseImageButton.setOnClickListener(v -> chooseImage());
        takePhotoButton.setOnClickListener(v -> takePhoto());
        registerButton.setOnClickListener(v -> registerUser());
    }

    private void initCameraLaunchers() {
        // בקשת הרשאת מצלמה
        requestCameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) openCamera();
                    else Toast.makeText(this, "הרשאת מצלמה דרושה כדי לצלם תמונה", Toast.LENGTH_SHORT).show();
                }
        );

        // צילום תמונה
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                result -> {
                    if (result) {
                        try {
                            selectedBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), cameraImageUri);
                            imageViewProfile.setImageBitmap(selectedBitmap);
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Failed to load captured image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    private void chooseImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imageResultLauncher.launch(intent);
    }

    private void takePhoto() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        } else {
            openCamera();
        }
    }

    private void openCamera() {
        cameraImageUri = createImageUri();
        if (cameraImageUri != null) {
            cameraLauncher.launch(cameraImageUri);
        } else {
            Toast.makeText(this, "שגיאה ביצירת קובץ תמונה", Toast.LENGTH_SHORT).show();
        }
    }

    private Uri createImageUri() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "TempPicture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera");
        return getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    private void registerUser() {
        String firstName = firstNameEditText.getText().toString();
        String lastName = lastNameEditText.getText().toString();
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        boolean allowNotifications = notificationsCheckBox.isChecked();

        if (email.isEmpty() || password.isEmpty() || firstName.isEmpty() || lastName.isEmpty()) {
            Toast.makeText(this, "All fields must be filled", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedBitmap == null) {
            Toast.makeText(this, "Please choose a profile image", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String userId = user.getUid();
                            uploadImageAndAddUserData(userId, firstName, lastName, email, allowNotifications);
                        }
                    } else {
                        Toast.makeText(RegisterActivity.this, "Registration failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void uploadImageAndAddUserData(String userId, String firstName, String lastName, String email, boolean allowNotifications) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        selectedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] bytes = baos.toByteArray();

        List<Integer> byteList = new ArrayList<>();
        for (byte b : bytes) byteList.add((int) b);

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("firstName", firstName);
        userMap.put("lastName", lastName);
        userMap.put("email", email);
        userMap.put("allowNotifications", allowNotifications);
        userMap.put("userId", userId);
        userMap.put("imageData", byteList);

        FBRef.refUsers.document(userId)
                .set(userMap)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(RegisterActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(RegisterActivity.this, "Error adding user data", Toast.LENGTH_SHORT).show());
    }
}
