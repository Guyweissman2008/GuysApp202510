package com.example.guysapp;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class FBRef {
    public static final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    public static final FirebaseFirestore FBFS = FirebaseFirestore.getInstance();
    public static final CollectionReference refRecipes = FBFS.collection("Recipes");
    public static final CollectionReference refSavedRecipes  = FBFS.collection("SavedRecipes");
    public static final CollectionReference refUsers = FBFS.collection("Users");
}
