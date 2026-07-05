package com.example.missingapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Lecturerhomepage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_lecturerhomepage);
        
        View root = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        LinearLayout container = findViewById(R.id.studentListContainer);
        if (container != null) {
            String[] names = {"Kevin Wesonga", "Turphena Ochieng", "Linda Irangi"};
            String[] details = {"CS201 • 3 marks missing", "BIT 112 • 1 mark missing", "CS305 • 2 marks missing"};

            for (int i = 0; i < names.length; i++) {
                View item = LayoutInflater.from(this).inflate(R.layout.item_lecturer_flagged, container, false);
                ((TextView)item.findViewById(R.id.studentName)).setText(names[i]);
                ((TextView)item.findViewById(R.id.incidentDesc)).setText(details[i]);
                container.addView(item);
            }
        }
    }
}