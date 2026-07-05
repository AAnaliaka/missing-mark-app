package com.example.missingapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

public class Homepage extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private ListenerRegistration notificationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_base);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        View mainView = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        bottomNav = findViewById(R.id.bottomNavigation);
        if (bottomNav != null) {
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.navigation_home) loadFragment(new HomeFragment());
                else if (id == R.id.navigation_courses) loadFragment(new CoursesFragment());
                else if (id == R.id.navigation_alerts) loadFragment(new AlertsFragment());
                else if (id == R.id.navigation_profile) loadFragment(new ProfileFragment());
                return true;
            });

            if (savedInstanceState == null) {
                loadFragment(new HomeFragment());
            }
        }

        startSystemNotificationListener(currentUser.getUid());
    }

    private void startSystemNotificationListener(String uid) {
        // Listen for new alerts to show system-wide notifications
        notificationListener = FirebaseFirestore.getInstance().collection("notifications")
                .whereEqualTo("userId", uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null && !value.isEmpty()) {
                        for (DocumentChange dc : value.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                // Trigger real system notification
                                String title = dc.getDocument().getString("title");
                                String message = dc.getDocument().getString("message");
                                
                                // Only notify if it's recent (not loading old ones)
                                NotificationHelper.showNotification(this, title, message, Homepage.class);
                            }
                        }
                    }
                });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    public void switchToTab(int menuId) {
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(menuId);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notificationListener != null) notificationListener.remove();
    }
}