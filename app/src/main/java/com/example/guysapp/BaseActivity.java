package com.example.guysapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class BaseActivity extends AppCompatActivity {

    protected BottomNavigationView bottomNav;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // כאן אין setContentView – כל Activity יגדיר את ה-layout שלו
    }

    protected void setupBottomNavigation(int selectedItemId) {
        bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav == null) return;

        bottomNav.setSelectedItemId(selectedItemId);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home && selectedItemId != R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                overridePendingTransition(0,0);
                finish();
                return true;

            } else if (id == R.id.nav_add && selectedItemId != R.id.nav_add) {
                startActivity(new Intent(this, AddRecipeActivity.class));
                overridePendingTransition(0,0);
                finish();
                return true;

            } else if (id == R.id.nav_profile && selectedItemId != R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                overridePendingTransition(0,0);
                finish();
                return true;

            } else if (id == R.id.nav_logout) {
                logoutUser();
                return true;
            }

            return false;
        });
    }

    private void logoutUser() {
        FBRef.mAuth.signOut();
        Toast.makeText(this, "התנתקת בהצלחה", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        finish();
    }
}

