package com.example.guysapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class HomeActivity extends AppCompatActivity {

    private Button buttonLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Find the logout button
        buttonLogout = findViewById(R.id.button_logout);

        // Set the onClickListener for the logout button
        buttonLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logoutUser();
            }
        });
    }

    private void logoutUser() {
        // Sign out the user
        FBRef.mAuth.signOut();

        // Show a toast message
        Toast.makeText(HomeActivity.this, "You have been logged out", Toast.LENGTH_SHORT).show();

        // Redirect to the Login screen (or wherever you need to go after logout)
        Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
        startActivity(intent);
        finish(); // Finish the current activity (optional, but it avoids going back to this activity after logout)
    }
}
