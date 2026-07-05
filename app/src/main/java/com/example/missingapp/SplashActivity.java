package com.example.missingapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        EdgeToEdge.enable(this, 
            SystemBarStyle.dark(Color.parseColor("#151E29")), 
            SystemBarStyle.light(Color.WHITE, Color.WHITE));
            
        setContentView(R.layout.activity_splash);

        View mainView = findViewById(R.id.main);

        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                checkUserRoleAndNavigate(user);
            } else {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                finish();
            }
        }, 3000);
    }

    private void checkUserRoleAndNavigate(FirebaseUser user) {
        Log.d(TAG, "Checking role for user: " + user.getUid());
        
        // Try to fetch from server first
        FirebaseFirestore.getInstance().collection("users").document(user.getUid()).get(Source.SERVER)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        navigateBasedOnResult(task.getResult());
                    } else {
                        // If server fetch fails, try cache as a fallback for offline/intermittent issues
                        Log.w(TAG, "Server fetch failed, trying cache...", task.getException());
                        FirebaseFirestore.getInstance().collection("users").document(user.getUid()).get(Source.CACHE)
                                .addOnCompleteListener(cacheTask -> {
                                    if (cacheTask.isSuccessful() && cacheTask.getResult() != null && cacheTask.getResult().exists()) {
                                        navigateBasedOnResult(cacheTask.getResult());
                                    } else {
                                        Log.e(TAG, "Cache fetch also failed", cacheTask.getException());
                                        // If both fail, the user might be truly offline or there's a permission issue
                                        // Redirect to login to refresh state
                                        Toast.makeText(this, "Connection error. Please log in again.", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(SplashActivity.this, Signinpage.class));
                                        finish();
                                    }
                                });
                    }
                });
    }

    private void navigateBasedOnResult(com.google.firebase.firestore.DocumentSnapshot doc) {
        Boolean isComplete = doc.getBoolean("isProfileComplete");
        String role = doc.getString("role");

        if (isComplete != null && isComplete) {
            if ("lecturer".equals(role)) {
                startActivity(new Intent(SplashActivity.this, LecturerHomeActivity.class));
            } else {
                startActivity(new Intent(SplashActivity.this, Homepage.class));
            }
        } else {
            startActivity(new Intent(SplashActivity.this, RegisterInfoActivity.class));
        }
        finish();
    }
}