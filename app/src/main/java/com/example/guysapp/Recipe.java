package com.example.guysapp;
import com.google.firebase.firestore.Blob;
public class Recipe {
    private String recipeId;
    private String title;
    private String description;
    private String category;
    private Blob imageData;
    private String username;
    private String userId;
    private String preparationTime;
    public Recipe() { }
    public Recipe(String recipeId,
                  String title,
                  String description,
                  Blob imageData,
                  String category,
                  String username,
                  String userId,String preparationTime) {

        this.recipeId = recipeId;
        this.title = title;
        this.description = description;
        this.imageData = imageData;
        this.category = category;
        this.username = username;
        this.userId = userId;
        this.preparationTime = preparationTime;
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
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public Blob getImageData() {
        return imageData;
    }
    public void setImageData(Blob imageData) {
        this.imageData = imageData;
    }
    public String getCategory() {
        return category;
    }
    public void setCategory(String category) {
        this.category = category;
    }
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }
    public String getPreparationTime() {
        return preparationTime;
    }
    public void setPreparationTime(String preparationTime) {
        this.preparationTime = preparationTime;
    }
}
