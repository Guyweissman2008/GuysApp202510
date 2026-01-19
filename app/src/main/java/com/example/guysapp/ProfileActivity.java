package com.example.guysapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
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
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProfileActivity extends BaseActivity {

    // UI
    private ImageView profileImage;
    private TextView textEmail;
    private RecyclerView recyclerViewRecipes;
    private Button buttonMyRecipes;
    private Button buttonSavedRecipes;
    private ProgressBar progressBar;
    private ImageView buttonEditProfile; // או Button, תלוי מה בחרת ב-XML
    // משתנה שיחזיק את התצוגה של התמונה בתוך הדיאלוג
    private ImageView dialogProfileImageView;
    // משתנה שיחזיק את ה-Uri של התמונה החדשה שנבחרה (לפני שמירה)
    private android.net.Uri tempSelectedImageUri;

    private RecipeAdapter adapter;
    private androidx.activity.result.ActivityResultLauncher<android.content.Intent> imagePickerLauncher;
    private final List<Recipe> myRecipes = new ArrayList<>();
    private final List<Recipe> savedRecipes = new ArrayList<>();

    private boolean showingMyRecipes = true;

    private ListenerRegistration myRecipesListener;
    private ListenerRegistration savedRecipesListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        setupBottomNavigation(R.id.nav_profile);
// הגדרת מקבל התוצאה מהגלריה
        imagePickerLauncher = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        tempSelectedImageUri = result.getData().getData();

                        // אם הדיאלוג פתוח, נציג בו את התמונה החדשה שנבחרה
                        if (dialogProfileImageView != null && tempSelectedImageUri != null) {
                            dialogProfileImageView.setImageURI(tempSelectedImageUri);
                        }
                    }
                }
        );
        initViews();
        setupRecyclerView();
        setupListeners();

        loadUserProfile();
    }

    @Override
    protected void onStop() {
        super.onStop();

        removeListener(myRecipesListener);
        removeListener(savedRecipesListener);
    }

    private void initViews() {
        profileImage = findViewById(R.id.profile_image);
        textEmail = findViewById(R.id.text_email);
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
        buttonMyRecipes.setOnClickListener(v -> showMyRecipes());
        buttonSavedRecipes.setOnClickListener(v -> showSavedRecipes());
        buttonEditProfile.setOnClickListener(v -> showEditProfileDialog()); // <-- זה השם החדש והנכון
        profileImage.setOnClickListener(v -> openGallery());
    }

    private void removeListener(ListenerRegistration listener) {
        if (listener != null) {
            listener.remove();
        }
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = FBRef.mAuth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();

        adapter.setSavedScreen(false);
        adapter.setShowDelete(true);
        adapter.setCurrentUserID(userId);

        progressBar.setVisibility(View.VISIBLE);

        FBRef.refUsers.document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    progressBar.setVisibility(View.GONE);

                    if (!documentSnapshot.exists()) return;

                    setProfileImageIfExists(documentSnapshot);
                    setFullName(documentSnapshot);

                    loadMyRecipesRealtime(userId);
                    loadSavedRecipesRealtime(userId);
                    loadSavedRecipeIdsForHearts(userId);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(ProfileActivity.this,
                            "Failed to load profile",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void setProfileImageIfExists(DocumentSnapshot documentSnapshot) {
        List<?> imageDataRaw = (List<?>) documentSnapshot.get("imageData");
        if (imageDataRaw == null || imageDataRaw.isEmpty()) return;

        byte[] bytes = new byte[imageDataRaw.size()];
        for (int i = 0; i < imageDataRaw.size(); i++) {
            Object o = imageDataRaw.get(i);
            int value;
            if (o instanceof Long) value = ((Long) o).intValue();
            else if (o instanceof Integer) value = (Integer) o;
            else value = 0;
            bytes[i] = (byte) value;
        }

        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        profileImage.setImageBitmap(bitmap);
    }

    private void setFullName(DocumentSnapshot documentSnapshot) {
        String firstName = documentSnapshot.getString("firstName");
        String lastName = documentSnapshot.getString("lastName");
        textEmail.setText(((firstName != null ? firstName : "") + " " +
                (lastName != null ? lastName : "")).trim());
    }

    private void loadMyRecipesRealtime(String userId) {
        removeListener(myRecipesListener);

        myRecipesListener = FBRef.refRecipes
                .whereEqualTo("userId", userId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null) return;

                    myRecipes.clear();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Recipe recipe = doc.toObject(Recipe.class);
                        if (recipe == null) continue;
                        recipe.setRecipeId(doc.getId());
                        myRecipes.add(recipe);
                    }

                    if (showingMyRecipes) {
                        adapter.updateList(myRecipes);
                    }
                });
    }

    private void loadSavedRecipesRealtime(String userId) {
        removeListener(savedRecipesListener);

        savedRecipesListener = FBRef.refSavedRecipes
                .whereEqualTo("userId", userId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null) return;

                    savedRecipes.clear();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        SavedRecipe saved = doc.toObject(SavedRecipe.class);
                        if (saved == null) {
                            cleanInvalidSavedRecipe(doc.getId());
                            continue;
                        }

                        String rid = saved.getRecipeId();
                        if (rid == null || rid.isEmpty()) continue;

                        Recipe recipe = new Recipe();
                        recipe.setRecipeId(rid);
                        recipe.setTitle(saved.getTitle());
                        recipe.setUsername(saved.getAuthorName());
                        recipe.setUserId(saved.getRecipeOwnerId());
                        recipe.setImageData(saved.getImageData());

                        // --- Fetch original recipe to get category ---
                        FBRef.refRecipes.document(rid).get()
                                .addOnSuccessListener(originalDoc -> {
                                    if (originalDoc.exists()) {
                                        String category = originalDoc.getString("category");
                                        recipe.setCategory(category);
                                    }

                                    savedRecipes.add(recipe);

                                    if (!showingMyRecipes) {
                                        adapter.updateList(savedRecipes);
                                    }
                                })
                                .addOnFailureListener(err -> {
                                    // אם נכשל, פשוט מוסיפים בלי category
                                    savedRecipes.add(recipe);
                                    if (!showingMyRecipes) {
                                        adapter.updateList(savedRecipes);
                                    }
                                });
                    }
                });
    }

    private void loadSavedRecipeIdsForHearts(String userId) {
        FBRef.refSavedRecipes
                .whereEqualTo("userId", userId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    Set<String> ids = new HashSet<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        SavedRecipe saved = doc.toObject(SavedRecipe.class);
                        if (saved != null && saved.getRecipeId() != null) {
                            ids.add(saved.getRecipeId());
                        }
                    }
                    adapter.setSavedIds(ids);
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

    private void cleanInvalidSavedRecipe(String savedDocId) {
        FBRef.refSavedRecipes.document(savedDocId).delete();
    }
    // 1. פונקציה להצגת הדיאלוג


    // 2. הפונקציה המרכזית שמעדכנת את השם בכל המקומות
    private void updateNameEverywhere(String firstName, String lastName) {
        progressBar.setVisibility(View.VISIBLE);
        String userId = FBRef.mAuth.getCurrentUser().getUid();
        String fullName = firstName + " " + lastName;

        // שלב א: עדכון טבלת המשתמשים (Users)
        java.util.Map<String, Object> userUpdates = new java.util.HashMap<>();
        userUpdates.put("firstName", firstName);
        userUpdates.put("lastName", lastName);

        FBRef.refUsers.document(userId).update(userUpdates)
                .addOnSuccessListener(aVoid -> {
                    // עדכון מקומי של הטקסט במסך כדי שהמשתמש יראה מיד שינוי
                    textEmail.setText(fullName);

                    // שלב ב: חיפוש ועדכון כל המתכונים שהמשתמש יצר (עבור מסך הבית)
                    updateRecipesAuthorName(userId, fullName);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(ProfileActivity.this, "שגיאה בעדכון פרופיל", Toast.LENGTH_SHORT).show();
                });
    }

    // פונקציית עזר לעדכון מתכונים
    private void updateRecipesAuthorName(String userId, String newFullName) {
        // מחפשים את כל המתכונים שהמשתמש הזה יצר
        FBRef.refRecipes.whereEqualTo("userId", userId).get()
                .addOnSuccessListener(querySnapshot -> {

                    // אנו משתמשים ב-WriteBatch כדי לעשות הרבה עדכונים בבת אחת בצורה יעילה
                    com.google.firebase.firestore.WriteBatch batch = com.google.firebase.firestore.FirebaseFirestore.getInstance().batch();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        // "username" הוא השדה במתכון שמחזיק את שם המחבר (לפי המבנה המקובל)
                        // אם אצלך במודל של Recipe השדה נקרא אחרת (למשל authorName), יש לשנות כאן
                        batch.update(doc.getReference(), "username", newFullName);
                    }

                    // הרצת העדכון למתכונים
                    batch.commit().addOnSuccessListener(aVoid -> {
                        // שלב ג: עדכון מתכונים שמורים (SavedRecipes)
                        // אם שמרו מתכון שלך, צריך לעדכן שם את ה-AuthorName כדי שיראו את השם החדש
                        updateSavedRecipesAuthorName(userId, newFullName);

                    }).addOnFailureListener(e -> progressBar.setVisibility(View.GONE));
                });
    }

    // פונקציית עזר לעדכון מתכונים שמורים
    private void updateSavedRecipesAuthorName(String userId, String newFullName) {
        // כאן אנחנו מחפשים במסמכי SavedRecipes איפה שה-recipeOwnerId הוא המשתמש שלנו
        FBRef.refSavedRecipes.whereEqualTo("recipeOwnerId", userId).get()
                .addOnSuccessListener(querySnapshot -> {

                    com.google.firebase.firestore.WriteBatch batch = com.google.firebase.firestore.FirebaseFirestore.getInstance().batch();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        // בודקים איך קוראים לשדה אצלך ב-SavedRecipe. בדרך כלל authorName
                        batch.update(doc.getReference(), "authorName", newFullName);
                    }

                    batch.commit().addOnSuccessListener(aVoid -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(ProfileActivity.this, "הפרופיל עודכן בהצלחה בכל האפליקציה!", Toast.LENGTH_LONG).show();
                    });
                });
    }
    // פתיחת הגלריה
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    // המרה ושמירה של התמונה

    // פונקציית עזר להקטנת התמונה ושמירה על פרופורציות

    private void showEditProfileDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("עריכת פרופיל");

        tempSelectedImageUri = null;

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 10);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);

        // --- תמונה ---
        dialogProfileImageView = new ImageView(this);
        android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(250, 250);
        params.setMargins(0, 0, 0, 30);
        dialogProfileImageView.setLayoutParams(params);
        dialogProfileImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

        if (profileImage.getDrawable() != null) {
            dialogProfileImageView.setImageDrawable(profileImage.getDrawable());
        } else {
            dialogProfileImageView.setImageResource(R.drawable.ic_launcher_background); // שימי אייקון משלך
        }

        dialogProfileImageView.setOnClickListener(v -> openGallery());
        layout.addView(dialogProfileImageView);

        TextView clickToChange = new TextView(this);
        clickToChange.setText("לחצי על התמונה כדי לשנות");
        clickToChange.setGravity(Gravity.CENTER);
        layout.addView(clickToChange);

        // --- שדות טקסט (תיקון: מילוי אוטומטי של השם הנוכחי) ---
        final EditText inputFirstName = new EditText(this);
        inputFirstName.setHint("שם פרטי");

        final EditText inputLastName = new EditText(this);
        inputLastName.setHint("שם משפחה");

        // לוקחים את הטקסט הנוכחי ומפצלים אותו
        String currentFullName = textEmail.getText().toString();
        String[] parts = currentFullName.split(" ");

        if (parts.length > 0) {
            inputFirstName.setText(parts[0]); // ממלאים שם פרטי
        }
        if (parts.length > 1) {
            // מחברים את שאר החלקים למקרה שיש שם משפחה מורכב
            StringBuilder lastNameBuilder = new StringBuilder();
            for (int i = 1; i < parts.length; i++) {
                lastNameBuilder.append(parts[i]).append(" ");
            }
            inputLastName.setText(lastNameBuilder.toString().trim());
        }

        layout.addView(inputFirstName);
        layout.addView(inputLastName);

        builder.setView(layout);

        builder.setPositiveButton("שמור שינויים", (dialog, which) -> {
            String newFirst = inputFirstName.getText().toString().trim();
            String newLast = inputLastName.getText().toString().trim();

            // עכשיו זה לא יהיה ריק כי מילאנו את זה מראש, אלא אם המשתמש מחק הכל בכוונה
            if (newFirst.isEmpty() || newLast.isEmpty()) {
                Toast.makeText(ProfileActivity.this, "יש למלא שם מלא", Toast.LENGTH_SHORT).show();
                return;
            }

            saveProfileChanges(newFirst, newLast);
        });

        builder.setNegativeButton("ביטול", (dialog, which) -> dialog.cancel());
        builder.show();
    }
    private void saveProfileChanges(String firstName, String lastName) {
        progressBar.setVisibility(View.VISIBLE);
        String userId = FBRef.mAuth.getCurrentUser().getUid();
        String fullName = firstName + " " + lastName;

        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("firstName", firstName);
        updates.put("lastName", lastName);

        if (tempSelectedImageUri != null) {
            try {
                List<Integer> newImageData = processImageUri(tempSelectedImageUri);
                updates.put("imageData", newImageData);
            } catch (Exception e) {
                Toast.makeText(this, "שגיאה בעיבוד התמונה", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                return;
            }
        }

        FBRef.refUsers.document(userId).update(updates)
                .addOnSuccessListener(aVoid -> {
                    // 1. עדכון הכותרת למעלה
                    textEmail.setText(fullName);

                    // 2. עדכון התמונה במסך (אם השתנתה)
                    if (tempSelectedImageUri != null) {
                        profileImage.setImageURI(tempSelectedImageUri);
                    }

                    // --- התיקון: עדכון מיידי של הרשימה המקומית במסך הפרופיל ---
                    for (Recipe recipe : myRecipes) {
                        recipe.setUsername(fullName);
                    }
                    // מעדכנים גם את השמורים למקרה שאנחנו במסך השמורים
                    for (Recipe recipe : savedRecipes) {
                        recipe.setUsername(fullName);
                    }
                    // מודיעים לאדפטר שהמידע השתנה כדי שיצייר מחדש את השמות
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }

                    // 3. עדכון מסד הנתונים עבור מסך הבית ושאר המשתמשים
                    updateRecipesAuthorName(userId, fullName);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(ProfileActivity.this, "שגיאה בשמירה", Toast.LENGTH_SHORT).show();
                });
    }
    // פונקציית עזר להמרת התמונה לרשימה של מספרים (עבור פיירבייס)
    private List<Integer> processImageUri(android.net.Uri uri) throws java.io.IOException {
        // 1. טעינת התמונה המקורית מהגלריה
        Bitmap originalBitmap = android.provider.MediaStore.Images.Media.getBitmap(getContentResolver(), uri);

        // 2. הקטנת התמונה כדי שלא תהיה כבדה מדי (300x300 בערך)
        Bitmap resizedBitmap = scaleBitmapDown(originalBitmap, 300);

        // 3. דחיסה ל-JPG
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos); // איכות 70%
        byte[] data = baos.toByteArray();

        // 4. המרה ל-List<Integer>
        List<Integer> imageData = new ArrayList<>();
        for (byte b : data) {
            imageData.add((int) b);
        }
        return imageData;
    }

    // פונקציית עזר לחישוב הקטנת התמונה (שומרת על פרופורציות)
    private Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {
        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;

        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }
}
