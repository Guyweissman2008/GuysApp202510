package com.example.guysapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // טיימר של 3 שניות (3000 מילי-שניות)
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                FirebaseUser user = FBRef.mAuth.getCurrentUser();
                // בדיקה : לאן להעביר את המשתמש?
                if (user != null) {
                    Toast.makeText(SplashActivity.this,
                            "כיף שחזרת, " + user.getEmail(),
                            Toast.LENGTH_SHORT).show();
                    // אם המשתמש כבר מחובר - ישר למסך הבית
                    startActivity(new Intent(SplashActivity.this, HomeActivity.class));
                } else {
                    // אם לא מחובר - למסך ההתחברות
                    startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                }

                // סגירת מסך הפתיחה כדי שלא יחזרו אליו בלחיצה על Back
                finish();
            }
        }, 3000);
    }
}