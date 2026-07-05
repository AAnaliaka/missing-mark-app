package com.example.missingapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class LecturerHomeActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private RecyclerView rv;
    private ReportsAdapter adapter;
    private List<MissingMarkReport> reportsList = new ArrayList<>();
    private ListenerRegistration registration, systemNotificationListener;
    private String currentStatus = "SUBMITTED";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_lecturer_home);

        db = FirebaseFirestore.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.logoutBtn).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        rv = findViewById(R.id.reportsRecyclerView);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReportsAdapter(reportsList);
        rv.setAdapter(adapter);

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentStatus = tab.getText().toString();
                if ("INVESTIGATING".equals(currentStatus)) currentStatus = "UNDER_INVESTIGATION";
                listenToReports();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        listenToReports();
        startLecturerNotificationListener();
    }

    private void startLecturerNotificationListener() {
        // Listen for new SUBMITTED reports to notify lecturer even if they are on another tab
        systemNotificationListener = db.collection("missingMarkRequests")
                .whereEqualTo("status", "SUBMITTED")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null && !value.isEmpty()) {
                        for (DocumentChange dc : value.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                String name = dc.getDocument().getString("studentName");
                                String unit = dc.getDocument().getString("unitCode");
                                
                                NotificationHelper.showNotification(this, "New Request: " + unit, 
                                        "New missing mark report submitted by " + name, 
                                        LecturerHomeActivity.class);
                            }
                        }
                    }
                });
    }

    private void listenToReports() {
        if (registration != null) registration.remove();

        registration = db.collection("missingMarkRequests")
                .whereEqualTo("status", currentStatus)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        String msg = error.getMessage() != null ? error.getMessage() : "Unknown error";
                        Log.e("LecturerHome", "Query failed: " + msg);
                        if (msg.contains("index")) {
                            Toast.makeText(this, "Database indexing in progress...", Toast.LENGTH_LONG).show();
                        }
                        return;
                    }
                    if (value != null) {
                        reportsList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            reportsList.add(new MissingMarkReport(
                                    doc.getId(),
                                    doc.getString("studentName"),
                                    doc.getString("regNo"),
                                    doc.getString("unitCode"),
                                    doc.getString("unitName"),
                                    doc.getString("status")
                            ));
                        }
                        adapter.notifyDataSetChanged();
                        
                        // Toggle Empty State
                        View empty = findViewById(R.id.emptyState);
                        if (empty != null) {
                            empty.setVisibility(reportsList.isEmpty() ? View.VISIBLE : View.GONE);
                        }
                    }
                });
    }

    private static class MissingMarkReport {
        String id, studentName, regNo, courseCode, courseName, status;
        MissingMarkReport(String id, String n, String r, String cc, String cn, String s) {
            this.id = id; studentName = n; regNo = r; courseCode = cc; courseName = cn; status = s;
        }
    }

    private class ReportsAdapter extends RecyclerView.Adapter<ReportViewHolder> {
        List<MissingMarkReport> list;
        ReportsAdapter(List<MissingMarkReport> l) { list = l; }

        @NonNull
        @Override
        public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lecturer_report, parent, false);
            return new ReportViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
            MissingMarkReport item = list.get(position);
            holder.name.setText(item.studentName);
            holder.regNo.setText(item.regNo);
            holder.course.setText(item.courseCode + ": " + item.courseName);
            holder.status.setText(item.status);

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(LecturerHomeActivity.this, ReportDetailsActivity.class);
                intent.putExtra("requestId", item.id);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() { return list.size(); }
    }

    private static class ReportViewHolder extends RecyclerView.ViewHolder {
        TextView name, regNo, course, status;
        ReportViewHolder(View v) {
            super(v);
            name = v.findViewById(R.id.studentName);
            regNo = v.findViewById(R.id.regNo);
            course = v.findViewById(R.id.courseTitle);
            status = v.findViewById(R.id.statusLabel);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (registration != null) registration.remove();
        if (systemNotificationListener != null) systemNotificationListener.remove();
    }
}