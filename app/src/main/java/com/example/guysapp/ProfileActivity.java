package com.example.guysapp;

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.bottomnavigation.BottomNavigationView;
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
    private BottomNavigationView bottomNavigationView;

    private boolean showingMyRecipes = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // findViewById
        profileImage = findViewById(R.id.profile_image);
        textEmail = findViewById(R.id.text_email);
        recyclerViewRecipes = findViewById(R.id.recyclerView_user_recipes);
        buttonMyRecipes = findViewById(R.id.button_my_recipes);
        buttonSavedRecipes = findViewById(R.id.button_saved_recipes);
        progressBar = findViewById(R.id.progressBar);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        recyclerViewRecipes.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecipeAdapter(new ArrayList<>(), new ArrayList<>());
        recyclerViewRecipes.setAdapter(adapter);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        setupBottomNavigation(R.id.nav_profile);

        // צבעי BottomNavigationView דינמיים
        setupBottomNavColors("#FF5C8D", "#AAAAAA"); // ורוד נבחר, אפור לא נבחר

        loadUserProfile();
        loadMyRecipesRealtime();
        loadSavedRecipesRealtime();

        // צבעים התחלתיים לכפתורים
        setActiveButton(buttonMyRecipes, buttonSavedRecipes);

        // לחיצות על כפתורים
        buttonMyRecipes.setOnClickListener(v -> {
            showMyRecipes();
            setActiveButton(buttonMyRecipes, buttonSavedRecipes);
        });

        buttonSavedRecipes.setOnClickListener(v -> {
            showSavedRecipes();
            setActiveButton(buttonSavedRecipes, buttonMyRecipes);
        });
    }

    // פונקציה לשינוי צבעים דינמית ב-BottomNavigationView
    private void setupBottomNavColors(String selectedHex, String unselectedHex) {
        int selectedColor = Color.parseColor(selectedHex);
        int unselectedColor = Color.parseColor(unselectedHex);

        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{-android.R.attr.state_checked}
        };
        int[] colors = new int[]{selectedColor, unselectedColor};

        ColorStateList csl = new ColorStateList(states, colors);
        bottomNavigationView.setItemIconTintList(csl);
        bottomNavigationView.setItemTextColor(csl);
    }

    // פונקציה לשנות צבע כפתור פעיל
    private void setActiveButton(Button active, Button inactive) {
        active.setBackgroundColor(Color.parseColor("#FF5C8D")); // ורוד
        inactive.setBackgroundColor(Color.parseColor("#DDDDDD")); // אפור
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();
        db.collection("Users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {

                        List<Long> imageData = (List<Long>) documentSnapshot.get("imageData");
                        if (imageData != null) {
                            byte[] bytes = new byte[imageData.size()];
                            for (int i = 0; i < imageData.size(); i++) {
                                bytes[i] = imageData.get(i).byteValue();
                            }
                            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                            profileImage.setImageBitmap(bitmap);
                        }

                        textEmail.setText(
                                documentSnapshot.getString("firstName") + " " +
                                        documentSnapshot.getString("lastName")
                        );
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

                    List<Task<DocumentSnapshot>> tasks = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : savedSnapshot) {
                        String recipeId = doc.getString("recipeId");

                        if (recipeId != null) {
                            savedRecipeIds.add(recipeId);

                            Task<DocumentSnapshot> task =
                                    db.collection("Recipes").document(recipeId).get();

                            tasks.add(task);

                            task.addOnSuccessListener(recipeDoc -> {
                                Recipe recipe = recipeDoc.toObject(Recipe.class);
                                if (recipe != null) {
                                    savedRecipes.add(recipe);
                                }
                            });
                        }
                    }

                    Tasks.whenAllSuccess(tasks).addOnSuccessListener(result -> {
                        if (!showingMyRecipes) {
                            adapter.updateList(savedRecipes, savedRecipeIds);
                        }
                    });
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
