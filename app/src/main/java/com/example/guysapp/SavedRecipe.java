package com.example.guysapp;

import java.util.ArrayList;
import java.util.List;

public class SavedRecipe {
    private String authUserId;      // ה-UID של המשתמש ששמר את המתכון
    private String recipeId;        // ID של המתכון המקורי
    private String title;
    private List<Integer> imageData;
    private String authorName;      // שם היוצר המקורי
    private String recipeOwnerId;   // UID של היוצר המקורי

    public SavedRecipe() {
    }

    public SavedRecipe(String authUserId, String recipeId, String title, List<Integer> imageData, String authorName, String recipeOwnerId) {
        this.authUserId = authUserId;
        this.recipeId = recipeId;
        this.title = title;
        this.imageData = imageData;
        this.authorName = authorName;
        this.recipeOwnerId = recipeOwnerId;
    }

    public String getAuthUserId() {
        return authUserId;
    }

    public void setAuthUserId(String authUserId) {
        this.authUserId = authUserId;
    }

    public String getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(String recipeId) {
        this.recipeId = recipeId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<Integer> getImageData() {
        return imageData;
    }

    public void setImageData(List<Integer> imageData) {
        this.imageData = imageData;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getRecipeOwnerId() {
        return recipeOwnerId;
    }

    public void setRecipeOwnerId(String recipeOwnerId) {
        this.recipeOwnerId = recipeOwnerId;
    }
}