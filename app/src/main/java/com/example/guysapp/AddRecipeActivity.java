package com.example.guysapp;
import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
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
import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;
public class AddRecipeActivity extends BaseActivity {
    private EditText editTitle, editDescription, editPrepTime;
    private ImageView imageRecipe;
    private Spinner spinnerCategory;
    private Button btnSave, btnAddImage;
    private ImageButton btnBack;
    private Bitmap selectedBitmap = null;
    private Uri cameraImageUri = null;
    private String selectedCategory = "";
    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        AddRecipeActivity.this.handleImage(result.getData().getData());
                    }
                }
            });
    private final ActivityResultLauncher<String> requestCameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean isGranted) {
                    if (isGranted) AddRecipeActivity.this.openCamera();
                    else
                        Toast.makeText(AddRecipeActivity.this, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show();
                }
            });
    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean result) {
                    if (result) AddRecipeActivity.this.handleImage(cameraImageUri);
                }
            });
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_recipe);
        initViews();
        setupCategorySpinner();
        setupClickListeners();
        setupBottomNavigation(R.id.nav_add);
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
        btnBack = findViewById(R.id.btnBackk);
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
        btnAddImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AddRecipeActivity.this.showImageSourceDialog();
            }
        });
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AddRecipeActivity.this.saveOrUpdateRecipe();
            }
        });
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(AddRecipeActivity.this, "back", Toast.LENGTH_SHORT).show();
                AddRecipeActivity.this.finish();
            }
        });
    }
    private void showImageSourceDialog() {
        String[] options = {"Take Photo", "Choose from Gallery", "Cancel"};
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Select Image Source");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) AddRecipeActivity.this.takePhoto();
                else if (which == 1) AddRecipeActivity.this.pickImageFromGallery();
                else dialog.dismiss();
            }
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
        cameraImageUri = ImageHelper.createImageUri(this, "Recipe Photo", "Taken for recipe: " + editTitle.getText().toString());

        if (cameraImageUri != null) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);

            // --- ׳”׳×׳™׳§׳•׳: ׳”׳•׳¡׳₪׳× ׳”׳¨׳©׳׳•׳× ׳’׳™׳©׳” ׳-URI ---
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            cameraLauncher.launch(cameraImageUri); // ׳”׳©׳•׳¨׳” ׳”׳׳§׳•׳¨׳™׳× ׳©׳׳
        } else {
            Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
        }
    }
    // ׳›׳“׳™ ׳©׳™׳”׳™׳” ׳׳₪׳©׳¨ ׳׳¨׳׳•׳× ׳¢׳ ׳”׳׳¡׳
    private void handleImage(Uri imageUri) {
        if (imageUri == null) return;

        selectedBitmap = ImageHelper.loadBitmapFromUri(this, imageUri);

        if (selectedBitmap != null) {
            selectedBitmap = ImageHelper.scaleForRecipe(selectedBitmap);
            imageRecipe.setImageBitmap(selectedBitmap);
        } else {
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
        Blob imageDataBlob = ImageHelper.bitmapToBlob(this, selectedBitmap, ImageHelper.NORMAL_IMAGE);
        if (imageDataBlob == null) {
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
            btnSave.setEnabled(true);
            return;
        }

        String userId = FBRef.mAuth.getCurrentUser().getUid();
        FBRef.refUsers.document(userId).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                String firstName = documentSnapshot.getString("firstName");
                String lastName = documentSnapshot.getString("lastName");
                String username = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();

                String recipeId = AddRecipeActivity.this.getIntent().getStringExtra("recipeId");
                if (recipeId == null || recipeId.isEmpty()) {
                    AddRecipeActivity.this.addNewRecipe(title, description, imageDataBlob, selectedCategory, userId, username, prepTime);
                } else {
                    AddRecipeActivity.this.updateRecipe(recipeId, title, description, imageDataBlob, selectedCategory, prepTime);
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                btnSave.setEnabled(true);
                Toast.makeText(AddRecipeActivity.this, "Error fetching user details", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addNewRecipe(String title, String description, Blob imageData, String category, String userId, String username, String prepTime) {
        String docId = FBRef.refRecipes.document().getId();
        Recipe recipe = new Recipe(docId, title, description, imageData, category, username, userId, prepTime);

        FBRef.refRecipes.document(docId).set(recipe)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("AddRecipeActivity", "Recipe saved, navigating back...");

                        setResult(RESULT_OK);
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        btnSave.setEnabled(true);
                        Toast.makeText(AddRecipeActivity.this, "Error saving recipe", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void updateRecipe(String recipeId, String title, String description, Blob imageData, String category, String prepTime) {
        FBRef.refRecipes.document(recipeId).update(
                        "title", title,
                        "description", description,
                        "category", category,
                        "imageData", imageData,
                        "preparationTime", prepTime)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("AddRecipeActivity", "Recipe saved, navigating back...");
                        setResult(RESULT_OK);
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        btnSave.setEnabled(true);
                        Toast.makeText(AddRecipeActivity.this, "Update failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void loadRecipeForEditing(String recipeId) {
        FBRef.refRecipes.document(recipeId).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot document) {

                        if (!document.exists()) return;

                        Recipe recipe = document.toObject(Recipe.class);
                        if (recipe == null) return;

                        editTitle.setText(recipe.getTitle());
                        editDescription.setText(recipe.getDescription());
                        editPrepTime.setText(recipe.getPreparationTime());

                        selectedCategory = recipe.getCategory();

                        ArrayAdapter<CharSequence> adapter =
                                (ArrayAdapter<CharSequence>) spinnerCategory.getAdapter();

                        if (adapter != null && selectedCategory != null) {
                            spinnerCategory.setSelection(adapter.getPosition(selectedCategory));
                        }

                        if (recipe.getImageData() != null) {
                            selectedBitmap = ImageHelper.decodeBlobToBitmap(recipe.getImageData());
                            if (selectedBitmap != null) {
                                imageRecipe.setImageBitmap(selectedBitmap);
                            }
                        }

                        btnSave.setText("Update Recipe");
                    }
                });
    }

    @Override
    protected void onDestroy() {
        if (imageRecipe != null) {
            imageRecipe.setImageDrawable(null);
        }
        selectedBitmap = null;
        cameraImageUri = null;
        super.onDestroy();
    }
}