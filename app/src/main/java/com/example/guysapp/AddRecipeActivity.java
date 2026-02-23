package com.example.guysapp;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
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
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AddRecipeActivity extends BaseActivity {

    // UI Elements
    private EditText editTitle, editDescription, editPrepTime;
    private ImageView imageRecipe;
    private Spinner spinnerCategory;
    private Button btnSave, btnAddImage;
    private ImageButton btnBack;

    // Image state
    private Bitmap selectedBitmap = null;
    private Uri cameraImageUri = null;
    private String selectedCategory = "";

    // Image compression settings
    private static final int IMAGE_MAX_SIZE_PX = 450;
    private static final int JPEG_QUALITY = 30;
    private static final int MAX_IMAGE_BYTES = 500 * 1024;

    // --- Launchers for Camera and Gallery ---

    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    handleImage(result.getData().getData());
                }
            });

    private final ActivityResultLauncher<String> requestCameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) openCamera();
                else Toast.makeText(AddRecipeActivity.this, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show();
            });

    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), result -> {
                if (result) handleImage(cameraImageUri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_recipe);

        setupBottomNavigation(R.id.nav_add);
        initViews();
        setupCategorySpinner();
        setupClickListeners();

        // Check if we are editing an existing recipe
        String recipeId = getIntent().getStringExtra("recipeId");
        if (recipeId != null && !recipeId.isEmpty()) {
            loadRecipeForEditing(recipeId);
        } else {
            btnSave.setText("Add Recipe");
        }
    }

    private void initViews() {
        editTitle = findViewById(R.id.edit_recipe_title);
        editDescription = findViewById(R.id.edit_recipe_description);
        imageRecipe = findViewById(R.id.image_recipe);
        spinnerCategory = findViewById(R.id.spinner_category);
        btnSave = findViewById(R.id.button_add_recipe);
        btnAddImage = findViewById(R.id.button_add_image);
        btnBack = findViewById(R.id.btnBack);
        editPrepTime = findViewById(R.id.edit_prep_time);
    }

    private void setupCategorySpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.recipe_categories, android.R.layout.simple_spinner_item);
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
        // Open the dialog to choose between Camera or Gallery
        btnAddImage.setOnClickListener(v -> showImageSourceDialog());

        btnSave.setOnClickListener(v -> saveOrUpdateRecipe());

        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(AddRecipeActivity.this, HomeActivity.class));
            finish();
        });
    }

    private void showImageSourceDialog() {
        String[] options = {"Take Photo", "Choose from Gallery", "Cancel"};
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Select Image Source");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) takePhoto();
            else if (which == 1) pickImageFromGallery();
            else dialog.dismiss();
        });
        builder.show();
    }

    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    private void takePhoto() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        } else {
            openCamera();
        }
    }

    private void openCamera() {
        cameraImageUri = createImageUri();
        if (cameraImageUri != null) cameraLauncher.launch(cameraImageUri);
        else Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
    }

    private Uri createImageUri() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Recipe Picture");
        return getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    private void handleImage(Uri imageUri) {
        if (imageUri == null) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                selectedBitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(), imageUri));
            } else {
                selectedBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            }
            imageRecipe.setImageBitmap(selectedBitmap);
        } catch (IOException e) {
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveOrUpdateRecipe() {
        btnSave.setEnabled(false);

        String title = editTitle.getText().toString().trim();
        String description = editDescription.getText().toString().trim();
        String prepTime = editPrepTime.getText().toString().trim();

        if (title.isEmpty() || description.isEmpty() || selectedCategory.isEmpty() || prepTime.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            btnSave.setEnabled(true);
            return;
        }

        if (selectedBitmap == null) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show();
            btnSave.setEnabled(true);
            return;
        }

        List<Integer> imageDataList = processSelectedImage(selectedBitmap);
        if (imageDataList == null) {
            btnSave.setEnabled(true);
            return;
        }

        if (FBRef.mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show();
            btnSave.setEnabled(true);
            return;
        }

        String userId = FBRef.mAuth.getCurrentUser().getUid();
        FBRef.refUsers.document(userId).get().addOnSuccessListener(documentSnapshot -> {
            String firstName = documentSnapshot.getString("firstName");
            String lastName = documentSnapshot.getString("lastName");
            String username = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();

            String recipeId = getIntent().getStringExtra("recipeId");
            if (recipeId == null || recipeId.isEmpty()) {
                addNewRecipe(title, description, imageDataList, selectedCategory, userId, username, prepTime);
            } else {
                updateRecipe(recipeId, title, description, imageDataList, selectedCategory, userId, username, prepTime);
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(AddRecipeActivity.this, "Error fetching user details", Toast.LENGTH_SHORT).show();
            btnSave.setEnabled(true);
        });
    }

    @Nullable
    private List<Integer> processSelectedImage(Bitmap bitmap) {
        Bitmap downscaled = downscaleBitmap(bitmap);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        downscaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, baos);
        byte[] bytes = baos.toByteArray();

        if (bytes.length > MAX_IMAGE_BYTES) {
            Toast.makeText(this, "Image is too large, please try another", Toast.LENGTH_LONG).show();
            return null;
        }

        List<Integer> imageDataList = new ArrayList<>();
        for (byte b : bytes) imageDataList.add(b & 0xFF);
        return imageDataList;
    }

    private Bitmap downscaleBitmap(Bitmap original) {
        int width = original.getWidth();
        int height = original.getHeight();
        float ratio = Math.min((float) IMAGE_MAX_SIZE_PX / width, (float) IMAGE_MAX_SIZE_PX / height);
        return Bitmap.createScaledBitmap(original, Math.round(width * ratio), Math.round(height * ratio), true);
    }

    private void addNewRecipe(String title, String description, List<Integer> imageDataList,
                              String category, String userId, String username, String prepTime) {
        String docId = FBRef.refRecipes.document().getId();
        Recipe recipe = new Recipe(docId, title, description, imageDataList, category, username, userId, prepTime);

        FBRef.refRecipes.document(docId).set(recipe).addOnSuccessListener(aVoid -> {
            Toast.makeText(AddRecipeActivity.this, "Recipe Added!", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> {
            Toast.makeText(AddRecipeActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            btnSave.setEnabled(true);
        });
    }

    private void updateRecipe(String recipeId, String title, String description,
                              List<Integer> imageDataList, String category, String userId, String username, String prepTime) {
        FBRef.refRecipes.document(recipeId).update(
                "title", title,
                "description", description,
                "category", category,
                "imageData", imageDataList,
                "preparationTime", prepTime
        ).addOnSuccessListener(aVoid -> {
            Toast.makeText(AddRecipeActivity.this, "Recipe Updated!", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> {
            Toast.makeText(AddRecipeActivity.this, "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            btnSave.setEnabled(true);
        });
    }

    private void loadRecipeForEditing(String recipeId) {
        FBRef.refRecipes.document(recipeId).get().addOnSuccessListener(document -> {
            if (!document.exists()) return;
            Recipe recipe = document.toObject(Recipe.class);
            if (recipe == null) return;

            editTitle.setText(recipe.getTitle());
            editDescription.setText(recipe.getDescription());
            editPrepTime.setText(recipe.getPreparationTime());
            selectedCategory = recipe.getCategory();

            ArrayAdapter adapter = (ArrayAdapter) spinnerCategory.getAdapter();
            spinnerCategory.setSelection(adapter.getPosition(selectedCategory));

            if (recipe.getImageData() != null) {
                byte[] bytes = recipe.imageDataToBytes();
                selectedBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                imageRecipe.setImageBitmap(selectedBitmap);
            }
            btnSave.setText("Update Recipe");
        });
    }
}