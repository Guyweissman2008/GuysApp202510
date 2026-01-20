package com.example.guysapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    private List<Recipe> recipeList;
    private String currentUserID;
    private boolean showDelete = false;
    private boolean isSavedScreen = false;
    private Set<String> savedIds = new HashSet<>();

    public RecipeAdapter(List<Recipe> recipeList) {
        this.recipeList = recipeList;
    }

    public void setCurrentUserID(String currentUserID) {
        this.currentUserID = currentUserID;
    }

    public void setShowDelete(boolean showDelete) {
        this.showDelete = showDelete;
    }

    public void setSavedScreen(boolean savedScreen) {
        this.isSavedScreen = savedScreen;
    }

    public void setSavedIds(Set<String> savedIds) {
        this.savedIds = (savedIds != null) ? savedIds : new HashSet<>();
        notifyDataSetChanged();
    }

    public void updateList(List<Recipe> newRecipes) {
        this.recipeList = newRecipes;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe recipe = recipeList.get(position);
        String recipeId = recipe.getRecipeId();

        holder.title.setText(recipe.getTitle() != null ? recipe.getTitle() : "");
        holder.description.setText(recipe.getDescription() != null ? recipe.getDescription() : "");
        holder.category.setText("קטגוריה: " + (recipe.getCategory() != null ? recipe.getCategory() : ""));
        String displayAuthor = recipe.getUsername() != null ? recipe.getUsername() : "משתמש אנונימי";
        holder.username.setText("הועלה על ידי: " + displayAuthor);

        bindImage(holder, recipe);
        bindSaveState(holder, recipeId);
        bindSaveClick(holder, recipe, recipeId, displayAuthor);
        bindDeleteClick(holder, recipeId, recipe);
        bindEditClick(holder, recipeId, recipe);

        holder.buttonShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //הטקסט שאני כותבת
                String shareBody = "היי! מצאתי מתכון מעולה \n\n" +
                        "שם המתכון: " + recipe.getTitle() + "\n" +
                        "קטגוריה: " + recipe.getCategory() + "\n\n" +
                        "מומלץ לנסות!";

            //יוצרת אינטנט
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
                sendIntent.setType("text/plain");

            //פתיחת שיתוף
                Intent shareIntent = Intent.createChooser(sendIntent, "שתף מתכון דרך...");

                // צריך Context כדי לפתוח מסך חדש, אנחנו לוקחים אותו מהכפתור עצמו,להשיג גישה ולבצע את פעולת השיתוף.
                v.getContext().startActivity(shareIntent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return recipeList != null ? recipeList.size() : 0;
    }

    private void bindImage(@NonNull RecipeViewHolder holder, Recipe recipe) {
        if (recipe.getImageData() != null && !recipe.getImageData().isEmpty()) {
            try {
                byte[] bytes = recipe.imageDataToBytes();
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                holder.image.setImageBitmap(bitmap);
            } catch (Exception e) {
                holder.image.setImageResource(R.mipmap.ic_launcher_round);
            }
        } else {
            holder.image.setImageResource(R.mipmap.ic_launcher_round);
        }
    }

    private void bindSaveState(@NonNull RecipeViewHolder holder, String recipeId) {
        if (recipeId != null && savedIds.contains(recipeId)) {
            holder.saveButton.setImageResource(R.drawable.ic_favorite_filled);
        } else {
            holder.saveButton.setImageResource(R.drawable.ic_favorite_border);
        }
    }

    private void bindSaveClick(@NonNull RecipeViewHolder holder,
                               Recipe recipe,
                               String recipeId,
                               String displayAuthor) {

        if (recipeId == null || recipeId.isEmpty() || FBRef.mAuth.getCurrentUser() == null) {
            holder.saveButton.setOnClickListener(null);
            return;
        }

        holder.saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int pos = holder.getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;

                String uid = FBRef.mAuth.getCurrentUser().getUid();
                String docId = buildSavedDocId(uid, recipeId);

                FBRef.refSavedRecipes.document(docId).get()
                        .addOnSuccessListener(documentSnapshot -> {

                            if (documentSnapshot.exists()) {
                                FBRef.refSavedRecipes.document(docId).delete()
                                        .addOnSuccessListener(aVoid -> {
                                            savedIds.remove(recipeId);
                                            notifyDataSetChanged();
                                            Toast.makeText(v.getContext(),
                                                    "הוסרה שמירה",
                                                    Toast.LENGTH_SHORT).show();
                                        });
                            } else {
                                SavedRecipe savedRecipe = new SavedRecipe(
                                        uid,
                                        recipeId,
                                        recipe.getTitle(),
                                        recipe.getImageData(),
                                        displayAuthor,
                                        recipe.getUserId()
                                );

                                FBRef.refSavedRecipes.document(docId)
                                        .set(savedRecipe)
                                        .addOnSuccessListener(aVoid -> {
                                            savedIds.add(recipeId);
                                            notifyDataSetChanged();
                                            Toast.makeText(v.getContext(),
                                                    "נשמר בהצלחה",
                                                    Toast.LENGTH_SHORT).show();
                                        });
                            }
                        });
            }
        });
    }

    private void bindDeleteClick(@NonNull RecipeViewHolder holder,
                                 String recipeId,
                                 Recipe recipe) {

        boolean canDelete = showDelete
                && currentUserID != null
                && recipe != null
                && recipe.getUserId() != null
                && currentUserID.equals(recipe.getUserId())
                && recipeId != null
                && !recipeId.isEmpty();

        if (!canDelete) {
            holder.deleteButton.setVisibility(View.GONE);
            holder.deleteButton.setOnClickListener(null);
            return;
        }

        holder.deleteButton.setVisibility(View.VISIBLE);
        holder.deleteButton.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;

            FBRef.refRecipes.document(recipeId).delete()
                    .addOnSuccessListener(aVoid -> {
                        deleteSavedReferencesForRecipe(recipeId);
                        Toast.makeText(v.getContext(),
                                "המתכון נמחק",
                                Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Toast.makeText(v.getContext(),
                            "שגיאה: " + e.getMessage(),
                            Toast.LENGTH_LONG).show());
        });
    }

    private void bindEditClick(@NonNull RecipeViewHolder holder,
                               String recipeId,
                               Recipe recipe) {

        boolean iAmOwner = currentUserID != null
                && recipe != null
                && recipe.getUserId() != null
                && currentUserID.equals(recipe.getUserId())
                && recipeId != null
                && !recipeId.isEmpty();

        if (!iAmOwner) {
            holder.editButton.setVisibility(View.GONE);
            holder.editButton.setOnClickListener(null);
            return;
        }

        holder.editButton.setVisibility(View.VISIBLE);
        holder.editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), AddRecipeActivity.class);
                intent.putExtra("recipeId", recipeId);
                /*
                intent.putExtra("title", recipe.getTitle());
                intent.putExtra("description", recipe.getDescription());
                intent.putExtra("category", recipe.getCategory());

                List<Integer> imageData = recipe.getImageData();
                if (imageData != null && !imageData.isEmpty()) {
                    byte[] imageBytes = new byte[imageData.size()];
                    for (int i = 0; i < imageData.size(); i++) {
                        imageBytes[i] = imageData.get(i).byteValue(); // <-- המרה נכונה
                    }
                    intent.putExtra("imageData", imageBytes);
                }
                 */
                v.getContext().startActivity(intent);
            }
        });
    }

    private String buildSavedDocId(String uid, String recipeId) {
        return uid + "_" + recipeId;
    }

    private void deleteSavedReferencesForRecipe(String recipeId) {
        FBRef.refSavedRecipes
                .whereEqualTo("recipeId", recipeId)
                .get()
                .addOnSuccessListener(qs -> FBRef.FBFS.runBatch(batch -> {
                    for (QueryDocumentSnapshot doc : qs) {
                        batch.delete(doc.getReference());
                    }
                }));
    }

    static class RecipeViewHolder extends RecyclerView.ViewHolder {

        ImageView image;
        ImageView saveButton;
        ImageView deleteButton;
        ImageView editButton; // <-- כפתור עריכה
        ImageView buttonShare;
        TextView title;
        TextView description;
        TextView category;
        TextView username;

        public RecipeViewHolder(@NonNull View itemView) {
            super(itemView);

            image = itemView.findViewById(R.id.image_recipe);
            saveButton = itemView.findViewById(R.id.image_save_recipe);
            deleteButton = itemView.findViewById(R.id.image_delete_recipe);
            editButton = itemView.findViewById(R.id.image_edit_recipe);
            buttonShare = itemView.findViewById(R.id.button_share);
            title = itemView.findViewById(R.id.text_recipe_title);
            description = itemView.findViewById(R.id.text_recipe_description);
            category = itemView.findViewById(R.id.text_recipe_category);
            username = itemView.findViewById(R.id.text_recipe_username);
        }
    }

    // פונקציה לעדכון פריט בודד ב-adapter
    public void updateRecipeInList(Recipe updatedRecipe) {
        int position = findRecipePosition(updatedRecipe.getRecipeId()); // חפש את המיקום של המתכון ברשימה
        if (position != -1) {
            recipeList.set(position, updatedRecipe); // עדכון המתכון ברשימה
            notifyItemChanged(position); // עדכון התצוגה
        }
    }

    // פונקציה למציאת המיקום של המתכון ברשימה
    private int findRecipePosition(String recipeId) {
        for (int i = 0; i < recipeList.size(); i++) {
            if (recipeList.get(i).getRecipeId().equals(recipeId)) {
                return i;
            }
        }
        return -1;
    }

}
