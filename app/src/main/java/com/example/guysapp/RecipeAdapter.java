package com.example.guysapp;

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

import java.util.List;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    private List<Recipe> recipeList;
    private List<String> recipeIds;
    private String currentUserID;
    private boolean showDelete = false;

    public RecipeAdapter(List<Recipe> recipeList, List<String> recipeIds) {
        this.recipeList = recipeList;
        this.recipeIds = recipeIds;
    }

    public void setCurrentUserID(String currentUserID) { this.currentUserID = currentUserID; }
    public void setShowDelete(boolean showDelete) { this.showDelete = showDelete; }

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
        String recipeDocId = recipeIds.get(position);

        // טקסטים
        holder.title.setText(recipe.getTitle() != null ? recipe.getTitle() : "");
        holder.description.setText(recipe.getDescription() != null ? recipe.getDescription() : "");
        holder.category.setText("קטגוריה: " + (recipe.getCategory() != null ? recipe.getCategory() : ""));
        String displayAuthor = recipe.getUsername() != null ? recipe.getUsername() : "משתמש אנונימי";
        holder.username.setText("הועלה על ידי: " + displayAuthor);

        // הצגת תמונה
        if (recipe.getImageData() != null && !recipe.getImageData().isEmpty()) {
            try {
                byte[] bytes = recipe.getImageDataAsBytes();
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                holder.image.setImageBitmap(bitmap);
            } catch (Exception e) {
                e.printStackTrace();
                holder.image.setImageResource(R.mipmap.ic_launcher_round);
            }
        } else {
            holder.image.setImageResource(R.mipmap.ic_launcher_round);
        }

        // כפתור שמירה
        holder.saveButton.setImageResource(R.drawable.ic_favorite_border);
        if (FBRef.mAuth.getCurrentUser() != null) {
            String docId = FBRef.mAuth.getCurrentUser().getUid() + "_" + recipeDocId;
            holder.saveButton.setOnClickListener(v -> {
                FBRef.FBFS.collection("SavedRecipes").document(docId).get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                FBRef.FBFS.collection("SavedRecipes").document(docId).delete();
                                holder.saveButton.setImageResource(R.drawable.ic_favorite_border);
                                Toast.makeText(v.getContext(), "הוסרה שמירה", Toast.LENGTH_SHORT).show();
                            } else {
                                FBRef.FBFS.collection("SavedRecipes").document(docId)
                                        .set(new SavedRecipe(
                                                FBRef.mAuth.getCurrentUser().getUid(), // המשתמש ששמר
                                                recipeDocId,
                                                recipe.getTitle(),
                                                recipe.getImageData(),
                                                displayAuthor,
                                                recipe.getUserId()  // UID של היוצר המקורי
                                        ));

                                holder.saveButton.setImageResource(R.drawable.ic_favorite_filled);
                                Toast.makeText(v.getContext(), "נשמר בהצלחה", Toast.LENGTH_SHORT).show();
                            }
                        });
            });
        }

        //RINAT
        // כפתור מחיקה
        if (showDelete
                && currentUserID != null
                && recipe.getUserId() != null
                && currentUserID.equals(recipe.getUserId())) {
            holder.deleteButton.setVisibility(View.VISIBLE);
            holder.deleteButton.setOnClickListener(v -> {
                FBRef.recipesRef.document(recipeDocId).delete()
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(v.getContext(), "המתכון נמחק", Toast.LENGTH_SHORT).show();
                            recipeList.remove(position);
                            recipeIds.remove(position);
                            notifyItemRemoved(position);
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(v.getContext(), "שגיאה במחיקה", Toast.LENGTH_SHORT).show());
            });
        } else {
            holder.deleteButton.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() { return recipeList.size(); }

    public void updateList(List<Recipe> newRecipes, List<String> newIds) {
        this.recipeList = newRecipes;
        this.recipeIds = newIds;
        notifyDataSetChanged();
    }

    static class RecipeViewHolder extends RecyclerView.ViewHolder {
        ImageView image, saveButton, deleteButton;
        TextView title, description, category, username;

        public RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.image_recipe);
            saveButton = itemView.findViewById(R.id.image_save_recipe);
            deleteButton = itemView.findViewById(R.id.image_delete_recipe);
            title = itemView.findViewById(R.id.text_recipe_title);
            description = itemView.findViewById(R.id.text_recipe_description);
            category = itemView.findViewById(R.id.text_recipe_category);
            username = itemView.findViewById(R.id.text_recipe_username);
        }
    }
}
