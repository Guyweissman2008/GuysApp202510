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
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                FirebaseUser user = FBRef.mAuth.getCurrentUser();
                if (user != null) {
                    // ניסיון לקחת את השם המלא
                    String name = user.getDisplayName();
                    // בדיקה: אם השם ריק או לא קיים, נשתמש במייל כגיבוי
                    if (name == null || name.isEmpty()) {
                        name = user.getEmail();
                    }
                    Toast.makeText(SplashActivity.this,
                            "Welcome back, " + name,
                            Toast.LENGTH_SHORT).show();

                    startActivity(new Intent(SplashActivity.this, HomeActivity.class));
                } else {
                    startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                }
                finish();
            }
        }, 3000);
    }
}