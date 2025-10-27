package com.example.guysapp;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class FBRef {

    public static FirebaseAuth mAuth = FirebaseAuth.getInstance();
    public static FirebaseFirestore FBFS = FirebaseFirestore.getInstance();
    public static CollectionReference refImages = FBFS.collection("Images");
    public static CollectionReference refUsers = FBFS.collection("Users");
}
