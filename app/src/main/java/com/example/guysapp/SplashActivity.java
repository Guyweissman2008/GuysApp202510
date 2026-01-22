package com.example.guysapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // הסתרת ה-ActionBar העליון כדי שיהיה מסך מלא ויפה
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // טיימר של 3 שניות (3000 מילי-שניות)
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // בדיקה חכמה: לאן להעביר את המשתמש?
                if (FBRef.mAuth.getCurrentUser() != null) {
                    // אם המשתמש כבר מחובר - ישר למסך הבית
                    startActivity(new Intent(SplashActivity.this, HomeActivity.class));
                } else {
                    // אם לא מחובר - למסך ההתחברות
                    // (אם שם המסך שלך אחר, למשל MainActivity, שנית את השם כאן)
                    startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                }

                // סגירת מסך הפתיחה כדי שלא יחזרו אליו בלחיצה על Back
                finish();
            }
        }, 3000);
    }
}