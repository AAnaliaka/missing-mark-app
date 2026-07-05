package com.example.missingapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.backBtn).setOnClickListener(v -> finish());
        
        findViewById(R.id.logoutActionBtn).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        TextView nameTxt = findViewById(R.id.profileName);
        TextView emailTxt = findViewById(R.id.profileEmail);
        TextView regNoTxt = findViewById(R.id.profileRegNo);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            nameTxt.setText(user.getDisplayName() != null ? user.getDisplayName() : "Student User");
            emailTxt.setText(user.getEmail());
            regNoTxt.setText("MMUST/001/2024"); 
        } else {
            nameTxt.setText("Developer Mode");
            emailTxt.setText("dev@gmail.com");
            regNoTxt.setText("DEV/2024/999");
        }

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.navigation_profile);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.navigation_home) {
                    startActivity(new Intent(this, Homepage.class));
                    return true;
                } else if (id == R.id.navigation_courses) {
                    startActivity(new Intent(this, CoursesActivity.class));
                    return true;
                } else if (id == R.id.navigation_alerts) {
                    startActivity(new Intent(this, AlertsActivity.class));
                    return true;
                }
                return id == R.id.navigation_profile;
            });
        }
    }
}