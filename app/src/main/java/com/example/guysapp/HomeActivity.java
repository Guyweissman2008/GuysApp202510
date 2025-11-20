package com.example.guysapp;

import static com.example.guysapp.FBRef.recipesRef;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends BaseActivity {

    private FloatingActionButton addRecipeButton;
    private RecyclerView recyclerView;
    private RecipeAdapter adapter;
    private List<Recipe> recipeList = new ArrayList<>();
    private List<Recipe> filteredList = new ArrayList<>();
    private EditText searchEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // קביעת BottomNavigationView דרך BaseActivity
        setupBottomNavigation(R.id.nav_home);

        addRecipeButton = findViewById(R.id.button_add_recipe);
        recyclerView = findViewById(R.id.recyclerView_recipes);
        searchEditText = findViewById(R.id.editText_search);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecipeAdapter(filteredList);
        recyclerView.setAdapter(adapter);

        loadRecipesRealtime();

        // כפתור הוספת מתכון
        addRecipeButton.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, AddRecipeActivity.class)));

        // חיפוש בזמן אמת
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterRecipes(s.toString());
            }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    private void loadRecipesRealtime() {
        recipesRef.addSnapshotListener((queryDocumentSnapshots, e) -> {
            if (e != null) {
                Toast.makeText(this, "Failed to load recipes", Toast.LENGTH_SHORT).show();
                return;
            }

            recipeList.clear();
            if (queryDocumentSnapshots != null) {
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    Recipe recipe = doc.toObject(Recipe.class);
                    recipeList.add(recipe);
                }
            }
            filterRecipes(searchEditText.getText().toString());
        });
    }

    private void filterRecipes(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(recipeList);
        } else {
            String lowerQuery = query.toLowerCase();
            for (Recipe r : recipeList) {
                String title = r.getTitle() != null ? r.getTitle().toLowerCase() : "";
                String category = r.getCategory() != null ? r.getCategory().toLowerCase() : "";
                if (title.contains(lowerQuery) || category.contains(lowerQuery)) {
                    filteredList.add(r);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }
}
