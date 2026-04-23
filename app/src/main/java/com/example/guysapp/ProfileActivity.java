package com.example.guysapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
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

public class ProfileActivity extends BaseActivity {
    // UI
    private Bitmap tempSelectedBitmap; // שומר את התמונה שצולמה
    private androidx.activity.result.ActivityResultLauncher<android.content.Intent> cameraLauncher;
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
    private List<Recipe> myRecipes;// רשימת המתכונים שהמשתמש יצר
    private List<Recipe> savedRecipes;// רשימת המתכונים שהמשתמש שמר מאחרים

    private boolean showingMyRecipes;
    //  מאזינים לכל מסמך מתכון שמור
    private final java.util.Map<String, ListenerRegistration> savedRecipeDocListeners = new java.util.HashMap<>();
    private ListenerRegistration myRecipesListener;
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
        loadUserProfile();// טעינת נתוני המשתמש והמתכונים בכל פעם שהמסך עולה
    }

    @Override
    protected void onStop() {
        super.onStop();
        removeListener(myRecipesListener);      // מתכונים שלי (refRecipes לפי userId)
        removeListener(savedRecipesListener);   // רשימת השמורים (refSavedRecipes)
        removeListener(savedRecipeIdsForHeartsListener); // IDs של שמורים בשביל לבבות
        for (ListenerRegistration lr : savedRecipeDocListeners.values()) {
            removeListener(lr);                 // מאזין לכל מתכון שמור
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
        // הגדרת מקבל התוצאה מהגלריה
        imagePickerLauncher = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            tempSelectedImageUri = result.getData().getData();
                            tempSelectedBitmap = null; // איפוס התמונה מהמצלמה אם בחרנו מהגלריה
                            if (dialogProfileImageView != null && tempSelectedImageUri != null) {
                                dialogProfileImageView.setImageURI(tempSelectedImageUri);
                            }
                        }
                    }
                }
        );

        // הגדרת מקבל התוצאה מהמצלמה
        cameraLauncher = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Bundle extras = result.getData().getExtras();
                            if (extras != null) {
                                tempSelectedBitmap = (Bitmap) extras.get("data");
                                tempSelectedImageUri = null; // איפוס ה-URI אם צילמנו במצלמה
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
        String[] options = {"בחר מהגלריה", "צלם תמונה"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("בחר מקור תמונה");
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


        List<?> imageDataRaw = (List<?>) documentSnapshot.get("imageData");
        if (imageDataRaw == null || imageDataRaw.isEmpty())
            return;


        byte[] bytes = new byte[imageDataRaw.size()];
        for (int i = 0; i < imageDataRaw.size(); i++) {
            Object o = imageDataRaw.get(i);
            int value;
            if (o instanceof Long) value = ((Long) o).intValue();
            else if (o instanceof Integer) value = (Integer) o;
            else value = 0;
            bytes[i] = (byte) (value & 0xFF);
        }

        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        profileImage.setImageBitmap(bitmap);


        /*
        String base64Code = documentSnapshot.getString("imageBase64");

        if (base64Code != null && !base64Code.isEmpty()) {
            byte[] decodedString = Base64.decode(base64Code, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            profileImage.setImageBitmap(bitmap);
        }

         */
    }

    private void setFullName(DocumentSnapshot documentSnapshot) {
        String firstName = documentSnapshot.getString("firstName");
        String lastName = documentSnapshot.getString("lastName");
        tvFullName.setText(((firstName != null ? firstName : "") + " " +
                (lastName != null ? lastName : "")).trim());
    }

    private void loadMyRecipesRealtime(String userId) {
        // סגירת מאזין קודם כדי למנוע מאזינים כפולים
        removeListener(myRecipesListener);

        // מאזין RealTime לכל המתכונים שהמשתמש יצר
        myRecipesListener = FBRef.refRecipes
                .whereEqualTo("userId", userId)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    // כל שינוי במתכונים של המשתמש (הוספה / עדכון / מחיקה)
                    // מפעיל מחדש את onEvent עם snapshot עדכני
                    public void onEvent(@Nullable QuerySnapshot snapshot,
                                        @Nullable FirebaseFirestoreException e) {

                        if (e != null || snapshot == null)
                            return;

                        // מתחילים מרשימה נקייה – תמונה חדשה של "המתכונים שלי"
                        myRecipes.clear();

                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            Recipe recipe = doc.toObject(Recipe.class);
                            if (recipe != null) {
                                // שמירת ה־id של המסמך בתוך האובייקט
                                recipe.setRecipeId(doc.getId());
                                myRecipes.add(recipe);
                            }
                        }

                        // עדכון המסך רק אם המשתמש נמצא בתצוגת "המתכונים שלי"
                        if (showingMyRecipes) {
                            adapter.updateList(myRecipes);
                        }
                    }
                });
    }


    private void loadSavedRecipesRealtime(String userId) {
        // סגירת מאזין קודם כדי למנוע הצטברות מאזינים
        removeListener(savedRecipesListener);

        // מאזין RealTime לרשימת ה־SavedRecipes של המשתמש
        savedRecipesListener = FBRef.refSavedRecipes
                .whereEqualTo("userId", userId)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    // כל שינוי בשמירה / הסרה של מתכון
                    // מפעיל מחדש את onEvent עם snapshot עדכני
                    public void onEvent(@Nullable QuerySnapshot snapshot,
                                        @Nullable FirebaseFirestoreException e) {

                        if (e != null || snapshot == null)
                            return;

                        // סגירת מאזינים קודמים של מסמכי מתכונים
                        for (ListenerRegistration lr : savedRecipeDocListeners.values()) {
                            removeListener(lr);
                        }
                        savedRecipeDocListeners.clear();

                        // מתחילים מרשימה ריקה – snapshot חדש
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

                            // מאזין RealTime למסמך המתכון המקורי
                            ListenerRegistration lr = FBRef.refRecipes.document(rid)
                                    .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                                        @Override
                                        // כל שינוי במתכון עצמו (עדכון / מחיקה)
                                        public void onEvent(@Nullable DocumentSnapshot recipeDoc,
                                                            @Nullable FirebaseFirestoreException err) {

                                            if (err != null || recipeDoc == null)
                                                return;

                                            // אם המתכון המקורי נמחק – מסירים אותו מהשמורים
                                            if (!recipeDoc.exists()) {
                                                removeRecipeFromSavedList(rid);
                                                if (!showingMyRecipes)
                                                    adapter.updateList(savedRecipes);
                                                return;
                                            }

                                            // המתכון קיים – עדכון / הוספה לרשימת השמורים
                                            Recipe r = recipeDoc.toObject(Recipe.class);
                                            if (r == null)
                                                return;

                                            r.setRecipeId(recipeDoc.getId());
                                            upsertSavedRecipe(r);

                                            // עדכון מסך רק אם נמצאים בתצוגת Saved
                                            if (!showingMyRecipes)
                                                adapter.updateList(savedRecipes);
                                        }
                                    });

                            // שמירת המאזין כדי שנוכל לסגור אותו בהמשך
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
        // סגירת מאזין קודם כדי למנוע כפילויות
        removeListener(savedRecipeIdsForHeartsListener);

        // מאזין RealTime לרשימת המתכונים השמורים של המשתמש
        // משמש רק לצורך סימון לבבות (Saved / Not Saved)
        savedRecipeIdsForHeartsListener = FBRef.refSavedRecipes
                .whereEqualTo("userId", userId)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    // כל שמירה או הסרה של מתכון
                    // מעדכנת את סט ה־IDs של הלבבות
                    public void onEvent(@Nullable QuerySnapshot snapshots,
                                        @Nullable FirebaseFirestoreException e) {

                        if (e != null || snapshots == null) {
                            // במקרה של שגיאה – מאפסים לבבות
                            adapter.setSavedIds(new HashSet<>());
                            return;
                        }

                        // סט מזהי מתכונים שמורים (ללא כפילויות)
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

                        // עדכון האדפטר אילו מתכונים מסומנים כ־Saved
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

        // --- תמונה ---
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
                showImageSourceDialog(); // מפעיל את חלונית הבחירה
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
                    return; // הדיאלוג לא ייסגר
                }

                saveProfileChanges(newFirst, newLast, dialog, positiveBtn);
            }
        });
    }
    private List<Integer> processBitmap(Bitmap originalBitmap) {
        Bitmap resizedBitmap = scaleBitmapDown(originalBitmap, 300);
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        byte[] data = baos.toByteArray();

        List<Integer> imageData = new ArrayList<>();
        for (byte b : data) {
            imageData.add(b & 0xFF);
        }
        return imageData;
    }

    //אני מפרידה שוב את השם המלא כדי לאפשר למשתמש לערוך רק אחד מהם,מתי שהדיאלוג נפתח.
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
    // שליפת הטקסט שהמשתמש הקליד ושליחה לפונקציית השמירה בפיירבייס
    private void saveProfileChanges(String firstName, String lastName, AlertDialog dialog, Button btnSave) {
        setSavingState(true, btnSave);
        FirebaseUser user = FBRef.mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "אין משתמש מחובר", Toast.LENGTH_SHORT).show();
            setSavingState(false, btnSave);
            return;
        }
        String userId = user.getUid();
        // חיבור השם הפרטי ושם המשפחה למחרוזת אחת
        String fullName = firstName + " " + lastName;

        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        //  לשמירה בפיירסור בשדות נפרדים כדי לשמור על סדר
        updates.put("firstName", firstName);
        updates.put("lastName", lastName);
        if (tempSelectedImageUri != null || tempSelectedBitmap != null) {
            try {
                List<Integer> newImageData;
                if (tempSelectedImageUri != null) {
                    newImageData = processImageUri(tempSelectedImageUri);
                } else {
                    newImageData = processBitmap(tempSelectedBitmap);
                }
                updates.put("imageData", newImageData);
            } catch (Exception e) {
                Toast.makeText(this, "Image too large or not supported. Try another one.", Toast.LENGTH_SHORT).show();
                resetDialogImageToCurrentProfile();
                setSavingState(false, btnSave);
                return;
            }
        }
        FBRef.refUsers.document(userId).update(updates)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        tvFullName.setText(fullName);

                        // ---> כאן שמים את הקוד <---
                        // 2. עדכון התמונה במסך (אם השתנתה)
                        if (tempSelectedImageUri != null) {
                            profileImage.setImageURI(tempSelectedImageUri);
                        } else if (tempSelectedBitmap != null) {
                            profileImage.setImageBitmap(tempSelectedBitmap);
                        }
                        // -----------------------------

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
                        updateRecipeAuthorNameInDatabase(userId, fullName);
                        setSavingState(false, btnSave);
                        dialog.dismiss(); // סוגרים רק אחרי הצלחה
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // מחזירים UI למצב רגיל (מפסיקים טעינה ומאפשרים ללחוץ שוב)
                        setSavingState(false, btnSave);

                        // אם הייתה תמונה שנבחרה לדיאלוג – מחזירים לתמונה הקודמת
                        // (כדי לא להישאר במצב "חצי נבחר" אחרי כשל בשמירה)
                        if (tempSelectedImageUri != null) {
                            resetDialogImageToCurrentProfile();
                        }

                        Toast.makeText(ProfileActivity.this,
                                "Save failed. Please try again.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }


    // פונקציית עזר לעדכון מתכונים
    // Updates author display name in Recipes (field: "username")
    private void updateRecipeAuthorNameInDatabase(String userId, String newFullName) {
        setLoading(true);
        // מחפשים את כל המתכונים שהמשתמש הזה יצר
        FBRef.refRecipes.whereEqualTo("userId", userId).get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot querySnapshot) {

                        if (querySnapshot == null || querySnapshot.isEmpty()) {
                            setLoading(false);
                            return;
                        }

                        // אנו משתמשים ב-WriteBatch כדי לעשות הרבה עדכונים בבת אחת בצורה יעילה
                        WriteBatch batch = com.google.firebase.firestore.FirebaseFirestore.getInstance().batch();

                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            // "username" הוא השדה במתכון שמחזיק את שם המחבר (לפי המבנה המקובל)
                            // אם אצלך במודל של Recipe השדה נקרא אחרת (למשל authorName), יש לשנות כאן
                            batch.update(doc.getReference(), "username", newFullName);
                        }

                        // הרצת העדכון למתכונים
                        batch.commit()
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        // שלב ג: עדכון מתכונים שמורים (SavedRecipes)
                                        // אם שמרו מתכון שלך, צריך לעדכן שם את ה-AuthorName כדי שיראו את השם החדש
                                        updateSavedRecipeAuthorNameInDatabase(userId, newFullName);

                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        setLoading(false);
                                        Toast.makeText(ProfileActivity.this,
                                                "שגיאה בעדכון מתכונים שלי: " + e.getMessage(),
                                                Toast.LENGTH_LONG).show();
                                    }
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

    // פונקציית עזר לעדכון מתכונים שמורים
    // Updates author display name in SavedRecipes (field: "authorName")
    private void updateSavedRecipeAuthorNameInDatabase(String userId, String newFullName) {

        // כאן אנחנו מחפשים במסמכי SavedRecipes איפה שה-recipeOwnerId הוא המשתמש שלנו
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

                        // אנו משתמשים ב-WriteBatch כדי לעשות הרבה עדכונים בבת אחת בצורה יעילה
                        WriteBatch batch = com.google.firebase.firestore.FirebaseFirestore.getInstance().batch();

                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            // בודקים איך קוראים לשדה אצלך ב-SavedRecipe. בדרך כלל authorName
                            batch.update(doc.getReference(), "authorName", newFullName);
                        }

                        batch.commit().addOnSuccessListener(aVoid -> {
                                    setLoading(false);
                                    Toast.makeText(ProfileActivity.this,
                                            "הפרופיל עודכן בהצלחה בכל האפליקציה!",
                                            Toast.LENGTH_LONG).show();
                                })
                                .addOnFailureListener(e -> {
                                    setLoading(false);
                                    Toast.makeText(ProfileActivity.this,
                                            "שגיאה בעדכון מתכונים שמורים: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        ProfileActivity.this.setLoading(false);
                        Toast.makeText(ProfileActivity.this,
                                "שגיאה בשליפת המתכונים השמורים לעדכון: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }


    // פונקציית עזר להמרת התמונה לרשימה של מספרים (עבור פיירבייס)
    private List<Integer> processImageUri(android.net.Uri uri) throws java.io.IOException {
        Bitmap originalBitmap = android.provider.MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
        return processBitmap(originalBitmap);
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


    // Helper
    private void setLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    // Helper
    private void setSavingState(boolean isSaving, Button btnSave) {
        setLoading(isSaving);
        if (btnSave != null) {
            btnSave.setEnabled(!isSaving);
        }
    }

    // Helper
    private void resetDialogImageToCurrentProfile() {
        tempSelectedImageUri = null;
        tempSelectedBitmap = null; // הוספנו את זה

        if (dialogProfileImageView == null) return;

        if (profileImage.getDrawable() != null) {
            dialogProfileImageView.setImageDrawable(profileImage.getDrawable());
        } else {
            dialogProfileImageView.setImageResource(R.drawable.ic_launcher_background);
        }
    }

}