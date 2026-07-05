package com.example.missingapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class HomeFragment extends Fragment {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private LinearLayout unitsContainer;
    private TextView userName, progressValue;
    private ProgressBar heroProgressBar;
    private ListenerRegistration resultsListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_homepage, container, false);
        
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        View mainContent = view.findViewById(R.id.mainContent);
        if (mainContent != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainContent, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), systemBars.top + 16, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }

        userName = view.findViewById(R.id.userName);
        unitsContainer = view.findViewById(R.id.unitsContainer);
        progressValue = view.findViewById(R.id.progressValue);
        heroProgressBar = view.findViewById(R.id.heroProgressBar); // Ensure this ID exists in XML or add it

        loadUserProfile();
        listenToResults(inflater, view);

        view.findViewById(R.id.trackMissingBtn).setOnClickListener(v -> 
            startActivity(new Intent(getActivity(), TrackMissingActivity.class)));

        view.findViewById(R.id.reportMissingCard).setOnClickListener(v -> 
            startActivity(new Intent(getActivity(), ReportMissingActivity.class)));
        
        view.findViewById(R.id.viewResultsCard).setOnClickListener(v -> 
            startActivity(new Intent(getActivity(), ViewResultsActivity.class)));

        view.findViewById(R.id.profileCard).setOnClickListener(v -> {
            ((Homepage)getActivity()).switchToTab(R.id.navigation_profile);
        });

        view.findViewById(R.id.notificationIcon).setOnClickListener(v -> {
            Toast.makeText(getContext(), "Search Feature coming soon!", Toast.LENGTH_SHORT).show();
        });

        return view;
    }

    private void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String regNo = documentSnapshot.getString("regNo");
                        if (regNo != null) userName.setText(regNo);
                        else userName.setText(user.getEmail());
                    }
                });
    }

    private void listenToResults(LayoutInflater inflater, View rootView) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        ProgressBar progressBar = rootView.findViewById(R.id.heroProgressBar);
        if (progressBar == null) {
            // Find by type if ID not set correctly in original layout
            // or just use the one we find in the trackMissingBtn container
            View heroCard = rootView.findViewById(R.id.trackMissingBtn);
            if (heroCard != null) {
                progressBar = heroCard.findViewById(R.id.heroProgressBar);
            }
        }
        final ProgressBar finalProgressBar = progressBar;

        resultsListener = db.collection("results")
                .whereEqualTo("studentUid", user.getUid())
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        unitsContainer.removeAllViews();
                        int totalUnits = value.size();
                        int missingMarks = 0;

                        for (DocumentSnapshot doc : value.getDocuments()) {
                            View card = inflater.inflate(R.layout.item_course_simple, unitsContainer, false);
                            
                            TextView code = card.findViewById(R.id.unitCode);
                            TextView name = card.findViewById(R.id.unitName);
                            TextView grade = card.findViewById(R.id.unitGrade);
                            Button reportBtn = card.findViewById(R.id.reportMissingBtn);

                            String unitCode = doc.getString("unitCode");
                            String unitName = doc.getString("unitName");
                            String unitGrade = doc.getString("grade");

                            code.setText(unitCode);
                            name.setText(unitName);
                            grade.setText(unitGrade);

                            if ("M".equals(unitGrade)) {
                                missingMarks++;
                                reportBtn.setVisibility(View.VISIBLE);
                                reportBtn.setOnClickListener(v -> {
                                    Intent intent = new Intent(getActivity(), ReportMissingActivity.class);
                                    intent.putExtra("courseCode", unitCode);
                                    intent.putExtra("courseName", unitName);
                                    startActivity(intent);
                                });
                            } else {
                                reportBtn.setVisibility(View.GONE);
                            }

                            unitsContainer.addView(card);
                        }

                        if (totalUnits > 0) {
                            int resolved = totalUnits - missingMarks;
                            int percentage = (resolved * 100) / totalUnits;
                            progressValue.setText(percentage + "%");
                            if (finalProgressBar != null) {
                                finalProgressBar.setProgress(percentage);
                            }
                        } else {
                            progressValue.setText("0%");
                            if (finalProgressBar != null) finalProgressBar.setProgress(0);
                        }
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (resultsListener != null) {
            resultsListener.remove();
        }
    }
}