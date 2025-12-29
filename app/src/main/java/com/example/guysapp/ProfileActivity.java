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

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

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

    private Button buttonMyRecipes, buttonSavedRecipes;
    private ProgressBar progressBar;

    private boolean showingMyRecipes = true;
    private String currentUserEmail;
    private ListenerRegistration savedIdsListener;

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

        setupBottomNavigation(R.id.nav_profile);

        buttonMyRecipes.setOnClickListener(v -> showMyRecipes());
        buttonSavedRecipes.setOnClickListener(v -> showSavedRecipes());

        loadUserProfile();
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = FBRef.mAuth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();
        adapter.setCurrentUserID(userId); // <-- current user's UID for ownership checks
        adapter.setShowDelete(true);

        FBRef.refUsers.document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) return;

                    currentUserEmail = documentSnapshot.getString("email");

                    // תמונת פרופיל
                    List<?> imageDataRaw = (List<?>) documentSnapshot.get("imageData");
                    if (imageDataRaw != null && !imageDataRaw.isEmpty()) {
                        byte[] bytes = new byte[imageDataRaw.size()];
                        for (int i = 0; i < imageDataRaw.size(); i++) {
                            Object o = imageDataRaw.get(i);
                            int value;
                            if (o instanceof Long) value = ((Long) o).intValue();
                            else if (o instanceof Integer) value = (Integer) o;
                            else value = 0; // fallback
                            bytes[i] = (byte) value;
                        }
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        profileImage.setImageBitmap(bitmap);
                    }

                    // שם מלא
                    String firstName = documentSnapshot.getString("firstName");
                    String lastName = documentSnapshot.getString("lastName");
                    textEmail.setText((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : ""));

                    loadMyRecipesRealtime();
                    loadSavedRecipesRealtime();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show());
    }


    private void loadMyRecipesRealtime() {
        if (FBRef.mAuth.getCurrentUser() == null) return;
        String userId = FBRef.mAuth.getCurrentUser().getUid();

        FBRef.recipesRef.whereEqualTo("userId", userId)
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
        if (FBRef.mAuth.getCurrentUser() == null) return;

        if (savedIdsListener != null) savedIdsListener.remove();

        savedIdsListener = FBRef.FBFS.collection("SavedRecipes")
                .whereEqualTo("userId", FBRef.mAuth.getCurrentUser().getUid())
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null) return;

                    savedRecipes.clear();
                    savedRecipeIds.clear();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        SavedRecipe saved = doc.toObject(SavedRecipe.class);
                        if (saved == null) continue;

                        Recipe recipe = new Recipe();
                        recipe.setTitle(saved.getTitle());
                        recipe.setUsername(saved.getAuthorName());       // שם היוצר
                        recipe.setUserId(saved.getRecipeOwnerId());      // UID של היוצר המקורי
                        recipe.setImageData(saved.getImageData());

                        savedRecipes.add(recipe);
                        savedRecipeIds.add(saved.getRecipeId());

                        // העתקת imageData כ-List<Integer>
                        recipe.setImageData(saved.getImageData());

                        savedRecipes.add(recipe);
                        savedRecipeIds.add(saved.getRecipeId());
                    }

                    if (!showingMyRecipes) {
                        adapter.updateList(savedRecipes, savedRecipeIds);
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
