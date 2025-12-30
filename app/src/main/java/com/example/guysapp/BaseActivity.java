package com.example.guysapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class BaseActivity extends AppCompatActivity {

    protected BottomNavigationView bottomNav;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void setupBottomNavigation(int selectedItemId) {

        bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav == null) {
            return;
        }

        // להציג טקסט + אייקון
        bottomNav.setLabelVisibilityMode(NavigationBarView.LABEL_VISIBILITY_LABELED);

        updateTitleBySelectedItem(selectedItemId);
        bottomNav.setSelectedItemId(selectedItemId);

        bottomNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.nav_home && selectedItemId != R.id.nav_home) {
                    navigateTo(HomeActivity.class);
                    return true;

                } else if (id == R.id.nav_add && selectedItemId != R.id.nav_add) {
                    navigateTo(AddRecipeActivity.class);
                    return true;

                } else if (id == R.id.nav_profile && selectedItemId != R.id.nav_profile) {
                    navigateTo(ProfileActivity.class);
                    return true;

                } else if (id == R.id.nav_logout) {
                    logoutUser();
                    return true;
                }

                return false;
            }
        });
    }

    private void updateTitleBySelectedItem(int selectedItemId) {
        if (selectedItemId == R.id.nav_home) {
            setTitle("בית");
        } else if (selectedItemId == R.id.nav_add) {
            setTitle("הוסף");
        } else if (selectedItemId == R.id.nav_profile) {
            setTitle("פרופיל");
        } else if (selectedItemId == R.id.nav_logout) {
            setTitle("התנתק");
        }
    }

    private void navigateTo(Class<?> destination) {
        startActivity(new Intent(this, destination));
        overridePendingTransition(0, 0);
        finish();
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
