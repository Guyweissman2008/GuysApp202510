package com.example.guysapp;

import java.util.List;

public class Recipe {
    private String title;
    private String description;
    private List<Integer> imageData; // תמונה כ-List<Integer>

    public Recipe() {
        // דרוש ל-Firestore
    }

    public Recipe(String title, String description, List<Integer> imageData) {
        this.title = title;
        this.description = description;
        this.imageData = imageData;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<Integer> getImageData() { return imageData; }
    public void setImageData(List<Integer> imageData) { this.imageData = imageData; }
}
