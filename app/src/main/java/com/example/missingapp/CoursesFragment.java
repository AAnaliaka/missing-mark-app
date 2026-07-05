package com.example.missingapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class CoursesFragment extends Fragment {
    
    private FirebaseFirestore db;
    private LinearLayout unitsContainer;
    private ListenerRegistration registration;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_courses, container, false);
        
        db = FirebaseFirestore.getInstance();
        
        View nav = view.findViewById(R.id.bottomNavigation);
        if (nav != null) nav.setVisibility(View.GONE);
        View back = view.findViewById(R.id.backBtn);
        if (back != null) back.setVisibility(View.GONE);

        unitsContainer = view.findViewById(R.id.coursesRecyclerViewContainer); 
        
        listenToCourses(inflater);

        return view;
    }

    private void listenToCourses(LayoutInflater inflater) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        registration = db.collection("results")
                .whereEqualTo("studentUid", uid)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    
                    unitsContainer.removeAllViews();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        View card = inflater.inflate(R.layout.item_course_simple, unitsContainer, false);
                        TextView code = card.findViewById(R.id.unitCode);
                        TextView name = card.findViewById(R.id.unitName);
                        TextView grade = card.findViewById(R.id.unitGrade);

                        code.setText(doc.getString("unitCode"));
                        name.setText(doc.getString("unitName"));
                        grade.setText(doc.getString("grade"));

                        unitsContainer.addView(card);
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (registration != null) registration.remove();
    }
}