package com.example.missingapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AlertsFragment extends Fragment {

    private FirebaseFirestore db;
    private AlertsAdapter adapter;
    private List<NotificationItem> notifications = new ArrayList<>();
    private ListenerRegistration registration;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_alerts, container, false);
        
        db = FirebaseFirestore.getInstance();
        
        View scrollContainer = view.findViewById(R.id.scrollContainer);
        if (scrollContainer != null) {
            ViewCompat.setOnApplyWindowInsetsListener(scrollContainer, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), systemBars.top + 24, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }

        RecyclerView rv = view.findViewById(R.id.alertsRecyclerView);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AlertsAdapter(notifications);
        rv.setAdapter(adapter);

        view.findViewById(R.id.clearAllBtn).setOnClickListener(v -> clearAllNotifications());

        listenToNotifications();

        return view;
    }

    private void listenToNotifications() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        registration = db.collection("notifications")
                .whereEqualTo("userId", uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    
                    notifications.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        notifications.add(new NotificationItem(
                                doc.getId(),
                                doc.getString("title"),
                                doc.getString("message"),
                                doc.getDate("timestamp"),
                                Boolean.TRUE.equals(doc.getBoolean("read"))
                        ));
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void clearAllNotifications() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        db.collection("notifications").whereEqualTo("userId", uid).get().addOnSuccessListener(queryDocumentSnapshots -> {
            WriteBatch batch = db.batch();
            for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                batch.delete(doc.getReference());
            }
            batch.commit().addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Cleared all notifications", Toast.LENGTH_SHORT).show());
        });
    }

    private static class NotificationItem {
        String id, title, message;
        Date timestamp;
        boolean read;
        NotificationItem(String id, String t, String m, Date ts, boolean r) {
            this.id = id; title = t; message = m; timestamp = ts; read = r;
        }
    }

    private class AlertsAdapter extends RecyclerView.Adapter<AlertViewHolder> {
        List<NotificationItem> list;
        AlertsAdapter(List<NotificationItem> l) { list = l; }

        @NonNull
        @Override
        public AlertViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alert, parent, false);
            return new AlertViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull AlertViewHolder holder, int position) {
            NotificationItem item = list.get(position);
            holder.title.setText(item.title);
            holder.message.setText(item.message);
            
            if (item.timestamp != null) {
                holder.time.setText(new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(item.timestamp));
            } else {
                holder.time.setText("Just now");
            }

            // Visual indicator for read/unread
            holder.itemView.setAlpha(item.read ? 0.6f : 1.0f);
            
            holder.itemView.setOnClickListener(v -> {
                if (!item.read) {
                    db.collection("notifications").document(item.id).update("read", true);
                }
            });
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (registration != null) registration.remove();
    }
}