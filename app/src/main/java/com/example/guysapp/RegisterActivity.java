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
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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

        // מעבר למסך התחברות
        goToLoginText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                finish();
            }
        });
    }

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
                                            "הרשאת מצלמה דרושה כדי לצלם תמונה",
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
        cameraImageUri = createImageUri();
        if (cameraImageUri != null) {
            cameraLauncher.launch(cameraImageUri);
        }
    }

    private Uri createImageUri() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "TempPicture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera");

        return getContentResolver()
                .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    private void loadBitmapFromUri(Uri uri) {
        try {
            selectedBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            imageViewProfile.setImageBitmap(selectedBitmap);
        } catch (IOException e) {
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
        }
    }

    private void registerUser() {
        String firstName = firstNameEditText.getText().toString().trim();;
        String lastName = lastNameEditText.getText().toString().trim();;
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();
        String confirmPassword = confirmPasswordEditText.getText().toString();

        if (firstName.isEmpty() || lastName.isEmpty()
                || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {

            Toast.makeText(this, "All fields must be filled", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "הסיסמאות אינן תואמות", Toast.LENGTH_SHORT).show();
            clearPasswords();
            return;
        }

        if (selectedBitmap == null) {
            Toast.makeText(this, "Please choose a profile image", Toast.LENGTH_SHORT).show();
            return;
        }

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
                        }
                    }
                });
    }

    private void saveUserWithImage(String userId,
                                   String firstName,
                                   String lastName,
                                   String email
                                  ) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        selectedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);

        List<Integer> byteList = new ArrayList<>();
        for (byte b : baos.toByteArray()) {
            byteList.add((int) b);
        }

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("userId", userId);
        userMap.put("firstName", firstName);
        userMap.put("lastName", lastName);
        userMap.put("email", email);
        userMap.put("imageData", byteList);

        FBRef.refUsers.document(userId)
                .set(userMap)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(RegisterActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(RegisterActivity.this, HomeActivity.class));
                            finish();
                        } else {
                            // כשל ב-Firestore → rollback: מחיקת המשתמש מ-FirebaseAuth
                            String errorMsg = "Error adding user data";
                            if (task.getException() != null) {
                                errorMsg = task.getException().getMessage();
                            }
                            Toast.makeText(RegisterActivity.this, errorMsg, Toast.LENGTH_LONG).show();

                            FirebaseUser currentUser = FBRef.mAuth.getCurrentUser();
                            if (currentUser != null) {
                                Task<Void> voidTask = currentUser.delete()
                                        .addOnCompleteListener(deleteTask -> {
                                            if (!deleteTask.isSuccessful() && deleteTask.getException() != null) {
                                                Toast.makeText(RegisterActivity.this,
                                                        "Failed to delete user after error: " + deleteTask.getException().getMessage(),
                                                        Toast.LENGTH_LONG).show();
                                            }
                                        });
                            }
                        }
                    }
                });
    }


        private void clearPasswords() {
        passwordEditText.setText("");
        confirmPasswordEditText.setText("");
        passwordEditText.requestFocus();
    }
}
