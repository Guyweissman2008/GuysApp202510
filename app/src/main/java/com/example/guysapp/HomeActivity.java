package com.example.guysapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HomeActivity extends BaseActivity {

    private FloatingActionButton addRecipeButton;
    private RecyclerView recyclerView;
    private RecipeAdapter adapter;

    // כל המתכונים מהמסד + רשימה מסוננת לחיפוש
    private List<Recipe> allRecipes;
    private List<Recipe> filteredRecipes;

    private EditText searchEditText;
    private FrameLayout progressOverlay;

    // IDs של מתכונים שנשמרו ע"י המשתמש (ללב מלא/ריק)
    private Set<String> savedRecipeIds;

    private ListenerRegistration recipesReg;
    private ListenerRegistration savedReg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initLists();
        setupBottomNavigation(R.id.nav_home);
        initViews();
        setupRecyclerView();
        setupListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();

        clearRegistrations();
        recipesReg = loadRecipesRealtime();
        savedReg = loadSavedRecipeIdsRealtime();
    }

    @Override
    protected void onStop() {
        super.onStop();
        clearRegistrations();
    }

    private void initLists() {
        allRecipes = new ArrayList<>();
        filteredRecipes = new ArrayList<>();
        savedRecipeIds = new HashSet<>();
    }

    private void initViews() {
        addRecipeButton = findViewById(R.id.button_add_recipe);
        recyclerView = findViewById(R.id.recyclerView_recipes);
        searchEditText = findViewById(R.id.editText_search);
        progressOverlay = findViewById(R.id.progress_overlay);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // האדפטר מקבל רק רשימת מתכונים (בלי רשימת ids נפרדת)
        adapter = new RecipeAdapter(filteredRecipes);
        adapter.setSavedScreen(false);
        adapter.setShowDelete(false);
        recyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        addRecipeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, AddRecipeActivity.class));
            }
        });

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterRecipes(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    private void clearRegistrations() {
        if (recipesReg != null) {
            recipesReg.remove();
            recipesReg = null;
        }

        if (savedReg != null) {
            savedReg.remove();
            savedReg = null;
        }
    }

    private ListenerRegistration loadRecipesRealtime() {
        progressOverlay.setVisibility(View.VISIBLE);

        return FBRef.refRecipes.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {

                if (e != null) {
                    progressOverlay.setVisibility(View.GONE);
                    Toast.makeText(HomeActivity.this,
                            "Firestore error: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    return;
                }

                progressOverlay.setVisibility(View.GONE);
                allRecipes.clear();

                if (snapshots != null) {
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Recipe recipe = doc.toObject(Recipe.class);
                        if (recipe != null) {
                            recipe.setRecipeId(doc.getId());
                            allRecipes.add(recipe);
                        }
                    }
                }

                String q = (searchEditText != null) ? searchEditText.getText().toString() : "";
                filterRecipes(q);
            }
        });
    }

    private void filterRecipes(String query) {
        filteredRecipes.clear();

        if (query == null) {
            query = "";
        }

        String q = query.trim().toLowerCase();

        if (q.isEmpty()) {
            filteredRecipes.addAll(allRecipes);
        } else {
            for (Recipe recipe : allRecipes) {
                String title = recipe.getTitle() != null ? recipe.getTitle().toLowerCase() : "";
                String category = recipe.getCategory() != null ? recipe.getCategory().toLowerCase() : "";

                if (title.contains(q) || category.contains(q)) {
                    filteredRecipes.add(recipe);
                }
            }
        }

        sortFilteredRecipes();
        adapter.updateList(filteredRecipes);
    }

    // מאזין לרשימת המתכונים השמורים של המשתמש (כדי להציג לב מלא/ריק)
    private ListenerRegistration loadSavedRecipeIdsRealtime() {
        if (FBRef.mAuth.getCurrentUser() == null) {
            adapter.setSavedIds(new HashSet<>());
            return null;
        }

        String uid = FBRef.mAuth.getCurrentUser().getUid();

        return FBRef.refSavedRecipes
                .whereEqualTo("userId", uid)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {

                        if (e != null) {
                            adapter.setSavedIds(new HashSet<>());
                            return;
                        }

                        if (snapshots == null) {
                            return;
                        }

                        savedRecipeIds.clear();

                        for (QueryDocumentSnapshot doc : snapshots) {
                            SavedRecipe saved = doc.toObject(SavedRecipe.class);
                            if (saved != null && saved.getRecipeId() != null) {
                                savedRecipeIds.add(saved.getRecipeId());
                            }
                        }

                        adapter.setSavedIds(savedRecipeIds);
                        String q = searchEditText.getText().toString();
                        filterRecipes(q);
                    }
                });
    }

    // מיון start
    private void sortFilteredRecipes() {
        Collections.sort(filteredRecipes, new Comparator<Recipe>() {
            @Override
            public int compare(Recipe a, Recipe b) {

                boolean aSaved = isSaved(a);
                boolean bSaved = isSaved(b);

                // 1 - שמורים
                if (aSaved != bSaved) {
                    return aSaved ? -1 : 1;
                }

                // 2 - קטגוריה
                int categoryCompare =
                        safe(a.getCategory()).compareTo(safe(b.getCategory()));
                if (categoryCompare != 0) {
                    return categoryCompare;
                }

                // 3- כותרת
                return safe(a.getTitle()).compareTo(safe(b.getTitle()));
            }
        });
    }

    private boolean isSaved(Recipe recipe) {
        if (recipe == null || recipe.getRecipeId() == null) return false;
        return savedRecipeIds.contains(recipe.getRecipeId());
    }

    private String safe(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }
    // מיון end

}
