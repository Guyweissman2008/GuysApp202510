package com.example.guysapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddRecipeActivity extends AppCompatActivity {

    private EditText editTitle, editDescription;
    private ImageView imageRecipe;
    private Button buttonAdd;
    private Spinner spinnerCategory;

    private Bitmap selectedBitmap = null;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String selectedCategory = "";

    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Uri imageUri = result.getData().getData();
                            try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                    selectedBitmap = ImageDecoder.decodeBitmap(
                                            ImageDecoder.createSource(getContentResolver(), imageUri)
                                    );
                                } else {
                                    selectedBitmap = MediaStore.Images.Media.getBitmap(
                                            getContentResolver(), imageUri
                                    );
                                }
                                imageRecipe.setImageBitmap(selectedBitmap);
                            } catch (IOException e) {
                                e.printStackTrace();
                                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_recipe);

        editTitle = findViewById(R.id.edit_recipe_title);
        editDescription = findViewById(R.id.edit_recipe_description);
        imageRecipe = findViewById(R.id.image_recipe);
        buttonAdd = findViewById(R.id.button_add_recipe);
        spinnerCategory = findViewById(R.id.spinner_category);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // טוען קטגוריות ל-Spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.recipe_categories,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                selectedCategory = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedCategory = "";
            }
        });

        imageRecipe.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImageLauncher.launch(intent);
        });

        buttonAdd.setOnClickListener(v -> addRecipe());
    }

    private void addRecipe() {
        String title = editTitle.getText().toString().trim();
        String description = editDescription.getText().toString().trim();

        if (title.isEmpty() || description.isEmpty() || selectedCategory.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedBitmap == null) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show();
            return;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        selectedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] bytes = baos.toByteArray();

        List<Long> imageDataList = new ArrayList<>();
        for (byte b : bytes) imageDataList.add((long) b);

        String userId = mAuth.getCurrentUser().getUid();

        Map<String, Object> recipe = new HashMap<>();
        recipe.put("title", title);
        recipe.put("description", description);
        recipe.put("imageData", imageDataList);
        recipe.put("userId", userId);
        recipe.put("category", selectedCategory);

        db.collection("Recipes")
                .add(recipe)
                .addOnSuccessListener(doc -> {
                    Toast.makeText(this, "Recipe added!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to add recipe", Toast.LENGTH_SHORT).show());
    }
}
