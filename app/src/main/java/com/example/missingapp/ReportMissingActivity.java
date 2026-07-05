package com.example.missingapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ReportMissingActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String studentRegNo;
    private String studentName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_report_missing);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        View mainView = findViewById(R.id.main);
        View backBtn = findViewById(R.id.backBtn);

        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            backBtn.setPadding(backBtn.getPaddingLeft(), systemBars.top, backBtn.getPaddingRight(), backBtn.getPaddingBottom());
            return insets;
        });

        backBtn.setOnClickListener(v -> finish());

        EditText courseName = findViewById(R.id.courseName);
        EditText courseCode = findViewById(R.id.courseCode);
        EditText yearStudy = findViewById(R.id.yearStudy);
        EditText otherInfo = findViewById(R.id.otherInfo);
        Button submitBtn = findViewById(R.id.submitReport);

        String code = getIntent().getStringExtra("courseCode");
        String name = getIntent().getStringExtra("courseName");
        if (code != null) courseCode.setText(code);
        if (name != null) courseName.setText(name);

        fetchStudentInfo();

        submitBtn.setOnClickListener(v -> {
            String cName = courseName.getText().toString().trim();
            String cCode = courseCode.getText().toString().trim();
            String ySem = yearStudy.getText().toString().trim();
            String description = otherInfo.getText().toString().trim();

            if (TextUtils.isEmpty(cName)) { courseName.setError("Required"); return; }
            if (TextUtils.isEmpty(cCode)) { courseCode.setError("Required"); return; }
            if (TextUtils.isEmpty(ySem)) { yearStudy.setError("Required"); return; }
            if (TextUtils.isEmpty(description)) { otherInfo.setError("Required"); return; }

            submitReport(cName, cCode, ySem, description);
        });
    }

    private void fetchStudentInfo() {
        String uid = mAuth.getUid();
        if (uid == null) return;

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        studentRegNo = doc.getString("regNo");
                        studentName = doc.getString("email"); 
                    }
                });
    }

    private void submitReport(String cName, String cCode, String ySem, String description) {
        String uid = mAuth.getUid();
        if (uid == null) return;

        // LOOPHOLE FIX: Prevent duplicate pending reports for the same unit
        db.collection("missingMarkRequests")
                .whereEqualTo("studentUid", uid)
                .whereEqualTo("unitCode", cCode)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    boolean hasActive = false;
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        String status = doc.getString("status");
                        if (!"COMPLETED".equals(status) && !"REJECTED".equals(status)) {
                            hasActive = true;
                            break;
                        }
                    }
                    
                    if (hasActive) {
                        Toast.makeText(this, "A report for this unit is already in progress.", Toast.LENGTH_LONG).show();
                    } else {
                        proceedWithSubmission(cName, cCode, ySem, description, uid);
                    }
                });
    }

    private void proceedWithSubmission(String cName, String cCode, String ySem, String description, String uid) {
        String requestId = UUID.randomUUID().toString();
        WriteBatch batch = db.batch();

        Map<String, Object> request = new HashMap<>();
        request.put("requestId", requestId);
        request.put("studentUid", uid);
        request.put("studentName", studentName);
        request.put("regNo", studentRegNo);
        request.put("unitCode", cCode);
        request.put("unitName", cName);
        request.put("semester", ySem);
        request.put("description", description);
        request.put("status", "SUBMITTED");
        request.put("timestamp", FieldValue.serverTimestamp());

        batch.set(db.collection("missingMarkRequests").document(requestId), request);

        Map<String, Object> event = new HashMap<>();
        event.put("requestId", requestId);
        event.put("status", "SUBMITTED");
        event.put("timestamp", FieldValue.serverTimestamp());
        event.put("message", "Request submitted by student.");
        event.put("responsibleUser", studentName);

        batch.set(db.collection("timelineEvents").document(), event);

        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", uid);
        notification.put("title", "Request Submitted");
        notification.put("message", "Your missing mark request for " + cCode + " has been submitted.");
        notification.put("timestamp", FieldValue.serverTimestamp());
        notification.put("read", false);

        batch.set(db.collection("notifications").document(), notification);

        batch.commit().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Report submitted successfully", Toast.LENGTH_SHORT).show();
                NotificationHelper.showNotification(this, "Report Filed", 
                    "Missing mark report for " + cCode + " has been submitted.", 
                    Homepage.class);
                finish();
            } else {
                Toast.makeText(this, "Error submitting report: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}