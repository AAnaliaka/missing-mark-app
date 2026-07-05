package com.example.missingapp;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;

public class RequestLifecycleManager {

    public interface LifecycleCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public static void updateRequestStatus(FirebaseFirestore db, String requestId, String studentUid, 
                                          String unitCode, String newStatus, String message, 
                                          String responsibleUser, LifecycleCallback callback) {
        
        WriteBatch batch = db.batch();
        DocumentReference reqRef = db.collection("missingMarkRequests").document(requestId);
        
        // 1. Update Request Status
        batch.update(reqRef, "status", newStatus);

        // 2. Create Timeline Event
        Map<String, Object> event = new HashMap<>();
        event.put("requestId", requestId);
        event.put("status", newStatus);
        event.put("timestamp", FieldValue.serverTimestamp());
        event.put("message", message);
        event.put("responsibleUser", responsibleUser);
        batch.set(db.collection("timelineEvents").document(), event);

        // 3. Create Alert for Student (This triggers the Real-time Phone Notification)
        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", studentUid);
        notification.put("title", "Update for " + unitCode);
        notification.put("message", message);
        notification.put("timestamp", FieldValue.serverTimestamp());
        notification.put("read", false);
        notification.put("type", "STATUS_CHANGE");
        batch.set(db.collection("notifications").document(), notification);

        batch.commit().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                callback.onSuccess();
            } else {
                callback.onFailure(task.getException());
            }
        });
    }
}