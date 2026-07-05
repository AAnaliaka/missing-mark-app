package com.example.missingapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class RegisterInfoActivity extends AppCompatActivity {

    private static final String TAG = "RegisterInfoActivity";
    private FirebaseFirestore db;
    private ProgressBar loader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register_info);

        db = FirebaseFirestore.getInstance();
        View mainView = findViewById(R.id.main);

        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            View title = findViewById(R.id.title);
            if (title != null) {
                title.setPadding(title.getPaddingLeft(), systemBars.top + 20, title.getPaddingRight(), title.getPaddingBottom());
            }
            return insets;
        });

        EditText regNoEditText = findViewById(R.id.regNoEditText);
        EditText schoolEditText = findViewById(R.id.schoolEditText);
        EditText departmentEditText = findViewById(R.id.departmentEditText);
        EditText programmeEditText = findViewById(R.id.programmeEditText);
        EditText yearEditText = findViewById(R.id.yearEditText);
        
        Button finishButton = findViewById(R.id.finishButton);
        loader = findViewById(R.id.loader);

        // EXTRA SECURITY: If a lecturer somehow lands here, redirect them immediately
        String email = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getEmail() : "";
        if ("lecturer@mmust.ac.ke".equalsIgnoreCase(email)) {
            startActivity(new Intent(this, LecturerHomeActivity.class));
            finish();
            return;
        }

        finishButton.setOnClickListener(v -> {
            String regNo = regNoEditText.getText().toString().trim();
            String school = schoolEditText.getText().toString().trim();
            String department = departmentEditText.getText().toString().trim();
            String programme = programmeEditText.getText().toString().trim();
            String year = yearEditText.getText().toString().trim();

            // SECRET PROMOTION CODE: Use this as Reg No to become a lecturer
            if ("ADMIN_LECTURER".equalsIgnoreCase(regNo)) {
                promoteToLecturer();
                return;
            }

            if (regNo.isEmpty()) { regNoEditText.setError("Required"); return; }
            if (school.isEmpty()) { schoolEditText.setError("Required"); return; }
            if (department.isEmpty()) { departmentEditText.setError("Required"); return; }
            if (programme.isEmpty()) { programmeEditText.setError("Required"); return; }
            if (year.isEmpty()) { yearEditText.setError("Required"); return; }

            saveProfile(regNo, school, department, programme, year);
        });
    }

    private void saveProfile(String regNo, String school, String department, String programme, String year) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, Signinpage.class));
            finish();
            return;
        }

        if (loader != null) loader.setVisibility(View.VISIBLE);

        Map<String, Object> profile = new HashMap<>();
        profile.put("regNo", regNo);
        profile.put("school", school);
        profile.put("department", department);
        profile.put("programme", programme);
        profile.put("yearOfStudy", year);
        profile.put("uid", uid);

        WriteBatch batch = db.batch();

        batch.set(db.collection("studentProfiles").document(uid), profile, SetOptions.merge());

        Map<String, Object> userUpdate = new HashMap<>();
        userUpdate.put("isProfileComplete", true);
        userUpdate.put("regNo", regNo);
        batch.set(db.collection("users").document(uid), userUpdate, SetOptions.merge());

        seedDummyUnits(batch, uid);

        batch.commit().addOnCompleteListener(task -> {
            if (loader != null) loader.setVisibility(View.GONE);
            if (task.isSuccessful()) {
                Toast.makeText(RegisterInfoActivity.this, "Profile completed successfully!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(RegisterInfoActivity.this, Homepage.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void seedDummyUnits(WriteBatch batch, String uid) {
        String[] allUnits = {
            "BIT 311|Mobile Application Development", "CCS 202|Operating Systems",
            "MATH 301|Calculus III", "BIT 312|Database Systems",
            "CCS 301|Software Engineering", "BIT 221|Network Management",
            "MATH 101|Discrete Mathematics", "BIT 411|Artificial Intelligence",
            "CCS 101|Introduction to Computing"
        };

        Random random = new Random();
        int numUnits = 5 + random.nextInt(3); 
        List<Integer> selectedIndices = new ArrayList<>();

        while (selectedIndices.size() < numUnits) {
            int idx = random.nextInt(allUnits.length);
            if (!selectedIndices.contains(idx)) selectedIndices.add(idx);
        }

        for (int i = 0; i < selectedIndices.size(); i++) {
            String[] parts = allUnits[selectedIndices.get(i)].split("\\|");
            String code = parts[0];
            String name = parts[1];
            String grade = (i < 2) ? "M" : getRandomGrade(random);

            Map<String, Object> result = new HashMap<>();
            result.put("unitCode", code);
            result.put("unitName", name);
            result.put("grade", grade);
            result.put("studentUid", uid);
            result.put("semester", "Jan-April 2024");

            batch.set(db.collection("results").document(uid + "_" + code), result, SetOptions.merge());
        }
    }

    private void promoteToLecturer() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        if (loader != null) loader.setVisibility(View.VISIBLE);
        Map<String, Object> updates = new HashMap<>();
        updates.put("role", "lecturer");
        updates.put("isProfileComplete", true);

        db.collection("users").document(uid).set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    if (loader != null) loader.setVisibility(View.GONE);
                    Toast.makeText(this, "Promoted to Lecturer!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, LecturerHomeActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    if (loader != null) loader.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private String getRandomGrade(Random r) {
        String[] grades = {"A", "B", "B+", "C", "C+", "D"};
        return grades[r.nextInt(grades.length)];
    }
}