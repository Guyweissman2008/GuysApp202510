package com.example.guysapp;

import java.util.List;

public class SavedRecipe {


    private String userId;      // המשתמש ששמר את המתכון
    private String recipeId;    // המתכון המקורי
    private String title;
    private List<Integer> imageData;
    private String authorName;      // פרטי היוצר המקורי (לתצוגה והרשאות)
    private String recipeOwnerId;   // פרטי היוצר המקורי (לתצוגה והרשאות)

    // חובה ל-Firestore
    public SavedRecipe() {
    }

    public SavedRecipe(String userId,
                       String recipeId,
                       String title,
                       List<Integer> imageData,
                       String authorName,
                       String recipeOwnerId) {

        this.userId = userId;
        this.recipeId = recipeId;
        this.title = title;
        this.imageData = imageData;
        this.authorName = authorName;
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
