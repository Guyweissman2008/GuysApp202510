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
    private com.google.android.material.chip.ChipGroup chipGroup;
    private String selectedCategory = "הכל"; // קטגוריית ברירת מחדל
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
        setupCategoryChips();
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

        // מנקים את מה שהמשתמש הקליד
        String cleanQuery = cleanString(query);

        if (cleanQuery.isEmpty()) {
            filteredRecipes.addAll(allRecipes);
        } else {
            for (Recipe recipe : allRecipes) {
                // מנקים גם את שם המתכון ואת הקטגוריה מאותו "זבל" אפשרי
                String cleanTitle = cleanString(recipe.getTitle());
                String cleanCategory = cleanString(recipe.getCategory());

                // עכשיו משווים "נקי" מול "נקי"
                if (cleanTitle.contains(cleanQuery) || cleanCategory.contains(cleanQuery)) {
                    filteredRecipes.add(recipe);
                }
            }
        }

        sortFilteredRecipes();
        adapter.updateList(filteredRecipes);
    }
    // פונקציה לניקוי יסודי של הטקסט לפני השוואה
    private String cleanString(String input) {
        if (input == null) return "";

        // 1. הסרת ניקוד עברי (טווח היוניקוד של הניקוד)
        String noNikkud = input.replaceAll("[\u0591-\u05C7]", "");

        // 2. החלפת "רווח קשיח" (Non-breaking space) ברווח רגיל
        String normalSpaces = noNikkud.replace("\u00A0", " ");

        // 3. הסרת תווי כיוון נסתרים (RTL/LTR marks) שיש בוואטסאפ/אינטרנט
        String noDirectionChars = normalSpaces.replaceAll("[\u200E\u200F]", "");

        // 4. המרה לאותיות קטנות + הסרת רווחים מיותרים בצדדים
        return noDirectionChars.trim().toLowerCase();
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
    private void setupCategoryChips() {
        chipGroup = findViewById(R.id.categories_chip_group);
        chipGroup.removeAllViews();

        // 1. יצירת רשימה דינמית
        java.util.List<String> categoryList = new java.util.ArrayList<>();

        // קודם כל מוסיפים את "הכל" (שלא קיים ב-XML, כי הוא רק לסינון)
        categoryList.add("הכל");

        // עכשיו מושכים את הקטגוריות האמיתיות מה-XML (אותו מקור כמו ב-AddRecipeActivity)
        String[] resourceCategories = getResources().getStringArray(R.array.recipe_categories);
        for (String cat : resourceCategories) {
            categoryList.add(cat);
        }

        // 2. יצירת הצ'יפים בלולאה
        for (String cat : categoryList) {
            com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(this);
            chip.setText(cat);
            chip.setCheckable(true);

            // עיצוב
            chip.setChipBackgroundColorResource(android.R.color.white);
            chip.setChipStrokeColorResource(android.R.color.darker_gray);
            chip.setChipStrokeWidth(1f);

            chip.setOnClickListener(v -> {
                selectedCategory = cat;
                String currentSearchText = searchEditText.getText().toString();
                filterRecipes(currentSearchText);
            });

            chipGroup.addView(chip);
        }

        // סימון "הכל" כברירת מחדל
        if (chipGroup.getChildCount() > 0) {
            ((com.google.android.material.chip.Chip) chipGroup.getChildAt(0)).setChecked(true);
        }
    }
}
