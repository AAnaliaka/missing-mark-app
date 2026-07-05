package com.example.missingapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class Signuppage extends AppCompatActivity {

    private static final String TAG = "Signuppage";
    private static final int RC_SIGN_IN = 9001;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;
    private EditText emailEditText, passwordEditText, confirmPasswordEditText;
    private Button signupButton;
    private CheckBox agreeCheckbox;
    private ProgressBar loader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this, 
            SystemBarStyle.dark(Color.TRANSPARENT), 
            SystemBarStyle.light(Color.WHITE, Color.WHITE));
            
        setContentView(R.layout.activity_signuppage);
        getWindow().setNavigationBarColor(Color.WHITE);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        try {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();
            mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        } catch (Exception e) {
            Log.e(TAG, "Google Sign-In configuration failed.", e);
        }

        View mainView = findViewById(R.id.main);
        View backBtn = findViewById(R.id.backBtn);
        View signupTitle = findViewById(R.id.signupTitle);

        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            if (backBtn != null) {
                backBtn.setPadding(backBtn.getPaddingLeft(), systemBars.top, backBtn.getPaddingRight(), backBtn.getPaddingBottom());
            }
            if (signupTitle != null) {
                signupTitle.setPadding(signupTitle.getPaddingLeft(), systemBars.top, signupTitle.getPaddingRight(), signupTitle.getPaddingBottom());
            }
            return insets;
        });

        emailEditText = findViewById(R.id.registrationEditText); 
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        signupButton = findViewById(R.id.signupButton);
        agreeCheckbox = findViewById(R.id.agreeCheckbox);
        loader = findViewById(R.id.loader);

        if (signupButton != null) {
            signupButton.setOnClickListener(v -> handleSignup());
        }

        View back = findViewById(R.id.backBtn);
        if (back != null) back.setOnClickListener(v -> finish());
        
        View loginLink = findViewById(R.id.loginText);
        if (loginLink != null) {
            loginLink.setOnClickListener(v -> {
                startActivity(new Intent(Signuppage.this, Signinpage.class));
            });
        }

        View google = findViewById(R.id.googleIcon);
        if (google != null) google.setOnClickListener(v -> signInWithGoogle());
    }

    private void handleSignup() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Password is required");
            return;
        }
        if (password.length() < 6) {
            passwordEditText.setError("Minimum 6 characters");
            return;
        }
        if (!password.equals(confirmPassword)) {
            confirmPasswordEditText.setError("Passwords do not match");
            return;
        }
        if (agreeCheckbox != null && !agreeCheckbox.isChecked()) {
            Toast.makeText(this, "Please agree to data processing", Toast.LENGTH_SHORT).show();
            return;
        }

        if (loader != null) loader.setVisibility(View.VISIBLE);
        Log.d(TAG, "Creating account for: " + email);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Auth user created, creating Firestore doc...");
                        createUserInFirestore(mAuth.getCurrentUser(), email);
                    } else {
                        Log.e(TAG, "Auth signup failed", task.getException());
                        if (loader != null) loader.setVisibility(View.GONE);
                        Toast.makeText(Signuppage.this, "Signup failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void createUserInFirestore(FirebaseUser user, String email) {
        if (user == null) return;

        String role = "student";
        boolean isComplete = false;

        // Automatically assign lecturer role to this specific email for testing
        if (email.equalsIgnoreCase("lecturer@mmust.ac.ke")) {
            role = "lecturer";
            isComplete = true;
        }

        Map<String, Object> userData = new HashMap<>();
        userData.put("email", user.getEmail());
        userData.put("role", role);
        userData.put("isProfileComplete", isComplete);
        userData.put("uid", user.getUid());

        final String finalRole = role;
        db.collection("users").document(user.getUid())
                .set(userData, SetOptions.merge())
                .addOnCompleteListener(task -> {
                    if (loader != null) loader.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Firestore user doc created");
                        Toast.makeText(Signuppage.this, "Account created successfully.", Toast.LENGTH_SHORT).show();
                        
                        if ("lecturer".equals(finalRole)) {
                            startActivity(new Intent(Signuppage.this, LecturerHomeActivity.class));
                        } else {
                            startActivity(new Intent(Signuppage.this, RegisterInfoActivity.class));
                        }
                        finish();
                    } else {
                        Log.e(TAG, "Firestore signup doc failed", task.getException());
                        Toast.makeText(Signuppage.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void signInWithGoogle() {
        if (mGoogleSignInClient == null) {
            Toast.makeText(this, "Google Sign-In is not configured correctly.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.e(TAG, "Google sign in failed", e);
                Toast.makeText(this, "Google sign in failed.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        if (loader != null) loader.setVisibility(View.VISIBLE);
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        checkUserRole(mAuth.getCurrentUser());
                    } else {
                        if (loader != null) loader.setVisibility(View.GONE);
                        Log.e(TAG, "Google Auth failed", task.getException());
                        Toast.makeText(this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUserRole(FirebaseUser user) {
        if (user == null) return;

        db.collection("users").document(user.getUid()).get()
                .addOnCompleteListener(task -> {
                    if (loader != null) loader.setVisibility(View.GONE);
                    if (task.isSuccessful() && task.getResult().exists()) {
                        String role = task.getResult().getString("role");
                        Boolean isComplete = task.getResult().getBoolean("isProfileComplete");

                        // FORCE LECTURER ROLE
                        if ("lecturer@mmust.ac.ke".equalsIgnoreCase(user.getEmail())) {
                            if (!"lecturer".equals(role)) {
                                db.collection("users").document(user.getUid()).update("role", "lecturer", "isProfileComplete", true);
                                role = "lecturer";
                                isComplete = true;
                            }
                        }

                        if (isComplete != null && isComplete) {
                            if ("lecturer".equals(role)) {
                                startActivity(new Intent(Signuppage.this, LecturerHomeActivity.class));
                            } else {
                                startActivity(new Intent(Signuppage.this, Homepage.class));
                            }
                        } else {
                            startActivity(new Intent(Signuppage.this, RegisterInfoActivity.class));
                        }
                        finish();
                    } else {
                        createUserInFirestore(user, user.getEmail());
                    }
                });
    }
}