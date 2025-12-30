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

    // סט של IDs של מתכונים שהמשתמש שמר (לב מלא/ריק בצורה יציבה)
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

        // ID של המתכון מגיע מתוך האובייקט
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

        // אם אין ID (תקלה בטעינה) - לא מאפשרים שמירה
        if (recipeId == null || recipeId.isEmpty()) {
            holder.saveButton.setOnClickListener(null);
            return;
        }

        // אם אין משתמש מחובר – לא מאפשרים שמירה
        if (FBRef.mAuth.getCurrentUser() == null) {
            holder.saveButton.setOnClickListener(null);
            return;
        }

        holder.saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int pos = holder.getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) {
                    return;
                }

                String uid = FBRef.mAuth.getCurrentUser().getUid();
                String docId = buildSavedDocId(uid, recipeId);

                FBRef.refSavedRecipes.document(docId).get()
                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot) {

                                if (documentSnapshot.exists()) {
                                    // ביטול שמירה
                                    FBRef.refSavedRecipes.document(docId).delete()
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    savedIds.remove(recipeId);
                                                    notifyDataSetChanged();
                                                    Toast.makeText(v.getContext(),
                                                            "הוסרה שמירה",
                                                            Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                } else {
                                    // שמירה
                                    SavedRecipe savedRecipe = new SavedRecipe(
                                            uid,               // userId של המשתמש ששמר
                                            recipeId,          // ID של המתכון המקורי
                                            recipe.getTitle(),
                                            recipe.getImageData(),
                                            displayAuthor,     // שם היוצר המקורי
                                            recipe.getUserId() // UID של היוצר המקורי
                                    );

                                    FBRef.refSavedRecipes.document(docId)
                                            .set(savedRecipe)
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    savedIds.add(recipeId);
                                                    notifyDataSetChanged();
                                                    Toast.makeText(v.getContext(),
                                                            "נשמר בהצלחה",
                                                            Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                }
                            }
                        });
            }
        });
    }

    private void bindDeleteClick(@NonNull RecipeViewHolder holder,
                                 String recipeId,
                                 Recipe recipe) {

        // מסך "שמורים": X רק אם אני ה-owner (מחיקת המתכון האמיתי)
        if (isSavedScreen) {
            boolean iAmOwner = currentUserID != null
                    && recipe != null
                    && recipe.getUserId() != null
                    && currentUserID.equals(recipe.getUserId())
                    && recipeId != null
                    && !recipeId.isEmpty();

            if (!iAmOwner) {
                holder.deleteButton.setVisibility(View.GONE);
                holder.deleteButton.setOnClickListener(null);
                return;
            }

            holder.deleteButton.setVisibility(View.VISIBLE);
            holder.deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = holder.getAdapterPosition();
                    if (pos == RecyclerView.NO_POSITION) {
                        return;
                    }

                    FBRef.refRecipes.document(recipeId).delete()
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    String uid = FBRef.mAuth.getCurrentUser().getUid();
                                    String savedDocId = buildSavedDocId(uid, recipeId);

                                    // קודם כול — אני מוחקת בוודאות את השמירה שלי
                                    FBRef.refSavedRecipes.document(savedDocId).delete();

                                    // אחר כך — מנסה למחוק שמירות של אחרים (אם מותר)
                                    deleteSavedReferencesForRecipe(recipeId);

                                    Toast.makeText(v.getContext(),
                                            "המתכון נמחק",
                                            Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(v.getContext(),
                                            "שגיאה: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                }
            });

            return;
        }

        // מצב רגיל: X מוחק מתכון מה-Recipes (רק לבעלים)
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
        holder.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int pos = holder.getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) {
                    return;
                }

                FBRef.refRecipes.document(recipeId).delete()
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                deleteSavedReferencesForRecipe(recipeId);
                                Toast.makeText(v.getContext(),
                                        "המתכון נמחק",
                                        Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(v.getContext(),
                                        "שגיאה: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            }
                        });
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
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot qs) {
                        FBRef.FBFS.runBatch(new WriteBatch.Function() {
                            @Override
                            public void apply(@NonNull WriteBatch batch) {
                                for (QueryDocumentSnapshot doc : qs) {
                                    batch.delete(doc.getReference());
                                }
                            }
                        });
                    }
                });
    }

    static class RecipeViewHolder extends RecyclerView.ViewHolder {

        ImageView image;
        ImageView saveButton;
        ImageView deleteButton;

        TextView title;
        TextView description;
        TextView category;
        TextView username;

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
