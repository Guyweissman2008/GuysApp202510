package com.example.guysapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
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
        loadMyRecipes();
        loadSavedRecipes();

        buttonMyRecipes.setOnClickListener(v -> showMyRecipes());
        buttonSavedRecipes.setOnClickListener(v -> showSavedRecipes());
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
                        textEmail.setText((String) documentSnapshot.get("firstName") + " " +
                                (String) documentSnapshot.get("lastName"));
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(ProfileActivity.this, "Failed to load profile image", Toast.LENGTH_SHORT).show());
    }

    private void loadMyRecipes() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        progressBar.setVisibility(View.VISIBLE);

        String userId = currentUser.getUid();

        db.collection("Recipes")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    myRecipes.clear();
                    myRecipeIds.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Recipe recipe = doc.toObject(Recipe.class);
                        if (recipe != null) {
                            myRecipes.add(recipe);
                            myRecipeIds.add(doc.getId());
                        }
                    }

                    if (showingMyRecipes) showMyRecipesAnimated();
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ProfileActivity.this, "Failed to load my recipes", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
    }

    private void loadSavedRecipes() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        progressBar.setVisibility(View.VISIBLE);

        String userId = currentUser.getUid();

        db.collection("SavedRecipes")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    savedRecipes.clear();
                    savedRecipeIds.clear();

                    List<Task<DocumentSnapshot>> tasks = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String recipeId = doc.getString("recipeId");
                        if (recipeId != null) {
                            Task<DocumentSnapshot> task = db.collection("Recipes").document(recipeId).get();
                            tasks.add(task);
                            task.addOnSuccessListener(recipeDoc -> {
                                Recipe recipe = recipeDoc.toObject(Recipe.class);
                                if (recipe != null) {
                                    savedRecipes.add(recipe);
                                    savedRecipeIds.add(recipeId);
                                }
                            });
                        }
                    }

                    Tasks.whenAllSuccess(tasks)
                            .addOnSuccessListener(results -> {
                                if (!showingMyRecipes) showSavedRecipesAnimated();
                                progressBar.setVisibility(View.GONE);
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ProfileActivity.this, "Failed to load saved recipes", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
    }

    private void showMyRecipesAnimated() {
        showingMyRecipes = true;

        recyclerViewRecipes.animate()
                .alpha(0f)
                .setDuration(150)
                .withEndAction(() -> {
                    adapter.updateList(myRecipes, myRecipeIds);

                    recyclerViewRecipes.animate()
                            .alpha(1f)
                            .setDuration(150)
                            .start();
                })
                .start();

        buttonMyRecipes.setBackgroundResource(R.drawable.button_active);
        buttonSavedRecipes.setBackgroundResource(R.drawable.button_inactive);
    }

    private void showSavedRecipesAnimated() {
        showingMyRecipes = false;

        recyclerViewRecipes.animate()
                .alpha(0f)
                .setDuration(150)
                .withEndAction(() -> {
                    adapter.updateList(savedRecipes, savedRecipeIds);

                    recyclerViewRecipes.animate()
                            .alpha(1f)
                            .setDuration(150)
                            .start();
                })
                .start();

        buttonMyRecipes.setBackgroundResource(R.drawable.button_inactive);
        buttonSavedRecipes.setBackgroundResource(R.drawable.button_active);
    }

    // למי שרוצה להחליף בלי אנימציה:
    private void showMyRecipes() {
        showingMyRecipes = true;
        adapter.updateList(myRecipes, myRecipeIds);
        buttonMyRecipes.setBackgroundResource(R.drawable.button_active);
        buttonSavedRecipes.setBackgroundResource(R.drawable.button_inactive);
    }

    private void showSavedRecipes() {
        showingMyRecipes = false;
        adapter.updateList(savedRecipes, savedRecipeIds);
        buttonMyRecipes.setBackgroundResource(R.drawable.button_inactive);
        buttonSavedRecipes.setBackgroundResource(R.drawable.button_active);
    }
}
