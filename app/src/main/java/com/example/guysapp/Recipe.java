package com.example.guysapp;

import java.util.List;

public class Recipe {

    private String recipeId;
    private String title;
    private String description;
    private String category;
    private List<Integer> imageData;
    private String username;
    private String userId;

    // חובה ל-Firestore
    public Recipe() { }

    public Recipe(String recipeId,
                  String title,
                  String description,
                  List<Integer> imageData,
                  String category,
                  String username,
                  String userId) {

        this.recipeId = recipeId;
        this.title = title;
        this.description = description;
        this.imageData = imageData;
        this.category = category;
        this.username = username;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Integer> getImageData() {
        return imageData;
    }

    public void setImageData(List<Integer> imageData) {
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

    // המרת List<Integer> ל-byte[] עבור Bitmap
    public byte[] imageDataToBytes() {
        if (imageData == null) {
            return null;
        }

        byte[] bytes = new byte[imageData.size()];

        for (int i = 0; i < imageData.size(); i++) {
            Integer val = imageData.get(i);
            bytes[i] = (val != null) ? val.byteValue() : 0;
        }

        return bytes;
    }
}
