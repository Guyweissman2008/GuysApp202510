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
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AddRecipeActivity extends BaseActivity {

    // UI
    private EditText editTitle, editDescription;
    private ImageView imageRecipe;
    private Spinner spinnerCategory;
    private Button buttonAdd, buttonCamera, buttonGallery;
    private ImageButton buttonBackHome;
    private EditText editPrepTime;
    // Image state
    private Bitmap selectedBitmap = null;
    private Uri cameraImageUri = null;
    private String selectedCategory = "";

    // ===== Image compression settings =====
    private static final int IMAGE_MAX_SIZE_PX = 450;
    private static final int JPEG_QUALITY = 30;
    private static final int MAX_IMAGE_BYTES = 500 * 1024;

    // ===== ActivityResultLaunchers =====
    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>() {
                        @Override
                        public void onActivityResult(ActivityResult result) {
                            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                                AddRecipeActivity.this.handleImage(result.getData().getData());
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
                            if (isGranted) AddRecipeActivity.this.openCamera();
                            else
                                Toast.makeText(AddRecipeActivity.this, "הרשאת מצלמה דרושה כדי לצלם תמונה", Toast.LENGTH_SHORT).show();
                        }
                    }
            );

    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.TakePicture(),
                    new ActivityResultCallback<Boolean>() {
                        @Override
                        public void onActivityResult(Boolean result) {
                            if (result) AddRecipeActivity.this.handleImage(cameraImageUri);
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
        String recipeId = getIntent().getStringExtra("recipeId");
        if (recipeId != null && !recipeId.isEmpty()) {
            loadRecipeForEditing(recipeId);
        } else {
            buttonAdd.setText("הוסף מתכון");
        }
    }

    // ===== Initialize UI =====
    private void initViews() {
        editTitle = findViewById(R.id.edit_recipe_title);
        editDescription = findViewById(R.id.edit_recipe_description);
        imageRecipe = findViewById(R.id.image_recipe);
        spinnerCategory = findViewById(R.id.spinner_category);
        buttonAdd = findViewById(R.id.button_add_recipe);
        buttonCamera = findViewById(R.id.button_camera);
        buttonGallery = findViewById(R.id.button_gallery);
        buttonBackHome = findViewById(R.id.btnBack);
        editPrepTime = findViewById(R.id.edit_prep_time);
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
                pickImageLauncher.launch(
                        new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                );
            }

        });

        buttonCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AddRecipeActivity.this.takePhoto();
            }
        });

        buttonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AddRecipeActivity.this.saveOrUpdateRecipe();
            }
        });

        buttonBackHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AddRecipeActivity.this, HomeActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    // ===== Image handling =====
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
        else Toast.makeText(this, "שגיאה ביצירת קובץ תמונה", Toast.LENGTH_SHORT).show();
    }

    private Uri createImageUri() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera");
        return getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    private void handleImage(Uri imageUri) {
        if (imageUri == null) return;

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

    // ===== Process image: downscale, compress, size check =====
    @Nullable
    private List<Integer> processSelectedImage(Bitmap bitmap) {
        if (bitmap == null) return null;

        // הקטנת רזולוציה כדי שהמתכון לא יהיה כבד מדי
        Bitmap downscaled = downscaleBitmap(bitmap);
        imageRecipe.setImageBitmap(downscaled);

        // דחיסת התמונה ל-JPEG באיכות מוגדרת
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        downscaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, baos);
        byte[] bytes = baos.toByteArray();

        // בדיקת גודל התמונה מול הגבול של Firestore
        if (bytes.length > MAX_IMAGE_BYTES) {
            Toast.makeText(this, "התמונה גדולה מדי, נסי תמונה אחרת", Toast.LENGTH_LONG).show();
            return null;
        }

        // המרת הבייטים לרשימה של Integers לשמירה במסד
        List<Integer> imageDataList = new ArrayList<>();
        for (byte b : bytes) imageDataList.add(b & 0xFF);

        return imageDataList;
    }

    private Bitmap downscaleBitmap(Bitmap original) {
        int width = original.getWidth();
        int height = original.getHeight();
        float ratio = Math.min((float) IMAGE_MAX_SIZE_PX / width, (float) IMAGE_MAX_SIZE_PX / height);

        int newWidth = Math.round(width * ratio);
        int newHeight = Math.round(height * ratio);
        return Bitmap.createScaledBitmap(original, newWidth, newHeight, true);
    }

    // ===== Save or update recipe =====
    private void saveOrUpdateRecipe() {
        buttonAdd.setEnabled(false);

        String title = editTitle.getText().toString().trim();
        String description = editDescription.getText().toString().trim();

        if (title.isEmpty() || description.isEmpty() || selectedCategory.isEmpty()) {
            Toast.makeText(this, "יש למלא את כל השדות", Toast.LENGTH_SHORT).show();
            buttonAdd.setEnabled(true);
            return;
        }


        List<Integer> imageDataList = processSelectedImage(selectedBitmap);
        if (imageDataList == null) {
            buttonAdd.setEnabled(true);
            return;
        }

        if (FBRef.mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show();
            buttonAdd.setEnabled(true);
            return;
        }
        String prepTime = editPrepTime.getText().toString().trim();

        if (title.isEmpty() || description.isEmpty() || selectedCategory.isEmpty() || prepTime.isEmpty()) {
            Toast.makeText(this, "יש למלא את כל השדות (כולל זמן)", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = FBRef.mAuth.getCurrentUser().getUid();

        FBRef.refUsers.document(userId).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        String firstName = documentSnapshot.getString("firstName");
                        String lastName = documentSnapshot.getString("lastName");
                        String username = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();

                        String recipeId = AddRecipeActivity.this.getIntent().getStringExtra("recipeId");
                        if (recipeId == null || recipeId.isEmpty()) {
                            AddRecipeActivity.this.addNewRecipe(title, description, imageDataList, selectedCategory, userId, username,prepTime);
                        } else {
                            AddRecipeActivity.this.updateRecipe(recipeId, title, description, imageDataList, selectedCategory, userId, username,prepTime);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(AddRecipeActivity.this, "שגיאה בטעינת פרטי משתמש", Toast.LENGTH_SHORT).show();
                        buttonAdd.setEnabled(true);
                    }
                });
    }

    private void addNewRecipe(String title, String description, List<Integer> imageDataList,
                              String category, String userId, String username,String prepTime) {

        String docId = FBRef.refRecipes.document().getId();
        Recipe recipe = new Recipe(docId, title, description, imageDataList, category, username, userId,prepTime);

        FBRef.refRecipes.document(docId)
                .set(recipe)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(AddRecipeActivity.this, "המתכון נוסף!", Toast.LENGTH_SHORT).show();
                        AddRecipeActivity.this.startActivity(new Intent(AddRecipeActivity.this, HomeActivity.class));
                        AddRecipeActivity.this.finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(AddRecipeActivity.this, "שגיאה בהוספה: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        buttonAdd.setEnabled(true);
                    }
                });
    }

    private void updateRecipe(String recipeId, String title, String description,
                              List<Integer> imageDataList, String category, String userId, String username,String prepTime) {

        FBRef.refRecipes.document(recipeId)
                .update(
                        "title", title,
                        "description", description,
                        "category", category,
                        "imageData", imageDataList,
                        "preparationTime", prepTime
                )
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // עדכון ב-DB הצליח
                        Toast.makeText(AddRecipeActivity.this, "המתכון עודכן!", Toast.LENGTH_SHORT).show();

                        // יצירת המתכון המעודכן
                        Recipe updatedRecipe = new Recipe(recipeId, title, description, imageDataList, category, username, userId,prepTime);

                        // עדכון ה-RecyclerView
                        RecipeAdapter adapter = getRecipeAdapter(); // קבל את ה-Adapter
                        if (adapter != null) {
                            adapter.updateRecipeInList(updatedRecipe); // עדכן את המתכון
                        }

                        finish(); // חזור למסך הקודם
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(AddRecipeActivity.this, "שגיאה בעדכון: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        buttonAdd.setEnabled(true);
                    }
                });
    }

    private RecipeAdapter getRecipeAdapter() {
        RecyclerView recyclerView = findViewById(R.id.recyclerView_user_recipes);
        RecyclerView.Adapter<?> adapter = recyclerView.getAdapter();
        if (adapter instanceof RecipeAdapter) {
            return (RecipeAdapter) adapter;
        }
        return null;
    }


    // ===== Load recipe for editing =====
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
                        selectedCategory = recipe.getCategory();

                        ArrayAdapter adapter = (ArrayAdapter) spinnerCategory.getAdapter();
                        int pos = adapter.getPosition(selectedCategory);
                        spinnerCategory.setSelection(pos);

                        if (recipe.getImageData() != null && !recipe.getImageData().isEmpty()) {
                            byte[] bytes = recipe.imageDataToBytes();
                            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                            imageRecipe.setImageBitmap(bitmap);
                            selectedBitmap = bitmap;
                        }
                        if (recipe.getPreparationTime() != null) {
                            editPrepTime.setText(recipe.getPreparationTime());
                        }
                        buttonAdd.setText("עדכן מתכון");
                    }
                });
    }
}
