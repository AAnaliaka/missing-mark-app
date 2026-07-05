package com.example.missingapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class ViewResultsActivity extends AppCompatActivity {

    private Button reportBtn;
    private List<ResultItem> results = new ArrayList<>();
    private int selectedPosition = -1;
    private IntResultsAdapter adapter;
    private FirebaseFirestore db;
    private ListenerRegistration registration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_view_results);

        db = FirebaseFirestore.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.backBtn).setOnClickListener(v -> finish());
        reportBtn = findViewById(R.id.reportBtnTop);

        RecyclerView rv = findViewById(R.id.resultsRecyclerView);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new IntResultsAdapter();
        rv.setAdapter(adapter);

        reportBtn.setOnClickListener(v -> {
            if (selectedPosition != -1) {
                ResultItem item = results.get(selectedPosition);
                Intent intent = new Intent(this, ReportMissingActivity.class);
                intent.putExtra("courseCode", item.code);
                intent.putExtra("courseName", item.name);
                startActivity(intent);
            }
        });

        listenToResults();
    }

    private void listenToResults() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        registration = db.collection("results")
                .whereEqualTo("studentUid", uid)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    
                    results.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        results.add(new ResultItem(
                                doc.getString("unitCode"),
                                doc.getString("unitName"),
                                doc.getString("grade")
                        ));
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private static class ResultItem {
        String code, name, grade;
        ResultItem(String c, String n, String g) {
            code = c; name = n; grade = g;
        }
    }

    private class IntResultsAdapter extends RecyclerView.Adapter<ResultViewHolder> {
        @NonNull
        @Override
        public ResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_result, parent, false);
            return new ResultViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ResultViewHolder holder, int position) {
            ResultItem item = results.get(position);
            holder.code.setText(item.code);
            holder.name.setText(item.name);
            holder.grade.setText(item.grade);

            if ("M".equals(item.grade)) {
                holder.grade.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            } else {
                holder.grade.setTextColor(getResources().getColor(R.color.primary_dark));
            }

            holder.checkIcon.setVisibility(selectedPosition == position ? View.VISIBLE : View.GONE);

            holder.itemView.setOnLongClickListener(v -> {
                int oldPos = selectedPosition;
                selectedPosition = holder.getAdapterPosition();
                
                notifyItemChanged(oldPos);
                notifyItemChanged(selectedPosition);
                
                reportBtn.setVisibility(View.VISIBLE);
                return true;
            });
        }

        @Override
        public int getItemCount() { return results.size(); }
    }

    private static class ResultViewHolder extends RecyclerView.ViewHolder {
        TextView code, name, grade;
        ImageView checkIcon;
        ResultViewHolder(View v) {
            super(v);
            code = v.findViewById(R.id.courseCode);
            name = v.findViewById(R.id.courseName);
            grade = v.findViewById(R.id.grade);
            checkIcon = v.findViewById(R.id.checkIcon);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (registration != null) registration.remove();
    }
}