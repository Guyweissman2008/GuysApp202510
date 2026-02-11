package com.example.guysapp;

public class SavedRecipe {


    private String userId;          // המשתמש ששמר את המתכון
    private String recipeId;        // המתכון המקורי
    private String recipeOwnerId;   // המשתמש שיצר את המתכון (לתצוגה והרשאות)

    // חובה ל-Firestore
    public SavedRecipe() {
    }

    public SavedRecipe(String userId,
                       String recipeId,
                       String recipeOwnerId) {

        this.userId = userId;
        this.recipeId = recipeId;
        this.recipeOwnerId = recipeOwnerId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(String recipeId) {
        this.recipeId = recipeId;
    }

    public String getRecipeOwnerId() {
        return recipeOwnerId;
    }

    public void setRecipeOwnerId(String recipeOwnerId) {
        this.recipeOwnerId = recipeOwnerId;
    }
}
