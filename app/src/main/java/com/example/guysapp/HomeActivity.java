package com.example.guysapp;

import static com.example.guysapp.FBRef.recipesRef;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private Button buttonLogout;
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

        buttonLogout = findViewById(R.id.button_logout);
        addRecipeButton = findViewById(R.id.button_add_recipe);
        recyclerView = findViewById(R.id.recyclerView_recipes);
        searchEditText = findViewById(R.id.editText_search);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecipeAdapter(filteredList);
        recyclerView.setAdapter(adapter);

        loadRecipesRealtime();

        buttonLogout.setOnClickListener(v -> logoutUser());
        addRecipeButton.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, AddRecipeActivity.class)));

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) return true;
            else if (id == R.id.nav_add) {
                startActivity(new Intent(HomeActivity.this, AddRecipeActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(HomeActivity.this, ProfileActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });

        // חיפוש בזמן אמת
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

    private void logoutUser() {
        FBRef.mAuth.signOut();
        Toast.makeText(HomeActivity.this, "You have been logged out", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(HomeActivity.this, LoginActivity.class));
        finish();
    }
}
