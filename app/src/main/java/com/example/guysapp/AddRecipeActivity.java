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

    // UI
    private EditText editTitle, editDescription;
    private ImageView imageRecipe;
    private Spinner spinnerCategory;
    private Button btnSave, btnCamera, btnGallery;
    private ImageButton btnBack;
    private EditText editPrepTime;
    // Image state
    private Bitmap selectedBitmap = null;
    private Uri cameraImageUri = null;
    private String selectedCategory = "";

    // Image compression settings
    private static final int IMAGE_MAX_SIZE_PX = 450;
    private static final int JPEG_QUALITY = 30;
    private static final int MAX_IMAGE_BYTES = 500 * 1024;

    // ActivityResultLaunchers
    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>() {
                        @Override
                        public void onActivityResult(ActivityResult result) {
                            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                                handleImage(result.getData().getData());
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
                            if (isGranted) openCamera();
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
                            if (result)
                                handleImage(cameraImageUri);
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_recipe);

        String recipeId = getIntent().getStringExtra("recipeId");

        setupBottomNavigation(R.id.nav_add);
        initViews();
        setupCategorySpinner();
        setupClickListeners();

        if (recipeId != null && !recipeId.isEmpty()) {
            loadRecipeForEditing(recipeId);
        } else {
            btnSave.setText("הוסף מתכון");
        }
    }

    private void initViews() {
        editTitle = findViewById(R.id.edit_recipe_title);
        editDescription = findViewById(R.id.edit_recipe_description);
        imageRecipe = findViewById(R.id.image_recipe);
        spinnerCategory = findViewById(R.id.spinner_category);
        btnSave = findViewById(R.id.button_add_recipe);
        btnCamera = findViewById(R.id.button_camera);
        btnGallery = findViewById(R.id.button_gallery);
        btnBack = findViewById(R.id.btnBack);
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
        btnGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickImageLauncher.launch(
                        new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                );
            }

        });

        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveOrUpdateRecipe();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AddRecipeActivity.this, HomeActivity.class);
                startActivity(intent);
                finish();
            }
        });
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
        if (cameraImageUri != null)
            cameraLauncher.launch(cameraImageUri);
        else
            Toast.makeText(this, "שגיאה ביצירת קובץ תמונה", Toast.LENGTH_SHORT).show();
    }

    private Uri createImageUri() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera");
        return getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    private void handleImage(Uri imageUri) {
        if (imageUri == null) {
            Toast.makeText(this, "לא נבחרה תמונה", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "לא ניתן לטעון את התמונה", Toast.LENGTH_SHORT).show();
        }
    }

    // עיבוד תמונה: הקטנה, דחיסה ובדיקת גודל
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
            Toast.makeText(this, "התמונה גדולה מדי, נסה תמונה אחרת", Toast.LENGTH_LONG).show();
            return null;
        }

        // המרת הבייטים לרשימה של Integers לשמירה במסד
        List<Integer> imageDataList = new ArrayList<>();
        for (byte b : bytes)
            imageDataList.add(b & 0xFF);

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

    private void saveOrUpdateRecipe() {
        btnSave.setEnabled(false); // כדי שלא ילחצו פעמיים ויוסיפו מתכון כפול

        String title = editTitle.getText().toString().trim();
        String description = editDescription.getText().toString().trim();
        String prepTime = editPrepTime.getText().toString().trim();

        if (title.isEmpty() || description.isEmpty() || selectedCategory.isEmpty() || prepTime.isEmpty()) {
            Toast.makeText(this, "יש למלא את כל השדות (כולל זמן)", Toast.LENGTH_SHORT).show();
            btnSave.setEnabled(true);
            return;
        }

        if (selectedBitmap == null) {
            Toast.makeText(this, "בחר תמונה למתכון", Toast.LENGTH_SHORT).show();
            btnSave.setEnabled(true);
            return;
        }

        List<Integer> imageDataList = processSelectedImage(selectedBitmap);
        if (imageDataList == null) {
            btnSave.setEnabled(true);
            return;
        }

        if (FBRef.mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "אין משתמש מחובר", Toast.LENGTH_SHORT).show();
            btnSave.setEnabled(true);
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

                        String recipeId = getIntent().getStringExtra("recipeId");
                        if (recipeId == null || recipeId.isEmpty()) {
                            addNewRecipe(title, description, imageDataList, selectedCategory, userId, username, prepTime);
                        } else {
                            updateRecipe(recipeId, title, description, imageDataList, selectedCategory, userId, username, prepTime);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(AddRecipeActivity.this, "שגיאה בטעינת פרטי משתמש", Toast.LENGTH_SHORT).show();
                        btnSave.setEnabled(true);
                    }
                });
    }

    private void addNewRecipe(String title, String description, List<Integer> imageDataList,
                              String category, String userId, String username, String prepTime) {

        String docId = FBRef.refRecipes.document().getId();
        Recipe recipe = new Recipe(docId, title, description, imageDataList, category, username, userId, prepTime);

        FBRef.refRecipes.document(docId)
                .set(recipe)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(AddRecipeActivity.this, "המתכון נוסף!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(AddRecipeActivity.this, HomeActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(AddRecipeActivity.this, "שגיאה בהוספה: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        btnSave.setEnabled(true);
                    }
                });
    }

    private void updateRecipe(String recipeId, String title, String description,
                              List<Integer> imageDataList, String category, String userId, String username, String prepTime) {

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
                        finish(); // חזור למסך הקודם
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(AddRecipeActivity.this, "שגיאה בעדכון: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        btnSave.setEnabled(true);
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
                        btnSave.setText("עדכן מתכון");
                    }
                });
    }
}
