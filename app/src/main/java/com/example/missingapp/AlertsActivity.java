package com.example.missingapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

public class AlertsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_alerts);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.backBtn).setOnClickListener(v -> finish());

        RecyclerView rv = findViewById(R.id.alertsRecyclerView);
        rv.setLayoutManager(new LinearLayoutManager(this));
        
        List<DataManager.Alert> alerts = DataManager.getAlerts();
        rv.setAdapter(new AlertsAdapter(alerts));

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.navigation_alerts);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.navigation_home) {
                    startActivity(new Intent(this, Homepage.class));
                    return true;
                } else if (id == R.id.navigation_courses) {
                    startActivity(new Intent(this, CoursesActivity.class));
                    return true;
                } else if (id == R.id.navigation_profile) {
                    startActivity(new Intent(this, ProfileActivity.class));
                    return true;
                }
                return id == R.id.navigation_alerts;
            });
        }
    }

    private class AlertsAdapter extends RecyclerView.Adapter<AlertViewHolder> {
        List<DataManager.Alert> list;
        AlertsAdapter(List<DataManager.Alert> l) { list = l; }

        @NonNull
        @Override
        public AlertViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alert, parent, false);
            return new AlertViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull AlertViewHolder holder, int position) {
            DataManager.Alert item = list.get(position);
            holder.title.setText(item.title);
            holder.message.setText(item.message);
            holder.time.setText(item.time);
        }

        @Override
        public int getItemCount() { return list.size(); }
    }

    private static class AlertViewHolder extends RecyclerView.ViewHolder {
        TextView title, message, time;
        AlertViewHolder(View v) {
            super(v);
            title = v.findViewById(R.id.alertTitle);
            message = v.findViewById(R.id.alertMessage);
            time = v.findViewById(R.id.alertTime);
        }
    }
}