package com.example.guysapp;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
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
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import androidx.activity.result.ActivityResultLauncher;
public class ProfileActivity extends BaseActivity {
    private Bitmap tempSelectedBitmap;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ImageView profileImage;
    private TextView tvFullName;
    private RecyclerView recyclerViewRecipes;
    private Button buttonMyRecipes;
    private Button buttonSavedRecipes;
    private ProgressBar progressBar;
    private ImageView buttonEditProfile;
    private ImageView dialogProfileImageView;
    private android.net.Uri tempSelectedImageUri;
    private androidx.activity.result.ActivityResultLauncher<android.content.Intent> imagePickerLauncher;
    private RecipeAdapter adapter;
    private List<Recipe> myRecipes;
    private List<Recipe> savedRecipes;
    private boolean showingMyRecipes;
    private final Map<String, ListenerRegistration> savedRecipeDocListeners = new HashMap<>();    private ListenerRegistration myRecipesListener;
    private ListenerRegistration savedRecipesListener;
    private ListenerRegistration savedRecipeIdsForHeartsListener;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        showingMyRecipes = true;
        setupBottomNavigation(R.id.nav_profile);
        initLists();
        initViews();
        setupRecyclerView();
        setupListeners();
        initActivityResultLaunchers();
    }
    private void initLists() {
        myRecipes = new ArrayList<>();
        savedRecipes = new ArrayList<>();
    }
    @Override
    protected void onStart() {
        super.onStart();
        Log.d("ProfileActivity", "onStart called");

        loadUserProfile();// ׳˜׳¢׳™׳ ׳× ׳ ׳×׳•׳ ׳™ ׳”׳׳©׳×׳׳© ׳•׳”׳׳×׳›׳•׳ ׳™׳ ׳‘׳›׳ ׳₪׳¢׳ ׳©׳”׳׳¡׳ ׳¢׳•׳׳”
    }
    @Override
    protected void onStop() {
        super.onStop();
        Log.d("ProfileActivity", "onStop called");
        removeListener(myRecipesListener);      // ׳׳×׳›׳•׳ ׳™׳ ׳©׳׳™ (refRecipes ׳׳₪׳™ userId)
        removeListener(savedRecipesListener);   // ׳¨׳©׳™׳׳× ׳”׳©׳׳•׳¨׳™׳ (refSavedRecipes)
        removeListener(savedRecipeIdsForHeartsListener); // IDs ׳©׳ ׳©׳׳•׳¨׳™׳ ׳‘׳©׳‘׳™׳ ׳׳‘׳‘׳•׳×
        for (ListenerRegistration lr : savedRecipeDocListeners.values()) {
            removeListener(lr);                 // ׳׳׳–׳™׳ ׳׳›׳ ׳׳×׳›׳•׳ ׳©׳׳•׳¨
        }
        savedRecipeDocListeners.clear();
    }
    private void initViews() {
        profileImage = findViewById(R.id.profile_image);
        tvFullName = findViewById(R.id.text_email);
        recyclerViewRecipes = findViewById(R.id.recyclerView_user_recipes);
        buttonEditProfile = findViewById(R.id.button_edit_profile);
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
        buttonEditProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditProfileDialog();
            }
        });
        profileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditProfileDialog();
            }
        });
    }
    private void initActivityResultLaunchers() {
        imagePickerLauncher = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            tempSelectedImageUri = result.getData().getData();
                            tempSelectedBitmap = null; // ׳׳™׳₪׳•׳¡ ׳”׳×׳׳•׳ ׳” ׳׳”׳׳¦׳׳׳” ׳׳ ׳‘׳—׳¨׳ ׳• ׳׳”׳’׳׳¨׳™׳”
                            if (dialogProfileImageView != null && tempSelectedImageUri != null) {
                                dialogProfileImageView.setImageURI(tempSelectedImageUri);
                            }
                        }
                    }
                }
        );
        cameraLauncher = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Bundle extras = result.getData().getExtras();
                            if (extras != null) {
                                tempSelectedBitmap = (Bitmap) extras.get("data");
                                tempSelectedImageUri = null; // ׳׳™׳₪׳•׳¡ ׳”-URI ׳׳ ׳¦׳™׳׳׳ ׳• ׳‘׳׳¦׳׳׳”
                                if (dialogProfileImageView != null && tempSelectedBitmap != null) {
                                    dialogProfileImageView.setImageBitmap(tempSelectedBitmap);
                                }
                            }
                        }
                    }
                }
        );
    }
    private void showImageSourceDialog() {
        String[] options = {"Choose from Gallery", "Take Photo"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Image Source");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    openGallery();
                } else if (which == 1) {
                    openCamera();
                }
            }
        });
        builder.show();
    }
    private void openCamera() {
        Intent takePictureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(takePictureIntent);
    }
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }
    private void removeListener(ListenerRegistration listener) {
        if (listener != null) {
            listener.remove();
        }
    }
    private void loadUserProfile() {
        FirebaseUser currentUser = FBRef.mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = currentUser.getUid();
        adapter.setSavedScreen(!showingMyRecipes);
        adapter.setShowDelete(true);
        adapter.setCurrentUserID(userId);
        setLoading(true);
        FBRef.refUsers.document(userId).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        setLoading(false);
                        if (documentSnapshot == null || !documentSnapshot.exists()) {
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
                        setLoading(false);
                        Toast.makeText(ProfileActivity.this,
                                "Error loading profile",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void setProfileImageIfExists(DocumentSnapshot documentSnapshot) {
        Object imageDataObj = documentSnapshot.get("imageData");
        if (imageDataObj == null) return;
        Bitmap bitmap = null;
        if (imageDataObj instanceof com.google.firebase.firestore.Blob) {
            bitmap = ImageHelper.decodeBlobToBitmap((com.google.firebase.firestore.Blob) imageDataObj);
        }
        else if (imageDataObj instanceof java.util.List) {
            bitmap = ImageHelper.decodeFirestoreData((java.util.List<?>) imageDataObj);
        }
        if (bitmap != null) {
            profileImage.setImageBitmap(bitmap);
        }
    }
    private void NEW_setProfileImageIfExists(DocumentSnapshot documentSnapshot) {
        com.google.firebase.firestore.Blob imageBlob = documentSnapshot.get("imageData", com.google.firebase.firestore.Blob.class);
        if (imageBlob != null) {
            Bitmap bitmap = ImageHelper.decodeBlobToBitmap(imageBlob);
            if (bitmap != null) {
                profileImage.setImageBitmap(bitmap);
            }
        }
    }
    private void setFullName(DocumentSnapshot documentSnapshot) {
        String firstName = documentSnapshot.getString("firstName");
        String lastName = documentSnapshot.getString("lastName");
        tvFullName.setText(((firstName != null ? firstName : "") + " " +
                (lastName != null ? lastName : "")).trim());
    }
    private void loadMyRecipesRealtime(String userId) {
        removeListener(myRecipesListener);
        myRecipesListener = FBRef.refRecipes
                .whereEqualTo("userId", userId)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshot,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null || snapshot == null)
                            return;
                        myRecipes.clear();
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            Recipe recipe = doc.toObject(Recipe.class);
                            if (recipe != null) {
                                recipe.setRecipeId(doc.getId());
                                myRecipes.add(recipe);
                            }
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
                        if (e != null || snapshot == null)
                            return;
                        for (ListenerRegistration lr : savedRecipeDocListeners.values()) {
                            removeListener(lr);
                        }
                        savedRecipeDocListeners.clear();
                        savedRecipes.clear();
                        if (!showingMyRecipes)
                            adapter.updateList(savedRecipes);
                        if (snapshot.isEmpty())
                            return;
                        for (DocumentSnapshot savedDoc : snapshot.getDocuments()) {
                            SavedRecipe saved = savedDoc.toObject(SavedRecipe.class);
                            if (saved == null)
                                continue;
                            String rid = saved.getRecipeId();
                            if (rid == null || rid.trim().isEmpty())
                                continue;
                            ListenerRegistration lr = FBRef.refRecipes.document(rid)
                                    .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                                        @Override
                                        public void onEvent(@Nullable DocumentSnapshot recipeDoc,
                                                            @Nullable FirebaseFirestoreException err) {

                                            if (err != null || recipeDoc == null)
                                                return;
                                            if (!recipeDoc.exists()) {
                                                removeRecipeFromSavedList(rid);
                                                if (!showingMyRecipes)
                                                    adapter.updateList(savedRecipes);
                                                return;
                                            }
                                            Recipe r = recipeDoc.toObject(Recipe.class);
                                            if (r == null)
                                                return;
                                            r.setRecipeId(recipeDoc.getId());
                                            upsertSavedRecipe(r);
                                            if (!showingMyRecipes)
                                                adapter.updateList(savedRecipes);
                                        }
                                    });
                            savedRecipeDocListeners.put(rid, lr);
                        }
                    }
                });
    }
    private void upsertSavedRecipe(Recipe updated) {
        if (updated == null || updated.getRecipeId() == null)
            return;

        for (int i = 0; i < savedRecipes.size(); i++) {
            Recipe recipe = savedRecipes.get(i);
            if (recipe != null && updated.getRecipeId().equals(recipe.getRecipeId())) {
                savedRecipes.set(i, updated);
                return;
            }
        }
        savedRecipes.add(updated);
    }
    private void removeRecipeFromSavedList(String recipeId) {
        if (recipeId == null)
            return;
        for (int i = 0; i < savedRecipes.size(); i++) {
            Recipe recipe = savedRecipes.get(i);
            if (recipe != null && recipeId.equals(recipe.getRecipeId())) {
                savedRecipes.remove(i);
                return;
            }
        }
    }
    private void loadSavedRecipeIdsForHearts(String userId) {
        removeListener(savedRecipeIdsForHeartsListener);
        savedRecipeIdsForHeartsListener = FBRef.refSavedRecipes
                .whereEqualTo("userId", userId)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null || snapshots == null) {
                            adapter.setSavedIds(new HashSet<>());
                            return;
                        }
                        Set<String> ids = new HashSet<>();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            SavedRecipe saved = doc.toObject(SavedRecipe.class);
                            if (saved == null)
                                continue;
                            String rid = saved.getRecipeId();
                            if (rid == null)
                                continue;
                            rid = rid.trim();
                            if (!rid.isEmpty()) {
                                ids.add(rid);
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
        adapter.setSavedScreen(true);
        adapter.setShowDelete(true);
        adapter.updateList(savedRecipes);
    }
    private void showEditProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Profile");
        tempSelectedImageUri = null;
        tempSelectedBitmap = null;
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 10);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        dialogProfileImageView = new ImageView(this);
        android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(250, 250);
        params.setMargins(0, 0, 0, 30);
        dialogProfileImageView.setLayoutParams(params);
        dialogProfileImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        if (profileImage.getDrawable() != null) {
            dialogProfileImageView.setImageDrawable(profileImage.getDrawable());
        } else {
            dialogProfileImageView.setImageResource(R.drawable.ic_launcher_background);
        }
        dialogProfileImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImageSourceDialog(); // ׳׳₪׳¢׳™׳ ׳׳× ׳—׳׳•׳ ׳™׳× ׳”׳‘׳—׳™׳¨׳”
            }
        });
        layout.addView(dialogProfileImageView);
        TextView clickToChange = new TextView(this);
        clickToChange.setText("Tap image to change");
        clickToChange.setGravity(Gravity.CENTER);
        layout.addView(clickToChange);
        final EditText inputFirstName = new EditText(this);
        inputFirstName.setHint("first name");
        final EditText inputLastName = new EditText(this);
        inputLastName.setHint("last name");
        fillNameFromTextView(inputFirstName, inputLastName);
        layout.addView(inputFirstName);
        layout.addView(inputLastName);
        builder.setView(layout);
        builder.setPositiveButton("save", null);
        builder.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
        Button positiveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positiveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newFirst = inputFirstName.getText().toString().trim();
                String newLast = inputLastName.getText().toString().trim();
                if (newFirst.isEmpty() || newLast.isEmpty()) {
                    Toast.makeText(ProfileActivity.this, "Please enter your full name", Toast.LENGTH_SHORT).show();
                    return; // ׳”׳“׳™׳׳׳•׳’ ׳׳ ׳™׳™׳¡׳’׳¨
                }
                saveProfileChanges(newFirst, newLast, dialog, positiveBtn);
            }
        });
    }
    private void fillNameFromTextView(EditText etFirstName, EditText etLastName) {
        String currentFullName = tvFullName.getText().toString().trim();
        if (currentFullName.isEmpty())
            return;
        String[] parts = currentFullName.split(" ");
        if (parts.length > 0)
            etFirstName.setText(parts[0]);
        if (parts.length > 1) {
            StringBuilder lastNameBuilder = new StringBuilder();
            for (int i = 1; i < parts.length; i++) {
                lastNameBuilder.append(parts[i]).append(" ");
            }
            etLastName.setText(lastNameBuilder.toString().trim());
        }
    }
    private void saveProfileChanges(String firstName, String lastName, AlertDialog dialog, Button btnSave) {
        setSavingState(true, btnSave);
        FirebaseUser user = FBRef.mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show();            setSavingState(false, btnSave);
            return;
        }
        String userId = user.getUid();
        String fullName = firstName + " " + lastName;
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("firstName", firstName);
        updates.put("lastName", lastName);
        if (tempSelectedImageUri != null || tempSelectedBitmap != null) {
            try {
                com.google.firebase.firestore.Blob newImageBlob; // ׳©׳™׳ ׳•׳™ ׳-Blob

                if (tempSelectedImageUri != null) {
                    Bitmap galleryBitmap = ImageHelper.loadBitmapFromUri(this, tempSelectedImageUri);
                    newImageBlob = ImageHelper.bitmapToBlob(this, galleryBitmap, ImageHelper.SMALL_IMAGE);
                } else {
                    newImageBlob = ImageHelper.bitmapToBlob(this, tempSelectedBitmap, ImageHelper.SMALL_IMAGE);
                }

                if (newImageBlob != null) {
                    updates.put("imageData", newImageBlob);
                }
            } catch (Exception e) {
                Toast.makeText(this, "Image processing failed", Toast.LENGTH_SHORT).show();                setSavingState(false, btnSave);
                return;
            }
        }
        FBRef.refUsers.document(userId).update(updates)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        tvFullName.setText(fullName);
                        if (tempSelectedImageUri != null) {
                            profileImage.setImageURI(tempSelectedImageUri);
                        } else if (tempSelectedBitmap != null) {
                            profileImage.setImageBitmap(tempSelectedBitmap);
                        }
                        for (Recipe recipe : myRecipes) {
                            recipe.setUsername(fullName);
                        }
                        for (Recipe recipe : savedRecipes) {
                            recipe.setUsername(fullName);
                        }
                        if (adapter != null) {
                            adapter.notifyDataSetChanged();
                        }
                        updateRecipeAuthorNameInDatabase(userId, fullName);
                        setSavingState(false, btnSave);
                        dialog.dismiss(); // ׳¡׳•׳’׳¨׳™׳ ׳¨׳§ ׳׳—׳¨׳™ ׳”׳¦׳׳—׳”
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        setSavingState(false, btnSave);
                        if (tempSelectedImageUri != null) {
                            resetDialogImageToCurrentProfile();
                        }
                        Toast.makeText(ProfileActivity.this,
                                "Save failed. Please try again.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void updateRecipeAuthorNameInDatabase(String userId, String newFullName) {
        setLoading(true);
        FBRef.refRecipes.whereEqualTo("userId", userId).get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot querySnapshot) {
                        if (querySnapshot == null || querySnapshot.isEmpty()) {
                            setLoading(false);
                            return;
                        }
                        WriteBatch batch = com.google.firebase.firestore.FirebaseFirestore.getInstance().batch();
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            batch.update(doc.getReference(), "username", newFullName);
                        }
                        batch.commit()
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        updateSavedRecipeAuthorNameInDatabase(userId, newFullName);
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        setLoading(false);
                                        Toast.makeText(ProfileActivity.this,
                                                "Error updating my recipes: " + e.getMessage(),
                                                Toast.LENGTH_LONG).show();                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        ProfileActivity.this.setLoading(false);
                        Toast.makeText(ProfileActivity.this,
                                " failed. " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
    private void updateSavedRecipeAuthorNameInDatabase(String userId, String newFullName) {
        FBRef.refSavedRecipes.whereEqualTo("recipeOwnerId", userId).get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot querySnapshot) {
                        if (querySnapshot == null || querySnapshot.isEmpty()) {
                            setLoading(false);
                            Toast.makeText(ProfileActivity.this,
                                    "Profile updated successfully",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                        WriteBatch batch = com.google.firebase.firestore.FirebaseFirestore.getInstance().batch();
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            batch.update(doc.getReference(), "authorName", newFullName);
                        }
                        batch.commit().addOnSuccessListener(aVoid -> {
                                    setLoading(false);
                                    Toast.makeText(ProfileActivity.this,
                                            "Profile updated successfully across the app!",
                                            Toast.LENGTH_LONG).show();
                                })
                                .addOnFailureListener(e -> {
                                    setLoading(false);
                                    Toast.makeText(ProfileActivity.this,
                                            "Error updating saved recipes: "
                                                    + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        ProfileActivity.this.setLoading(false);
                        Toast.makeText(ProfileActivity.this,
                                "Error loading saved recipes for update: "
                                        + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
    private void setLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
    private void setSavingState(boolean isSaving, Button btnSave) {
        setLoading(isSaving);
        if (btnSave != null) {
            btnSave.setEnabled(!isSaving);
        }
    }
    private void resetDialogImageToCurrentProfile() {
        tempSelectedImageUri = null;
        tempSelectedBitmap = null;
        if (dialogProfileImageView == null) return;
        if (profileImage.getDrawable() != null) {
            dialogProfileImageView.setImageDrawable(profileImage.getDrawable());
        } else {
            dialogProfileImageView.setImageResource(R.drawable.ic_launcher_background);
        }
    }
}