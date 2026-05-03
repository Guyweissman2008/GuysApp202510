package com.example.guysapp;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ContextThemeWrapper;
import com.google.android.material.chip.Chip;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.app.AlertDialog;
import android.content.DialogInterface;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.os.Handler;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import android.text.InputType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.google.android.material.chip.ChipGroup;
public class HomeActivity extends BaseActivity {
    private FloatingActionButton addRecipeButton;
    private RecyclerView recyclerView;
    private RecipeAdapter adapter;
    private List<Recipe> allRecipes;
    private List<Recipe> filteredRecipes;
    private ChipGroup chipGroup;
    private String selectedCategory;
    private EditText searchEditText;
    private FrameLayout progressOverlay;
    private Set<String> savedRecipeIds;
    private ListenerRegistration recipesReg;
    private ListenerRegistration savedReg;
    private FloatingActionButton btnTimer;
    private NetworkChangeReceiver networkReceiver;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        selectedCategory = "All"; // קטגוריית ברירת מחדל
        initLists();
        initViews();
        setupRecyclerView();
        setupCategoryChips();
        setupListeners();
        setupBottomNavigation(R.id.nav_home);
        checkUserRoleAndUpdateAdapter();
        NotificationHelper.createNotificationChannel(this);
    }
    @Override
    protected void onStart() {
        super.onStart();
        clearRegistrations();
        recipesReg = loadRecipesRealtime();
        savedReg = loadSavedRecipeIdsRealtime();
        //(בדיקת אינטרנט)
        if (networkReceiver == null) {
            networkReceiver = new NetworkChangeReceiver();
        }
        // האזנה לשינוים באינטרנט
        android.content.IntentFilter filter = new android.content.IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkReceiver, filter);
    }
    @Override
    protected void onStop() {
        super.onStop();
        clearRegistrations();
        if (networkReceiver != null) {
            try {
                unregisterReceiver(networkReceiver);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
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
        btnTimer = findViewById(R.id.btn_kitchen_timer);
    }
    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecipeAdapter(filteredRecipes);
        adapter.setSavedScreen(false); // בשביל שיציג ת כל המתכונים
        adapter.setShowDelete(true); // כדי שתיהיה אופצית מחיקה לאדמין
        recyclerView.setAdapter(adapter);
    }
    private void checkUserRoleAndUpdateAdapter() {
    if (FBRef.mAuth.getCurrentUser() != null) {
     String uid = FBRef.mAuth.getCurrentUser().getUid();
     adapter.setCurrentUserID(uid);
     com.google.firebase.firestore.FirebaseFirestore.getInstance()
     .collection("Users")
     .whereEqualTo("userId", uid)
     .get()
     .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
     @Override
      public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
      if (!queryDocumentSnapshots.isEmpty()) {
      DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0);
       String role = documentSnapshot.getString("role");
       boolean isAdmin = "admin".equals(role);
        Log.d("RoleCheck", "User found! Role is: " + role + " | isAdmin: " + isAdmin);
         adapter.setIsAdmin(isAdmin);
      } else {
          Log.d("RoleCheck", "User document not found in DB!");
      }
     }
     })
     .addOnFailureListener(new OnFailureListener() {
     @Override
     public void onFailure(@NonNull Exception e) {
     Log.e("HomeActivity", "Error getting user role", e);
     }
     });
     }
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
    if (btnTimer != null) {
    btnTimer.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View v) {
                    showTimerDialog();
                }
    });
    }
    }
    private void showTimerDialog() {
    final EditText input = new EditText(HomeActivity.this);
    input.setHint("for example, 20");
    input.setInputType(InputType.TYPE_CLASS_NUMBER); // רק מספרים
    AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
    builder.setTitle("Set Timer (Minutes)");
    builder.setView(input);
    builder.setPositiveButton("Start", new DialogInterface.OnClickListener() {
    @Override
    public void onClick(DialogInterface dialog, int which) {
    startTimerFromInput(input.getText().toString().trim());
    }
    });
    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
    @Override
    public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
    });
    builder.show();
    }
    private void startTimerFromInput(String minutesStr) {
    if (minutesStr.isEmpty()) {
    Toast.makeText(this, "Please enter time!", Toast.LENGTH_SHORT).show();
    return;
    }
    int minutes;
    try {
    minutes = Integer.parseInt(minutesStr);
    } catch (Exception e) {
    Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
    return;
    }
    long durationInMillis = minutes * 60L * 1000L;
    if (minutes == 999) {
    durationInMillis = 10000;
    Toast.makeText(this, "Debug Mode: 10 seconds", Toast.LENGTH_SHORT).show();
    } else {
    Toast.makeText(this, " Timer set for " + minutes + " minutes", Toast.LENGTH_SHORT).show();
     }
     final String timeText = minutes + " minutes"; //כדי שלא ימחק שאני אעבור לפעולה
     new Handler().postDelayed(new Runnable() {
     @Override
     public void run() {
     NotificationHelper.showNotification(HomeActivity.this, "Food is Ready! ",
     "Time is up! (" + timeText + "), check your recipe."
      );
      }
      }, durationInMillis);
    }
    //ניקודי מאזינים
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
     progressOverlay.setVisibility(View.VISIBLE); //גלגל טעינה עד שיעלה
     return FBRef.refRecipes.addSnapshotListener(new EventListener<QuerySnapshot>() {
      @Override
      public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
      if (e != null) {
      progressOverlay.setVisibility(View.GONE);
      Toast.makeText(HomeActivity.this, "Firestore error: " + e.getMessage(),
      Toast.LENGTH_LONG).show();
      return;
      }
      progressOverlay.setVisibility(View.GONE);
      allRecipes.clear(); //מרוקנת את הרשימה הישנה
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
        String q = cleanString(query);
        String selectedClean = cleanString(selectedCategory);
        boolean isAllCategories = selectedClean.equals("all");
        List<Recipe> tempList = new ArrayList<>();
        for (Recipe recipe : allRecipes) {
        String title = cleanString(recipe.getTitle());
        String recCategory = cleanString(recipe.getCategory());
        boolean matchesSearch = q.isEmpty() || title.contains(q);
        boolean matchesCategory = isAllCategories || recCategory.equals(selectedClean);
        Log.d("filterRecipes", "title: " + title + ", recCat: " + recCategory + ", selectedCat: " + selectedClean + ", match: " + matchesCategory);
        if (matchesSearch && matchesCategory) {
        tempList.add(recipe);
        }
        }
        filteredRecipes.clear();
        filteredRecipes.addAll(tempList);
        sortFilteredRecipes();
        adapter.updateList(filteredRecipes);
    }
    private String cleanString(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase();
    }
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
      String q = (searchEditText != null) ? searchEditText.getText().toString() : "";
      filterRecipes(q);
      }
      });
    }
    private void sortFilteredRecipes() {
    Collections.sort(filteredRecipes, new Comparator<Recipe>() {
     @Override
      public int compare(Recipe a, Recipe b) {
      boolean aSaved = isSaved(a);
       boolean bSaved = isSaved(b);
       if (aSaved != bSaved) {
        if (aSaved) return -1;
        return 1;
       }
       int categoryCompare = safe(a.getCategory()).compareTo(safe(b.getCategory()));
       if (categoryCompare != 0) {
       return categoryCompare;
       }
       return safe(a.getTitle()).compareTo(safe(b.getTitle()));
       }
        });
    }
    private boolean isSaved(Recipe recipe) {
        if (recipe == null || recipe.getRecipeId() == null)
            return false;
        return savedRecipeIds.contains(recipe.getRecipeId());
    }
    private String safe(String s) {//ניקוי רווחים
        if (s == null) return "";
        return s.trim().toLowerCase();
    }
    private void setupCategoryChips() {
        chipGroup = findViewById(R.id.categories_chip_group);
        chipGroup.setSingleSelection(true);
        chipGroup.setSelectionRequired(true);
        chipGroup.removeAllViews();
        chipGroup.setOnCheckedChangeListener(null);
        List<String> categoryList = new ArrayList<>();
        categoryList.add("All");
        String[] resourceCategories = getResources().getStringArray(R.array.recipe_categories);
        Collections.addAll(categoryList, resourceCategories);
        for (int i = 0; i < categoryList.size(); i++) {
            String cat = categoryList.get(i);
            ContextThemeWrapper styleWrapper = new ContextThemeWrapper(this, R.style.CategoryChipStyle);
            Chip chip = new Chip(styleWrapper);
            chip.setText(cat);
            chip.setCheckable(true);
            chip.setId(i);
            chipGroup.addView(chip);
            if (cat.equals("All")) {
                chip.setChecked(true);
            }
        }
        chipGroup.setOnCheckedChangeListener(new ChipGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(ChipGroup group, int checkedId) {
                Chip checkedChip = group.findViewById(checkedId);
                if (checkedChip != null && checkedChip.isChecked()) {
                    selectedCategory = checkedChip.getText().toString();
                    Log.d("CategorySelection", "Selected: " + selectedCategory);
                    String currentSearchText = (searchEditText != null) ? searchEditText.getText().toString() : "";
                    filterRecipes(currentSearchText);
                }
            }
        });
    }
}