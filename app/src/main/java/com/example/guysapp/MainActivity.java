package com.example.guysapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // קביעת פדינג עבור מערכת הסורגים
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // בדיקה אם המשתמש מחובר
        checkUserStatus();
    }

    private void checkUserStatus() {
        FirebaseUser user = FBRef.mAuth.getCurrentUser();
        if (user != null) {
            // אם המשתמש מחובר, הצג מסך ראשי או עשה פעולה כלשהי
            Toast.makeText(this, "Welcome back, " + user.getEmail(), Toast.LENGTH_SHORT).show();
            // למשל, נעבור למסך הבית
            startActivity(new Intent(MainActivity.this, HomeActivity.class));
            finish();  // סיום המסך הנוכחי
        } else {
            // אם המשתמש לא מחובר, נוודא שהוא יעבור למסך התחברות
            Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(loginIntent);
            finish();  // סיום המסך הנוכחי
        }
    }
}
