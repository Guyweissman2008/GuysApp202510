package com.example.guysapp;

import java.util.List;

public class Recipe {

    private String title;
    private String description;
    private List<Integer> imageData; // תמונה כ-List<Integer>
    private String category;
    private String username;   // שם המשתמש להצגה במסך
    private String userId;     // UID של המשתמש, לצורך הרשאות מחיקה

    public Recipe() {}

    public Recipe(String title, String description, List<Integer> imageData,
                  String category, String username, String userId) {
        this.title = title;
        this.description = description;
        this.imageData = imageData;
        this.category = category;
        this.username = username;
        this.userId = userId;
    }

    // Getters & Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<Integer> getImageData() { return imageData; }
    public void setImageData(List<Integer> imageData) { this.imageData = imageData; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    // מתודה להמרת List<Integer> ל-byte[] עבור Bitmap
    public byte[] getImageDataAsBytes() {
        if (imageData == null) return null;
        byte[] bytes = new byte[imageData.size()];
        for (int i = 0; i < imageData.size(); i++) {
            bytes[i] = imageData.get(i).byteValue();
        }
        return bytes;
    }
}
