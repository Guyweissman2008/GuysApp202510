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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    private List<Recipe> recipeList;
    private List<String> recipeIds;

    private String currentUsername; // שם המשתמש הנוכחי (לפרופיל)
    private boolean showDelete = false; // האם להראות כפתור מחיקה

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();

    public RecipeAdapter(List<Recipe> recipeList, List<String> recipeIds) {
        this.recipeList = recipeList;
        this.recipeIds = recipeIds;
    }

    public void setCurrentUsername(String username) {
        this.currentUsername = username;
    }

    public void setShowDelete(boolean showDelete) {
        this.showDelete = showDelete;
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
        String recipeDocId = recipeIds.get(position);

        // כותרת, תיאור, קטגוריה
        holder.title.setText(recipe.getTitle() != null ? recipe.getTitle() : "");
        holder.description.setText(recipe.getDescription() != null ? recipe.getDescription() : "");
        holder.category.setText("קטגוריה: " + (recipe.getCategory() != null ? recipe.getCategory() : ""));
        holder.username.setText("הועלה על ידי: " + (recipe.getUsername() != null ? recipe.getUsername() : "משתמש אנונימי"));

        // טעינת תמונה
        if (recipe.getImageData() != null && !recipe.getImageData().isEmpty()) {
            try {
                byte[] bytes = new byte[recipe.getImageData().size()];
                for (int i = 0; i < bytes.length; i++) {
                    bytes[i] = recipe.getImageData().get(i).byteValue();
                }
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
        if (mAuth.getCurrentUser() != null) {
            String docId = mAuth.getCurrentUser().getUid() + "_" + recipeDocId;
            holder.saveButton.setOnClickListener(v -> {
                db.collection("SavedRecipes").document(docId).get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                db.collection("SavedRecipes").document(docId).delete();
                                holder.saveButton.setImageResource(R.drawable.ic_favorite_border);
                                Toast.makeText(v.getContext(), "הוסרה שמירה", Toast.LENGTH_SHORT).show();
                            } else {
                                db.collection("SavedRecipes").document(docId)
                                        .set(new SavedRecipe(mAuth.getCurrentUser().getUid(), recipeDocId));
                                holder.saveButton.setImageResource(R.drawable.ic_favorite_filled);
                                Toast.makeText(v.getContext(), "נשמר בהצלחה", Toast.LENGTH_SHORT).show();
                            }
                        });
            });
        }

        // כפתור מחיקה – רק אם showDelete=true ושם המשתמש מתאים
        if (showDelete && currentUsername != null && recipe.getUsername() != null
        /////////TODO        && currentUsername.equals(recipe.getUsername())
        ) {
            holder.deleteButton.setVisibility(View.VISIBLE);
            holder.deleteButton.setOnClickListener(v -> {
                db.collection("Recipes").document(recipeDocId).delete()
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(v.getContext(), "המתכון נמחק", Toast.LENGTH_SHORT).show();
                            recipeList.remove(position);
                            recipeIds.remove(position);
                            notifyItemRemoved(position);
                        })
                        .addOnFailureListener(e -> Toast.makeText(v.getContext(), "שגיאה במחיקה", Toast.LENGTH_SHORT).show());
            });
            holder.deleteButton.setVisibility(View.VISIBLE);
        } else {
            holder.deleteButton.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return recipeList.size();
    }

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
