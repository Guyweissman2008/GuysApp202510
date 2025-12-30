package com.example.guysapp;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AddRecipeActivity extends BaseActivity {

    // UI
    private EditText editTitle;
    private EditText editDescription;
    private ImageView imageRecipe;
    private Spinner spinnerCategory;
    private Button buttonAdd;
    private Button buttonCamera;
    private Button buttonGallery;
    private ImageButton buttonBackHome;

    // Image state
    private Bitmap selectedBitmap = null;
    private Uri cameraImageUri = null;

    private String selectedCategory = "";

    // ===== Image compression tuning =====
    private static final int IMAGE_MAX_SIZE_PX = 450;      // גודל מקסימלי (רוחב/גובה)
    private static final int JPEG_QUALITY = 30;            // איכות JPEG (0–100)
    private static final int MAX_IMAGE_BYTES = 500 * 1024; // רשת ביטחון ל-Firestore (bytes)

    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>() {
                        @Override
                        public void onActivityResult(ActivityResult result) {
                            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                                Uri imageUri = result.getData().getData();
                                handleImage(imageUri);
                            }
                        }
                    }
            );

    private final ActivityResultLauncher<String> requestCameraPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    new ActivityResultCallback<Boolean>() {
                        @Override
                        public void onActivityResult(Boolean isGranted) {
                            if (isGranted) {
                                openCamera();
                            } else {
                                Toast.makeText(AddRecipeActivity.this,
                                        "הרשאת מצלמה דרושה כדי לצלם תמונה",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
            );

    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.TakePicture(),
                    new ActivityResultCallback<Boolean>() {
                        @Override
                        public void onActivityResult(Boolean result) {
                            if (result) {
                                handleImage(cameraImageUri);
                            }
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_recipe);

        setupBottomNavigation(R.id.nav_add);

        initViews();
        setupCategorySpinner();
        setupClickListeners();
    }

    private void initViews() {
        editTitle = findViewById(R.id.edit_recipe_title);
        editDescription = findViewById(R.id.edit_recipe_description);
        imageRecipe = findViewById(R.id.image_recipe);
        buttonAdd = findViewById(R.id.button_add_recipe);
        buttonCamera = findViewById(R.id.button_camera);
        buttonGallery = findViewById(R.id.button_gallery);
        buttonBackHome = findViewById(R.id.btnBack);
        spinnerCategory = findViewById(R.id.spinner_category);
    }

    private void setupCategorySpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.recipe_categories,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        spinnerCategory.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                selectedCategory = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                selectedCategory = "";
            }
        });
    }

    private void setupClickListeners() {
        buttonGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                pickImageLauncher.launch(intent);
            }
        });

        buttonCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto();
            }
        });

        buttonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addRecipe();
            }
        });

        buttonBackHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void takePhoto() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {

            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            return;
        }

        openCamera();
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
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera");
        return getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    private void handleImage(Uri imageUri) {
        if (imageUri == null) {
            Toast.makeText(this, "Image URI is null", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                selectedBitmap = ImageDecoder.decodeBitmap(
                        ImageDecoder.createSource(getContentResolver(), imageUri)
                );
            } else {
                selectedBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            }
            imageRecipe.setImageBitmap(selectedBitmap);
        } catch (IOException e) {
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
        }
    }

    private void addRecipe() {
        buttonAdd.setEnabled(false);

        String title = editTitle.getText().toString().trim();
        String description = editDescription.getText().toString().trim();

        if (title.isEmpty() || description.isEmpty() || selectedCategory.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            buttonAdd.setEnabled(true);
            return;
        }

        if (selectedBitmap == null) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show();
            buttonAdd.setEnabled(true);
            return;
        }

        if (FBRef.mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show();
            buttonAdd.setEnabled(true);
            return;
        }

        // 3 שלבים בגלל מגבלת גודל במסמך Firestore: הקטנת רזולוציה, דחיסת JPEG ובדיקת גודל לפני שמירה

        // 1 - הקטנת רזולוציה
        selectedBitmap = downscaleBitmap(selectedBitmap);
        imageRecipe.setImageBitmap(selectedBitmap);

        // 2 - דחיסה
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        selectedBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, baos);
        byte[] bytes = baos.toByteArray();

        // 3 - בדיקת גודל
        int sizeKB = bytes.length / 1024;
        Log.d("IMAGE_SIZE", "Image size = " + sizeKB + " KB");

        if (bytes.length > MAX_IMAGE_BYTES) { // רשת ביטחון
            Toast.makeText(this,
                    "התמונה גדולה מדי, נסי תמונה אחרת",
                    Toast.LENGTH_LONG).show();
            buttonAdd.setEnabled(true);
            return;
        }

        List<Integer> imageDataList = new ArrayList<>();
        for (byte b : bytes) {
            imageDataList.add(b & 0xFF);
        }

        String userId = FBRef.mAuth.getCurrentUser().getUid();

        FBRef.refUsers.document(userId).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {

                        String firstName = documentSnapshot.getString("firstName");
                        String lastName = documentSnapshot.getString("lastName");

                        String username =
                                (firstName != null ? firstName : "") + " " +
                                        (lastName != null ? lastName : "");

                        AddRecipeActivity.this.uploadRecipe(
                                title,
                                description,
                                imageDataList,
                                selectedCategory,
                                userId,
                                username.trim()
                        );
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(AddRecipeActivity.this,
                                "Failed to load user details",
                                Toast.LENGTH_SHORT).show();
                        buttonAdd.setEnabled(true);
                    }
                });

    }

    private void uploadRecipe(String title,
                              String description,
                              List<Integer> imageDataList,
                              String category,
                              String userId,
                              String username) {

        String docId = FBRef.refRecipes.document().getId();
        Recipe recipe = new Recipe(docId, title, description, imageDataList, category, username, userId);

        FBRef.refRecipes.document(docId)
                .set(recipe)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(AddRecipeActivity.this, "Recipe added!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(AddRecipeActivity.this, HomeActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(AddRecipeActivity.this,
                                "Failed to add recipe: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                        Log.d("AddRecipeActivity", "Failed to add recipe:"+e.getMessage());
                        buttonAdd.setEnabled(true);
                    }
                });
    }

    // הקטנת רזולוציה
    private Bitmap downscaleBitmap(Bitmap original) {
        int maxSize = IMAGE_MAX_SIZE_PX;

        int width = original.getWidth();
        int height = original.getHeight();

        float ratio = Math.min(
                (float) maxSize / width,
                (float) maxSize / height
        );

        int newWidth = Math.round(width * ratio);
        int newHeight = Math.round(height * ratio);

        return Bitmap.createScaledBitmap(original, newWidth, newHeight, true);
    }
}
