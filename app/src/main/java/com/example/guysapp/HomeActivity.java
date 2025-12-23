package com.example.guysapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import static com.example.guysapp.FBRef.recipesRef;

public class HomeActivity extends BaseActivity {

    private FloatingActionButton addRecipeButton;
    private RecyclerView recyclerView;
    private RecipeAdapter adapter;
    private List<Recipe> recipeList = new ArrayList<>();
    private List<String> recipeIds = new ArrayList<>();
    private List<Recipe> filteredList = new ArrayList<>();
    private List<String> filteredIds = new ArrayList<>();
    private EditText searchEditText;
    private FrameLayout progressOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        setupBottomNavigation(R.id.nav_home);

        addRecipeButton = findViewById(R.id.button_add_recipe);
        recyclerView = findViewById(R.id.recyclerView_recipes);
        searchEditText = findViewById(R.id.editText_search);
        progressOverlay = findViewById(R.id.progress_overlay);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecipeAdapter(filteredList, filteredIds);
        adapter.setShowDelete(false); // אין כפתור מחיקה בבית
        recyclerView.setAdapter(adapter);

        loadRecipesRealtime();

        addRecipeButton.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, AddRecipeActivity.class)));

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterRecipes(s.toString());
            }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    private void loadRecipesRealtime() {
        progressOverlay.setVisibility(FrameLayout.VISIBLE);

        recipesRef.addSnapshotListener((queryDocumentSnapshots, e) -> {
            if (e != null) {
                Toast.makeText(this, "Failed to load recipes", Toast.LENGTH_SHORT).show();
                progressOverlay.setVisibility(FrameLayout.GONE);
                return;
            }

            recipeList.clear();
            recipeIds.clear();

            if (queryDocumentSnapshots != null) {
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    Recipe recipe = doc.toObject(Recipe.class);
                    recipeList.add(recipe);
                    recipeIds.add(doc.getId());
                }
            }

            filterRecipes(searchEditText.getText().toString());
            progressOverlay.setVisibility(FrameLayout.GONE);
        });
    }

    private void filterRecipes(String query) {
        filteredList.clear();
        filteredIds.clear();

        if (query.isEmpty()) {
            filteredList.addAll(recipeList);
            filteredIds.addAll(recipeIds);
        } else {
            String lowerQuery = query.toLowerCase();
            for (int i = 0; i < recipeList.size(); i++) {
                Recipe r = recipeList.get(i);
                String title = r.getTitle() != null ? r.getTitle().toLowerCase() : "";
                String category = r.getCategory() != null ? r.getCategory().toLowerCase() : "";
                if (title.contains(lowerQuery) || category.contains(lowerQuery)) {
                    filteredList.add(r);
                    filteredIds.add(recipeIds.get(i));
                }
            }
        }

        adapter.updateList(filteredList, filteredIds);
    }
}
