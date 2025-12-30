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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProfileActivity extends BaseActivity {

    // UI
    private ImageView profileImage;
    private TextView textEmail;
    private RecyclerView recyclerViewRecipes;
    private Button buttonMyRecipes;
    private Button buttonSavedRecipes;
    private ProgressBar progressBar;

    private RecipeAdapter adapter;

    private final List<Recipe> myRecipes = new ArrayList<>();
    private final List<Recipe> savedRecipes = new ArrayList<>();

    private boolean showingMyRecipes = true;

    private ListenerRegistration myRecipesListener;
    private ListenerRegistration savedRecipesListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        setupBottomNavigation(R.id.nav_profile);

        initViews();
        setupRecyclerView();
        setupListeners();

        loadUserProfile();
    }

    @Override
    protected void onStop() {
        super.onStop();

        // לא להשאיר מאזינים פתוחים כשעוזבים מסך
        removeListener(myRecipesListener);
        myRecipesListener = null;

        removeListener(savedRecipesListener);
        savedRecipesListener = null;
    }

    private void initViews() {
        profileImage = findViewById(R.id.profile_image);
        textEmail = findViewById(R.id.text_email);
        recyclerViewRecipes = findViewById(R.id.recyclerView_user_recipes);

        buttonMyRecipes = findViewById(R.id.button_my_recipes);
        buttonSavedRecipes = findViewById(R.id.button_saved_recipes);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupRecyclerView() {
        recyclerViewRecipes.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecipeAdapter(new ArrayList<>());
        recyclerViewRecipes.setAdapter(adapter);
    }

    private void setupListeners() {
        buttonMyRecipes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMyRecipes();
            }
        });

        buttonSavedRecipes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSavedRecipes();
            }
        });
    }

    private void removeListener(ListenerRegistration listener) {
        if (listener != null) {
            listener.remove();
        }
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = FBRef.mAuth.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        String userId = currentUser.getUid();

        adapter.setSavedScreen(false);
        adapter.setShowDelete(true);
        adapter.setCurrentUserID(userId);

        progressBar.setVisibility(View.VISIBLE);

        FBRef.refUsers.document(userId).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        progressBar.setVisibility(View.GONE);

                        if (!documentSnapshot.exists()) {
                            return;
                        }

                        setProfileImageIfExists(documentSnapshot);
                        setFullName(documentSnapshot);

                        loadMyRecipesRealtime(userId);
                        loadSavedRecipesRealtime(userId);
                        loadSavedRecipeIdsForHearts(userId);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(ProfileActivity.this,
                                "Failed to load profile",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setProfileImageIfExists(DocumentSnapshot documentSnapshot) {
        List<?> imageDataRaw = (List<?>) documentSnapshot.get("imageData");
        if (imageDataRaw == null || imageDataRaw.isEmpty()) {
            return;
        }

        byte[] bytes = new byte[imageDataRaw.size()];

        for (int i = 0; i < imageDataRaw.size(); i++) {
            Object o = imageDataRaw.get(i);

            int value;
            if (o instanceof Long) {
                value = ((Long) o).intValue();
            } else if (o instanceof Integer) {
                value = (Integer) o;
            } else {
                value = 0;
            }

            bytes[i] = (byte) value;
        }

        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        profileImage.setImageBitmap(bitmap);
    }

    private void setFullName(DocumentSnapshot documentSnapshot) {
        String firstName = documentSnapshot.getString("firstName");
        String lastName = documentSnapshot.getString("lastName");

        String fullName =
                (firstName != null ? firstName : "") + " " +
                        (lastName != null ? lastName : "");

        textEmail.setText(fullName.trim());
    }

    private void loadMyRecipesRealtime(String userId) {
        removeListener(myRecipesListener);

        myRecipesListener = FBRef.refRecipes
                .whereEqualTo("userId", userId)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshot,
                                        @Nullable FirebaseFirestoreException e) {

                        if (e != null || snapshot == null) {
                            return;
                        }

                        myRecipes.clear();

                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            Recipe recipe = doc.toObject(Recipe.class);
                            if (recipe == null) {
                                continue;
                            }

                            recipe.setRecipeId(doc.getId());
                            myRecipes.add(recipe);
                        }

                        if (showingMyRecipes) {
                            adapter.updateList(myRecipes);
                        }
                    }
                });
    }

    private void loadSavedRecipesRealtime(String userId) {
        removeListener(savedRecipesListener);

        savedRecipesListener = FBRef.refSavedRecipes
                .whereEqualTo("userId", userId)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshot,
                                        @Nullable FirebaseFirestoreException e) {

                        if (e != null || snapshot == null) {
                            return;
                        }

                        savedRecipes.clear();

                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            SavedRecipe saved = doc.toObject(SavedRecipe.class);
                            if (saved == null) {
                                cleanInvalidSavedRecipe(doc.getId()); // fix old bug
                                continue;
                            }

                            String rid = saved.getRecipeId();
                            if (rid == null || rid.isEmpty()) {
                                continue;
                            }

                            Recipe recipe = new Recipe();
                            recipe.setRecipeId(rid); // חשוב: זה ה-ID של המתכון המקורי
                            recipe.setTitle(saved.getTitle());
                            recipe.setUsername(saved.getAuthorName());
                            recipe.setUserId(saved.getRecipeOwnerId());
                            recipe.setImageData(saved.getImageData());

                            savedRecipes.add(recipe);
                        }

                        if (!showingMyRecipes) {
                            adapter.updateList(savedRecipes);
                        }
                    }
                });
    }

    // כדי שהלב במסך פרופיל יוצג נכון גם ברשימת "שמורים"
    private void loadSavedRecipeIdsForHearts(String userId) {
        FBRef.refSavedRecipes
                .whereEqualTo("userId", userId)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots,
                                        @Nullable FirebaseFirestoreException e) {

                        if (e != null || snapshots == null) {
                            return;
                        }

                        Set<String> ids = new HashSet<>();

                        for (QueryDocumentSnapshot doc : snapshots) {
                            SavedRecipe saved = doc.toObject(SavedRecipe.class);
                            if (saved != null && saved.getRecipeId() != null) {
                                ids.add(saved.getRecipeId());
                            }
                        }

                        adapter.setSavedIds(ids);
                    }
                });
    }

    private void showMyRecipes() {
        showingMyRecipes = true;

        adapter.setSavedScreen(false);
        adapter.setShowDelete(true);

        adapter.updateList(myRecipes);
    }

    private void showSavedRecipes() {
        showingMyRecipes = false;

        adapter.setSavedScreen(true);   // אנחנו במסך שמורים
        adapter.setShowDelete(true);    // מאפשר X רק אם אני owner

        adapter.updateList(savedRecipes);
    }

    private void cleanInvalidSavedRecipe(String savedDocId) {
        FBRef.refSavedRecipes
                .document(savedDocId)
                .delete();
    }
}
