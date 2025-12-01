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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    private List<Recipe> recipeList;
    private List<String> recipeIds; // רשימת documentId של המתכונים
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();

    public RecipeAdapter(List<Recipe> recipeList, List<String> recipeIds) {
        this.recipeList = recipeList;
        this.recipeIds = recipeIds;
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

        holder.title.setText(recipe.getTitle());
        holder.description.setText(recipe.getDescription());
        holder.category.setText("קטגוריה: " + recipe.getCategory());

        // טעינת תמונה
        if (recipe.getImageData() != null) {
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
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String docId = currentUser.getUid() + "_" + recipeDocId;

            db.collection("SavedRecipes").document(docId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            holder.saveButton.setImageResource(R.drawable.ic_favorite_filled);
                        } else {
                            holder.saveButton.setImageResource(R.drawable.ic_favorite_border);
                        }
                    });

            holder.saveButton.setOnClickListener(v -> {
                db.collection("SavedRecipes").document(docId).get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                db.collection("SavedRecipes").document(docId).delete();
                                holder.saveButton.setImageResource(R.drawable.ic_favorite_border);
                                Toast.makeText(v.getContext(), "הסרת מתכון מהשמורים", Toast.LENGTH_SHORT).show();
                            } else {
                                db.collection("SavedRecipes").document(docId)
                                        .set(new SavedRecipe(currentUser.getUid(), recipeDocId));
                                holder.saveButton.setImageResource(R.drawable.ic_favorite_filled);
                                Toast.makeText(v.getContext(), "מתכון נשמר", Toast.LENGTH_SHORT).show();
                            }
                        });
            });
        }
    }

    @Override
    public int getItemCount() {
        return recipeList.size();
    }

    // מתודה לעדכון רשימות
    public void updateList(List<Recipe> newRecipes, List<String> newIds) {
        this.recipeList = newRecipes;
        this.recipeIds = newIds;
        notifyDataSetChanged();
    }

    static class RecipeViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        ImageView saveButton;
        TextView title;
        TextView description;
        TextView category;

        public RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.image_recipe);
            saveButton = itemView.findViewById(R.id.image_save_recipe);
            title = itemView.findViewById(R.id.text_recipe_title);
            description = itemView.findViewById(R.id.text_recipe_description);
            category = itemView.findViewById(R.id.text_recipe_category);
        }
    }
}
