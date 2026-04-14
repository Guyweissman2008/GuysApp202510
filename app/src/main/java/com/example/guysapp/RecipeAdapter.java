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
    private boolean isAdmin = false;
    public RecipeAdapter(List<Recipe> recipeList) {
        this.recipeList = recipeList;
    }

    public void setCurrentUserID(String currentUserID) {
        this.currentUserID = currentUserID;
    }

    public void setShowDelete(boolean showDelete) {
        this.showDelete = showDelete;
        notifyDataSetChanged();
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
    public void setIsAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
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
        if (recipe == null)
            return;

        String recipeId = recipe.getRecipeId();

        // כותרת ותיאור
        holder.title.setText(recipe.getTitle() != null ? recipe.getTitle() : "");
        holder.description.setText(recipe.getDescription() != null ? recipe.getDescription() : "");

        // קטגוריה (Category)
        holder.category.setText("Category: " + (recipe.getCategory() != null ? recipe.getCategory() : ""));

        // מחבר (Uploaded by)
        String displayAuthor = recipe.getUsername() != null ? recipe.getUsername() : "Anonymous";
        holder.username.setText("Uploaded by: " + displayAuthor);

        bindImage(holder, recipe);
        bindSaveState(holder, recipeId);
        bindSaveClick(holder, recipe, recipeId, displayAuthor);
        bindDeleteClick(holder, recipeId, recipe);
        bindEditClick(holder, recipeId, recipe);

        // זמן הכנה (mins)
        if (recipe.getPreparationTime() != null && !recipe.getPreparationTime().isEmpty()) {
            holder.textPrepTime.setText(recipe.getPreparationTime() + " mins");
            holder.textPrepTime.setVisibility(View.VISIBLE);
        } else {
            holder.textPrepTime.setVisibility(View.GONE);
        }

        // כפתור שיתוף (Share)
        holder.buttonShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // הודעת השיתוף באנגלית
                String shareBody = "Hey! check out this great recipe: \n\n" +
                        "Recipe Name: " + recipe.getTitle() + "\n" +
                        "Category: " + recipe.getCategory() + "\n\n" +
                        "Recommended!";

                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
                sendIntent.setType("text/plain");

                Intent shareIntent = Intent.createChooser(sendIntent, "Share recipe via...");
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

                // נסיון לקרוא אם המסמך קיים (האם כבר שמור?)
                FBRef.refSavedRecipes.document(docId).get()
                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                if (documentSnapshot.exists()) {
                                    // אם קיים - מחק (Unsave)
                                    FBRef.refSavedRecipes.document(docId).delete()
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    savedIds.remove(recipeId);
                                                    notifyDataSetChanged();
                                                    Toast.makeText(v.getContext(),
                                                            "Removed from favorites", // תרגום
                                                            Toast.LENGTH_SHORT).show();
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Toast.makeText(v.getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                                }
                                            });
                                } else {
                                    // אם לא קיים - צור חדש (Save)
                                    SavedRecipe savedRecipe = new SavedRecipe(
                                            uid,
                                            recipeId,
                                            recipe.getUserId()
                                    );

                                    FBRef.refSavedRecipes.document(docId)
                                            .set(savedRecipe)
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    savedIds.add(recipeId);
                                                    notifyDataSetChanged();
                                                    Toast.makeText(v.getContext(),
                                                            "Saved successfully", // תרגום
                                                            Toast.LENGTH_SHORT).show();
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Toast.makeText(v.getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                                }
                                            });
                                }
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(v.getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
            }
        });
    }

    private void bindDeleteClick(@NonNull RecipeViewHolder holder,
                                 String recipeId,
                                 Recipe recipe) {

        String recipeOwnerId = (recipe != null) ? recipe.getUserId() : "null";

        boolean isOwner = currentUserID != null
                && recipe != null
                && recipe.getUserId() != null
                && currentUserID.equals(recipe.getUserId());

        boolean canDelete = showDelete
                && recipeId != null
                && !recipeId.isEmpty()
                && (isOwner || isAdmin);

        // --- הדפסות למציאת הבעיה ---
        android.util.Log.d("DeleteDebug", "--- בדיקת מתכון: " + (recipe != null ? recipe.getTitle() : "null") + " ---");
        android.util.Log.d("DeleteDebug", "1. showDelete (האם מותר למחוק במסך?): " + showDelete);
        android.util.Log.d("DeleteDebug", "2. currentUserID (המשתמש שמחובר): " + currentUserID);
        android.util.Log.d("DeleteDebug", "3. recipeUserId (מי יצר את המתכון?): " + recipeOwnerId);
        android.util.Log.d("DeleteDebug", "4. isOwner (האם זה אותו אדם?): " + isOwner);
        android.util.Log.d("DeleteDebug", "5. isAdmin (האם הוא מנהל?): " + isAdmin);
        android.util.Log.d("DeleteDebug", "--> התוצאה הסופית (canDelete): " + canDelete);
        // ---------------------------

        if (!canDelete) {
            holder.deleteButton.setVisibility(View.GONE);
            holder.deleteButton.setOnClickListener(null);
            return;
        }

        holder.deleteButton.setVisibility(View.VISIBLE);
        holder.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int pos = holder.getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;

                FBRef.refRecipes.document(recipeId).delete()
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                RecipeAdapter.this.deleteSavedReferencesForRecipe(v.getContext(), recipeId);
                                Toast.makeText(v.getContext(),
                                        "Recipe deleted",
                                        Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(v.getContext(),
                                        "Error: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            }
                        });
            }
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
                v.getContext().startActivity(intent);
            }
        });
    }

    private String buildSavedDocId(String uid, String recipeId) {
        return uid + "_" + recipeId;
    }

    private void deleteSavedReferencesForRecipe(android.content.Context context, String recipeId) {
        FBRef.refSavedRecipes
                .whereEqualTo("recipeId", recipeId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot qs) {
                        // מחיקת כל ההתייחסויות השמורות למתכון זה (Batch Write)
                        if (qs.isEmpty()) return;

                        WriteBatch batch = FBRef.FBFS.batch();
                        for (DocumentSnapshot doc : qs.getDocuments()) {
                            batch.delete(doc.getReference());
                        }

                        batch.commit().addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(context, "Error removing saved refs: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(context,
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }


    static class RecipeViewHolder extends RecyclerView.ViewHolder {
        TextView textPrepTime;
        ImageView image;
        ImageView saveButton;
        ImageView deleteButton;
        ImageView editButton;
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
            textPrepTime = itemView.findViewById(R.id.text_prep_time);
        }
    }

    public void updateRecipeInList(Recipe updatedRecipe) {
        if (updatedRecipe == null || updatedRecipe.getRecipeId() == null)
            return;
        int position = findRecipePosition(updatedRecipe.getRecipeId());
        if (position != -1) {
            recipeList.set(position, updatedRecipe);
            notifyItemChanged(position);
        }
    }

    private int findRecipePosition(String recipeId) {
        if (recipeId == null || recipeList == null)
            return -1;

        for (int i = 0; i < recipeList.size(); i++) {
            Recipe r = recipeList.get(i);
            if (r == null) continue;

            String id = r.getRecipeId();
            if (id != null && id.equals(recipeId))
                return i;
        }
        return -1;
    }
}