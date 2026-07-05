package com.example.missingapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
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

import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private TextView nameTxt, emailTxt, regNoTxt, schoolTxt, deptTxt, progTxt, yearTxt;
    private TextView statUnits, statMissing, statResolved;
    private FirebaseFirestore db;
    private ListenerRegistration resultsListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_profile, container, false);
        
        db = FirebaseFirestore.getInstance();

        View scrollContainer = view.findViewById(R.id.scrollContainer);
        if (scrollContainer != null) {
            ViewCompat.setOnApplyWindowInsetsListener(scrollContainer, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), systemBars.top + 24, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }

        view.findViewById(R.id.logoutActionBtn).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getActivity(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        });

        nameTxt = view.findViewById(R.id.profileName);
        emailTxt = view.findViewById(R.id.profileEmail);
        regNoTxt = view.findViewById(R.id.profileRegNo);
        schoolTxt = view.findViewById(R.id.profileSchool);
        deptTxt = view.findViewById(R.id.profileDepartment);
        progTxt = view.findViewById(R.id.profileProgramme);
        yearTxt = view.findViewById(R.id.profileYear);

        statUnits = view.findViewById(R.id.statUnits);
        statMissing = view.findViewById(R.id.statMissing);
        statResolved = view.findViewById(R.id.statResolved);

        view.findViewById(R.id.editProfileBtn).setOnClickListener(v -> showEditProfileDialog());

        loadProfileData();
        listenToStats();

        return view;
    }

    private void loadProfileData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String reg = doc.getString("regNo");
                        if (reg != null) regNoTxt.setText(reg);
                        emailTxt.setText(doc.getString("email"));
                    }
                });

        db.collection("studentProfiles").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        schoolTxt.setText("School: " + doc.getString("school"));
                        deptTxt.setText("Dept: " + doc.getString("department"));
                        progTxt.setText("Prog: " + doc.getString("programme"));
                        yearTxt.setText("Year: " + doc.getString("yearOfStudy"));
                        
                        String name = user.getDisplayName();
                        if (name == null || name.isEmpty()) {
                            name = user.getEmail();
                            if (name != null && name.contains("@")) {
                                name = name.split("@")[0].toUpperCase();
                            }
                        }
                        nameTxt.setText(name);
                    }
                });
    }

    private void listenToStats() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        resultsListener = db.collection("results")
                .whereEqualTo("studentUid", user.getUid())
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    
                    int total = value.size();
                    int missing = 0;
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        if ("M".equals(doc.getString("grade"))) missing++;
                    }
                    
                    statUnits.setText(String.valueOf(total));
                    statMissing.setText(String.valueOf(missing));
                    statResolved.setText(String.valueOf(total - missing));
                });
    }

    private void showEditProfileDialog() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText yearInput = new EditText(getContext());
        yearInput.setHint("Update Year (1-4)");
        layout.addView(yearInput);

        final EditText deptInput = new EditText(getContext());
        deptInput.setHint("Update Department");
        layout.addView(deptInput);

        new AlertDialog.Builder(getContext())
            .setTitle("Edit Profile")
            .setMessage("Update your academic details")
            .setView(layout)
            .setPositiveButton("UPDATE", (dialog, which) -> {
                String year = yearInput.getText().toString().trim();
                String dept = deptInput.getText().toString().trim();
                
                Map<String, Object> updates = new HashMap<>();
                if (!year.isEmpty()) updates.put("yearOfStudy", year);
                if (!dept.isEmpty()) updates.put("department", dept);
                
                if (!updates.isEmpty()) {
                    db.collection("studentProfiles").document(user.getUid()).update(updates)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), "Profile Updated", Toast.LENGTH_SHORT).show();
                            loadProfileData();
                        });
                }
            })
            .setNegativeButton("CANCEL", null)
            .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (resultsListener != null) resultsListener.remove();
    }
}