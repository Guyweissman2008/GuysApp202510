package com.example.guysapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProfileActivity extends BaseActivity {

    // UI
    private ImageView profileImage;
    private TextView textEmail;
    private ImageButton btnEditProfile;

    private RecyclerView recyclerViewRecipes;
    private Button buttonMyRecipes, buttonSavedRecipes;
    private ProgressBar progressBar;

    private RecipeAdapter adapter;

    private final List<Recipe> myRecipes = new ArrayList<>();
    private final List<Recipe> savedRecipes = new ArrayList<>();

    private boolean showingMyRecipes = true;

    private ListenerRegistration myRecipesListener, savedRecipesListener;

    // === חדש ===
    private Bitmap selectedProfileBitmap;
    private ImageView dialogProfileImage;

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

    private void initViews() {
        profileImage = findViewById(R.id.profile_image);
        textEmail = findViewById(R.id.text_email);
        btnEditProfile = findViewById(R.id.btn_edit_profile);

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
        buttonMyRecipes.setOnClickListener(v -> showMyRecipes());
        buttonSavedRecipes.setOnClickListener(v -> showSavedRecipes());
        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());
    }

    // ===== טעינת פרופיל =====
    private void loadUserProfile() {
        FirebaseUser user = FBRef.mAuth.getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();
        adapter.setCurrentUserID(uid);

        progressBar.setVisibility(View.VISIBLE);

        FBRef.refUsers.document(uid).get().addOnSuccessListener(doc -> {
            progressBar.setVisibility(View.GONE);
            if (!doc.exists()) return;

            setFullName(doc);
            setProfileImageIfExists(doc);

            loadMyRecipesRealtime(uid);
            loadSavedRecipesRealtime(uid);
            loadSavedRecipeIdsForHearts(uid);
        });
    }

    private void setFullName(DocumentSnapshot doc) {
        String first = doc.getString("firstName");
        String last = doc.getString("lastName");
        textEmail.setText((first + " " + last).trim());
    }

    private void setProfileImageIfExists(DocumentSnapshot doc) {
        List<?> raw = (List<?>) doc.get("imageData");
        if (raw == null) return;

        byte[] bytes = new byte[raw.size()];
        for (int i = 0; i < raw.size(); i++)
            bytes[i] = (byte) ((Long) raw.get(i)).intValue();

        profileImage.setImageBitmap(
                BitmapFactory.decodeByteArray(bytes, 0, bytes.length)
        );
    }

    // ===== דיאלוג עריכה =====
    private void showEditProfileDialog() {

        View view = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);

        EditText etFirst = view.findViewById(R.id.et_first_name);
        EditText etLast = view.findViewById(R.id.et_last_name);
        Button btnSave = view.findViewById(R.id.btn_save_profile);
        dialogProfileImage = view.findViewById(R.id.img_edit_profile);

        dialogProfileImage.setImageDrawable(profileImage.getDrawable());

        dialogProfileImage.setOnClickListener(v -> pickImageLauncher.launch(
                new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        ));

        String[] parts = textEmail.getText().toString().split(" ");
        if (parts.length > 0) etFirst.setText(parts[0]);
        if (parts.length > 1) etLast.setText(parts[1]);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        btnSave.setOnClickListener(v -> {
            updateUserProfile(
                    etFirst.getText().toString(),
                    etLast.getText().toString()
            );
            dialog.dismiss();
        });

        dialog.show();
    }

    // ===== גלריה =====
    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        try {
                            Uri uri = result.getData().getData();
                            selectedProfileBitmap =
                                    MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                            dialogProfileImage.setImageBitmap(selectedProfileBitmap);
                        } catch (Exception ignored) {}
                    });

    // ===== שמירה =====
    private void updateUserProfile(String first, String last) {

        if (FBRef.mAuth.getCurrentUser() == null) return;
        String uid = FBRef.mAuth.getCurrentUser().getUid();

        Map<String, Object> data = new HashMap<>();
        data.put("firstName", first);
        data.put("lastName", last);

        if (selectedProfileBitmap != null) {
            data.put("imageData", bitmapToList(selectedProfileBitmap));
            profileImage.setImageBitmap(selectedProfileBitmap);
        }

        textEmail.setText(first + " " + last);

        FBRef.refUsers.document(uid).update(data);
    }

    private List<Integer> bitmapToList(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 30, baos);
        byte[] bytes = baos.toByteArray();

        List<Integer> list = new ArrayList<>();
        for (byte b : bytes) list.add(b & 0xFF);
        return list;
    }

    // ===== מתכונים (לא שונה) =====
    private void loadMyRecipesRealtime(String uid) {
        myRecipesListener = FBRef.refRecipes
                .whereEqualTo("userId", uid)
                .addSnapshotListener((s, e) -> {
                    myRecipes.clear();
                    for (DocumentSnapshot d : s.getDocuments()) {
                        Recipe r = d.toObject(Recipe.class);
                        if (r != null) {
                            r.setRecipeId(d.getId());
                            myRecipes.add(r);
                        }
                    }
                    if (showingMyRecipes) adapter.updateList(myRecipes);
                });
    }

    private void loadSavedRecipesRealtime(String uid) {
        savedRecipesListener = FBRef.refSavedRecipes
                .whereEqualTo("userId", uid)
                .addSnapshotListener((s, e) -> {
                    savedRecipes.clear();
                    for (DocumentSnapshot d : s.getDocuments()) {
                        SavedRecipe sr = d.toObject(SavedRecipe.class);
                        if (sr == null) continue;

                        Recipe r = new Recipe();
                        r.setRecipeId(sr.getRecipeId());
                        r.setTitle(sr.getTitle());
                        r.setUsername(sr.getAuthorName());
                        r.setImageData(sr.getImageData());
                        savedRecipes.add(r);
                    }
                    if (!showingMyRecipes) adapter.updateList(savedRecipes);
                });
    }

    private void loadSavedRecipeIdsForHearts(String uid) {
        FBRef.refSavedRecipes.whereEqualTo("userId", uid)
                .addSnapshotListener((s, e) -> {
                    Set<String> ids = new HashSet<>();
                    for (QueryDocumentSnapshot d : s)
                        ids.add(d.getString("recipeId"));
                    adapter.setSavedIds(ids);
                });
    }

    private void showMyRecipes() {
        showingMyRecipes = true;
        adapter.updateList(myRecipes);
    }

    private void showSavedRecipes() {
        showingMyRecipes = false;
        adapter.updateList(savedRecipes);
    }
}
