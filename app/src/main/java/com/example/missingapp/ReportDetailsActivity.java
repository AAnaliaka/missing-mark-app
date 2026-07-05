package com.example.missingapp;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;

public class ReportDetailsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String requestId;
    private ListenerRegistration requestListener;
    
    private TextView studentName, studentReg, courseInfo, semesterInfo, descriptionText, statusLabel;
    private Button receiveBtn, investigateBtn, approveBtn, rejectBtn, updateMarkBtn, completeBtn;
    private LinearLayout decisionContainer;
    private View gradeInputLayout;
    private EditText gradeEditText;

    private String studentUid, unitCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_report_details);

        db = FirebaseFirestore.getInstance();
        requestId = getIntent().getStringExtra("requestId");

        initViews();
        setupListeners();
        listenToRequest();
    }

    private void initViews() {
        studentName = findViewById(R.id.studentName);
        studentReg = findViewById(R.id.studentReg);
        courseInfo = findViewById(R.id.courseInfo);
        semesterInfo = findViewById(R.id.semesterInfo);
        descriptionText = findViewById(R.id.descriptionText);
        statusLabel = findViewById(R.id.statusLabel);

        receiveBtn = findViewById(R.id.receiveBtn);
        investigateBtn = findViewById(R.id.investigateBtn);
        decisionContainer = findViewById(R.id.decisionContainer);
        approveBtn = findViewById(R.id.approveBtn);
        rejectBtn = findViewById(R.id.rejectBtn);
        gradeInputLayout = findViewById(R.id.gradeInputLayout);
        gradeEditText = findViewById(R.id.gradeEditText);
        updateMarkBtn = findViewById(R.id.updateMarkBtn);
        completeBtn = findViewById(R.id.completeBtn);

        findViewById(R.id.backBtn).setOnClickListener(v -> finish());
    }

    private void setupListeners() {
        receiveBtn.setOnClickListener(v -> updateStatus("RECEIVED", "Request received by lecturer."));
        investigateBtn.setOnClickListener(v -> updateStatus("UNDER_INVESTIGATION", "Investigation started."));
        approveBtn.setOnClickListener(v -> updateStatus("APPROVED", "Request approved. Waiting for mark update."));
        
        rejectBtn.setOnClickListener(v -> showRejectionDialog());
        
        updateMarkBtn.setOnClickListener(v -> handleUpdateMark());
        completeBtn.setOnClickListener(v -> updateStatus("COMPLETED", "Missing mark resolved and request completed."));
    }

    private void showRejectionDialog() {
        final EditText input = new EditText(this);
        input.setHint("Enter reason for rejection (e.g. Script not found)");
        
        new AlertDialog.Builder(this)
            .setTitle("Reject Request")
            .setMessage("Please provide a reason for rejecting this missing mark report.")
            .setView(input)
            .setPositiveButton("REJECT", (dialog, which) -> {
                String reason = input.getText().toString().trim();
                if (TextUtils.isEmpty(reason)) {
                    Toast.makeText(this, "Reason is required to reject", Toast.LENGTH_SHORT).show();
                    return;
                }
                updateStatus("REJECTED", "Rejected: " + reason);
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    private void listenToRequest() {
        if (requestId == null) return;

        requestListener = db.collection("missingMarkRequests").document(requestId)
                .addSnapshotListener((doc, error) -> {
                    if (error != null || doc == null || !doc.exists()) return;

                    studentUid = doc.getString("studentUid");
                    unitCode = doc.getString("unitCode");

                    studentName.setText(doc.getString("studentName"));
                    studentReg.setText(doc.getString("regNo"));
                    courseInfo.setText(unitCode + " " + doc.getString("unitName"));
                    semesterInfo.setText(doc.getString("semester"));
                    descriptionText.setText(doc.getString("description"));
                    
                    String status = doc.getString("status");
                    statusLabel.setText(status);
                    updateUIBasedOnStatus(status);
                });
    }

    private void updateUIBasedOnStatus(String status) {
        receiveBtn.setVisibility(View.GONE);
        investigateBtn.setVisibility(View.GONE);
        decisionContainer.setVisibility(View.GONE);
        gradeInputLayout.setVisibility(View.GONE);
        updateMarkBtn.setVisibility(View.GONE);
        completeBtn.setVisibility(View.GONE);

        if ("SUBMITTED".equals(status)) {
            receiveBtn.setVisibility(View.VISIBLE);
        } else if ("RECEIVED".equals(status)) {
            investigateBtn.setVisibility(View.VISIBLE);
        } else if ("UNDER_INVESTIGATION".equals(status)) {
            decisionContainer.setVisibility(View.VISIBLE);
        } else if ("APPROVED".equals(status)) {
            gradeInputLayout.setVisibility(View.VISIBLE);
            updateMarkBtn.setVisibility(View.VISIBLE);
        } else if ("MARK_UPDATED".equals(status)) {
            completeBtn.setVisibility(View.VISIBLE);
        }
    }

    private void updateStatus(String newStatus, String timelineMsg) {
        if (studentUid == null) return;

        RequestLifecycleManager.updateRequestStatus(db, requestId, studentUid, unitCode, newStatus, timelineMsg, "Lecturer", new RequestLifecycleManager.LifecycleCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(ReportDetailsActivity.this, "Status updated to " + newStatus, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(ReportDetailsActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleUpdateMark() {
        String newGrade = gradeEditText.getText().toString().trim();
        if (TextUtils.isEmpty(newGrade)) {
            gradeEditText.setError("Required");
            return;
        }

        if (studentUid == null || unitCode == null) return;

        WriteBatch batch = db.batch();
        
        DocumentReference resultRef = db.collection("results").document(studentUid + "_" + unitCode);
        batch.update(resultRef, "grade", newGrade);

        DocumentReference reqRef = db.collection("missingMarkRequests").document(requestId);
        batch.update(reqRef, "status", "MARK_UPDATED");

        Map<String, Object> event = new HashMap<>();
        event.put("requestId", requestId);
        event.put("status", "MARK_UPDATED");
        event.put("timestamp", FieldValue.serverTimestamp());
        event.put("message", "Marks updated to " + newGrade);
        event.put("responsibleUser", "Lecturer");
        batch.set(db.collection("timelineEvents").document(), event);

        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", studentUid);
        notification.put("title", "Marks Resolved!");
        notification.put("message", "Your grade for " + unitCode + " has been updated to " + newGrade);
        notification.put("timestamp", FieldValue.serverTimestamp());
        notification.put("read", false);
        notification.put("type", "MARK_UPDATED");
        batch.set(db.collection("notifications").document(), notification);

        batch.commit().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Mark updated successfully", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (requestListener != null) requestListener.remove();
    }
}