package com.example.guysapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ProfileActivity extends BaseActivity {

    private ImageView profileImage;
    private TextView textEmail;
    private RecyclerView recyclerViewRecipes;
    private RecipeAdapter adapter;

    private List<Recipe> myRecipes = new ArrayList<>();
    private List<String> myRecipeIds = new ArrayList<>();
    private List<Recipe> savedRecipes = new ArrayList<>();
    private List<String> savedRecipeIds = new ArrayList<>();

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private Button buttonMyRecipes, buttonSavedRecipes;
    private ProgressBar progressBar;

    private boolean showingMyRecipes = true;
    private String currentUsername;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        profileImage = findViewById(R.id.profile_image);
        textEmail = findViewById(R.id.text_email);
        recyclerViewRecipes = findViewById(R.id.recyclerView_user_recipes);
        buttonMyRecipes = findViewById(R.id.button_my_recipes);
        buttonSavedRecipes = findViewById(R.id.button_saved_recipes);
        progressBar = findViewById(R.id.progressBar);

        recyclerViewRecipes.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecipeAdapter(new ArrayList<>(), new ArrayList<>());
        recyclerViewRecipes.setAdapter(adapter);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        setupBottomNavigation(R.id.nav_profile);

        loadUserProfile();

        buttonMyRecipes.setOnClickListener(v -> showMyRecipes());
        buttonSavedRecipes.setOnClickListener(v -> showSavedRecipes());

        loadMyRecipesRealtime();
        loadSavedRecipesRealtime();
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();

        db.collection("Users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {

                        currentUsername = documentSnapshot.getString("email"); // שם המשתמש לשימוש באדפטר

                        List<Long> imageData = (List<Long>) documentSnapshot.get("imageData");
                        if (imageData != null) {
                            byte[] bytes = new byte[imageData.size()];
                            for (int i = 0; i < imageData.size(); i++) {
                                bytes[i] = imageData.get(i).byteValue();
                            }
                            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                            profileImage.setImageBitmap(bitmap);
                        }

                        textEmail.setText(documentSnapshot.getString("firstName") + " " +
                                documentSnapshot.getString("lastName"));

                        // הגדרת שם המשתמש והצגת מחיקה באדפטר
                        adapter.setCurrentUsername(currentUsername);
                        adapter.setShowDelete(true);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show());
    }

    private void loadMyRecipesRealtime() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();

        db.collection("Recipes")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null) return;

                    myRecipes.clear();
                    myRecipeIds.clear();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Recipe recipe = doc.toObject(Recipe.class);
                        if (recipe != null) {
                            myRecipes.add(recipe);
                            myRecipeIds.add(doc.getId());
                        }
                    }

                    if (showingMyRecipes) {
                        adapter.updateList(myRecipes, myRecipeIds);
                    }
                });
    }

    private void loadSavedRecipesRealtime() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();

        db.collection("SavedRecipes")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((savedSnapshot, e) -> {
                    if (e != null || savedSnapshot == null) return;

                    savedRecipes.clear();
                    savedRecipeIds.clear();

                    for (QueryDocumentSnapshot doc : savedSnapshot) {
                        String recipeId = doc.getString("recipeId");
                        if (recipeId != null) {
                            savedRecipeIds.add(recipeId);
                            db.collection("Recipes").document(recipeId).get()
                                    .addOnSuccessListener(recipeDoc -> {
                                        Recipe recipe = recipeDoc.toObject(Recipe.class);
                                        if (recipe != null) savedRecipes.add(recipe);
                                        if (!showingMyRecipes) {
                                            adapter.updateList(savedRecipes, savedRecipeIds);
                                        }
                                    });
                        }
                    }
                });
    }

    private void showMyRecipes() {
        showingMyRecipes = true;
        adapter.updateList(myRecipes, myRecipeIds);
    }

    private void showSavedRecipes() {
        showingMyRecipes = false;
        adapter.updateList(savedRecipes, savedRecipeIds);
    }
}
