package com.example.guysapp;

import static com.example.guysapp.FBRef.recipesRef;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
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
    private List<String> recipeIds = new ArrayList<>();
    private List<Recipe> filteredList = new ArrayList<>();
    private List<String> filteredIds = new ArrayList<>();
    private EditText searchEditText;
    private FrameLayout progressOverlay;

    private NetworkChangeReceiver networkChangeReceiver; // BroadcastReceiver

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
        recyclerView.setAdapter(adapter);

        loadRecipesRealtime();

        // הוספת מאזין לשינויים במצב החיבור
        networkChangeReceiver = new NetworkChangeReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        intentFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        registerReceiver(networkChangeReceiver, intentFilter);

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
        // הצג את ה-ProgressBar בזמן טעינה
        progressOverlay.setVisibility(View.VISIBLE);

        recipesRef.addSnapshotListener((queryDocumentSnapshots, e) -> {
            if (e != null) {
                Toast.makeText(this, "Failed to load recipes", Toast.LENGTH_SHORT).show();
                progressOverlay.setVisibility(View.GONE);
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

            // הסתר את ה-ProgressBar לאחר העדכון
            progressOverlay.setVisibility(View.GONE);
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

        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // ביטול רישום ה-Receiver
        unregisterReceiver(networkChangeReceiver);
    }
}
