package com.example.missingapp;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TrackMissingActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private LinearLayout timelineContainer;
    private TextView unitTitle, statusSummary;
    private String studentUid;
    private ListenerRegistration requestsListener, timelineListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_track_missing);

        db = FirebaseFirestore.getInstance();
        studentUid = FirebaseAuth.getInstance().getUid();

        View mainView = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            findViewById(R.id.backBtn).setPadding(findViewById(R.id.backBtn).getPaddingLeft(), systemBars.top, findViewById(R.id.backBtn).getPaddingRight(), findViewById(R.id.backBtn).getPaddingBottom());
            return insets;
        });

        findViewById(R.id.backBtn).setOnClickListener(v -> finish());

        unitTitle = findViewById(R.id.unitTitle);
        statusSummary = findViewById(R.id.statusSummary);
        timelineContainer = findViewById(R.id.timelineContainer);

        listenToRequests();
    }

    private void listenToRequests() {
        if (studentUid == null) return;

        requestsListener = db.collection("missingMarkRequests")
                .whereEqualTo("studentUid", studentUid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("TrackMissing", "Error listening to requests", error);
                        return;
                    }
                    
                    if (value == null || value.isEmpty()) {
                        unitTitle.setText("No Active Requests");
                        statusSummary.setText("You haven't reported any missing marks yet.");
                        return;
                    }

                    DocumentSnapshot doc = value.getDocuments().get(0);
                    String requestId = doc.getId();
                    String status = doc.getString("status");
                    unitTitle.setText(doc.getString("unitCode") + ": " + doc.getString("unitName"));
                    statusSummary.setText("Current Status: " + status);

                    listenToTimeline(requestId);
                });
    }

    private void listenToTimeline(String requestId) {
        if (timelineListener != null) timelineListener.remove();

        timelineListener = db.collection("timelineEvents")
                .whereEqualTo("requestId", requestId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    timelineContainer.removeAllViews();
                    LayoutInflater inflater = LayoutInflater.from(this);

                    int count = value.size();
                    for (int i = 0; i < count; i++) {
                        DocumentSnapshot doc = value.getDocuments().get(i);
                        View view = inflater.inflate(R.layout.item_timeline, timelineContainer, false);

                        TextView title = view.findViewById(R.id.statusTitle);
                        TextView message = view.findViewById(R.id.statusMessage);
                        TextView time = view.findViewById(R.id.statusTime);
                        TextView responsible = view.findViewById(R.id.responsibleUser);
                        View line = view.findViewById(R.id.line);

                        String statusText = doc.getString("status");
                        title.setText(statusText);
                        message.setText(doc.getString("message"));
                        responsible.setText("By: " + doc.getString("responsibleUser"));

                        Date date = doc.getDate("timestamp");
                        if (date != null) {
                            time.setText(new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(date));
                        } else {
                            time.setText("Just now");
                        }

                        if ("REJECTED".equals(statusText)) {
                            title.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
                        } else if ("COMPLETED".equals(statusText) || "MARK_UPDATED".equals(statusText)) {
                            title.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
                        }

                        if (i == count - 1) {
                            line.setVisibility(View.GONE);
                        }

                        timelineContainer.addView(view);
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (requestsListener != null) requestsListener.remove();
        if (timelineListener != null) timelineListener.remove();
    }
}