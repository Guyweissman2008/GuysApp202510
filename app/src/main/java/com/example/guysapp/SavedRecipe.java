package com.example.guysapp;

public class SavedRecipe {
    private String userId;
    private String recipeId;

    // Constructor ריק נדרש על ידי Firestore
    public SavedRecipe() {}

    public SavedRecipe(String userId, String recipeId) {
        this.userId = userId;
        this.recipeId = recipeId;
    }

    // Getters ו-Setters
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
}
