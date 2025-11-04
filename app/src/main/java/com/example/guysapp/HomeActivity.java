package com.example.guysapp;

import static com.example.guysapp.FBRef.recipesRef;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // קישור בין רכיבי UI
        buttonLogout = findViewById(R.id.button_logout);
        addRecipeButton = findViewById(R.id.button_add_recipe);
        recyclerView = findViewById(R.id.recyclerView_recipes);

        // הגדרת RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecipeAdapter(recipeList);
        recyclerView.setAdapter(adapter);

        // טעינת מתכונים בזמן אמת
        loadRecipesRealtime();

        // כפתור יציאה
        buttonLogout.setOnClickListener(v -> logoutUser());

        // כפתור הוספת מתכון (הכפתור הצף)
        addRecipeButton.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, AddRecipeActivity.class)));

        // 🔽 תפריט תחתון (Bottom Navigation)
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_home); // ברירת מחדל: מסך הבית

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                // כבר במסך הבית
                return true;
            } else if (id == R.id.nav_add) {
                startActivity(new Intent(HomeActivity.this, AddRecipeActivity.class));
                overridePendingTransition(0, 0); // מעבר חלק
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(HomeActivity.this, ProfileActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }

    // טעינת מתכונים בזמן אמת
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
            adapter.notifyDataSetChanged();
        });
    }

    // יציאת משתמש
    private void logoutUser() {
        FBRef.mAuth.signOut();
        Toast.makeText(HomeActivity.this, "You have been logged out", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(HomeActivity.this, LoginActivity.class));
        finish();
    }
}
