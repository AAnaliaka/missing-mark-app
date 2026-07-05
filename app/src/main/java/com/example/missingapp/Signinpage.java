package com.example.missingapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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

public class Signinpage extends AppCompatActivity {

    private static final String TAG = "Signinpage";
    private static final int RC_SIGN_IN = 9001;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;
    private EditText loginInput, passwordEditText;
    private Button loginButton;
    private ProgressBar loader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this, 
            SystemBarStyle.dark(Color.TRANSPARENT), 
            SystemBarStyle.light(Color.WHITE, Color.WHITE));
            
        setContentView(R.layout.activity_signinpage);
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
            Log.e(TAG, "Google Sign-In configuration failed. Check default_web_client_id.", e);
        }

        View mainView = findViewById(R.id.main);
        View backBtn = findViewById(R.id.backBtn);
        View welcomeTitle = findViewById(R.id.welcomeTitle);

        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            if (backBtn != null) {
                backBtn.setPadding(backBtn.getPaddingLeft(), systemBars.top, backBtn.getPaddingRight(), backBtn.getPaddingBottom());
            }
            if (welcomeTitle != null) {
                welcomeTitle.setPadding(welcomeTitle.getPaddingLeft(), systemBars.top, welcomeTitle.getPaddingRight(), welcomeTitle.getPaddingBottom());
            }
            return insets;
        });

        loginInput = findViewById(R.id.registrationEditText); 
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        loader = findViewById(R.id.loader);

        if (loginButton != null) {
            loginButton.setOnClickListener(v -> {
                String email = loginInput.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();

                if (TextUtils.isEmpty(email)) {
                    loginInput.setError("Required");
                    return;
                }
                if (TextUtils.isEmpty(password)) {
                    passwordEditText.setError("Required");
                    return;
                }

                performFirebaseLogin(email, password);
            });
        }

        View google = findViewById(R.id.googleIcon);
        if (google != null) google.setOnClickListener(v -> signInWithGoogle());

        View signupLink = findViewById(R.id.signupText);
        if (signupLink != null) {
            signupLink.setOnClickListener(v -> {
                startActivity(new Intent(Signinpage.this, Signuppage.class));
            });
        }
        
        View forgotPass = findViewById(R.id.forgotPassword);
        if (forgotPass != null) {
            forgotPass.setOnClickListener(v -> {
                startActivity(new Intent(Signinpage.this, Resetpasswordpage.class));
            });
        }
    }

    private void performFirebaseLogin(String email, String password) {
        if (loader != null) loader.setVisibility(View.VISIBLE);
        Log.d(TAG, "Attempting login for: " + email);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Auth successful, checking role...");
                        checkUserRole(mAuth.getCurrentUser());
                    } else {
                        Log.e(TAG, "Auth failed", task.getException());
                        if (loader != null) loader.setVisibility(View.GONE);
                        Toast.makeText(Signinpage.this, "Access denied. Please check your credentials.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void checkUserRole(FirebaseUser user) {
        if (user == null) {
            if (loader != null) loader.setVisibility(View.GONE);
            return;
        }

        Log.d(TAG, "Fetching Firestore document for UID: " + user.getUid());
        
        // Force fetch from server to bypass potential "client is offline" cache issues
        db.collection("users").document(user.getUid()).get(com.google.firebase.firestore.Source.SERVER)
                .addOnCompleteListener(task -> {
                    if (loader != null) loader.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        if (task.getResult() != null && task.getResult().exists()) {
                            Log.d(TAG, "User document found");
                            
                            String role = task.getResult().getString("role");
                            Boolean isComplete = task.getResult().getBoolean("isProfileComplete");

                            // FORCE LECTURER ROLE if the email matches
                            if ("lecturer@mmust.ac.ke".equalsIgnoreCase(user.getEmail())) {
                                if (!"lecturer".equals(role)) {
                                    // Update the role if it's wrong
                                    db.collection("users").document(user.getUid()).update("role", "lecturer", "isProfileComplete", true);
                                    role = "lecturer";
                                    isComplete = true;
                                }
                            }

                            if (isComplete != null && isComplete) {
                                if ("lecturer".equals(role)) {
                                    startActivity(new Intent(Signinpage.this, LecturerHomeActivity.class));
                                } else {
                                    startActivity(new Intent(Signinpage.this, Homepage.class));
                                }
                            } else {
                                Log.d(TAG, "Profile incomplete, redirecting to RegisterInfoActivity");
                                startActivity(new Intent(Signinpage.this, RegisterInfoActivity.class));
                            }
                            finish();
                        } else {
                            Log.d(TAG, "User document missing, creating one...");
                            createUserInFirestore(user);
                        }
                    } else {
                        Log.e(TAG, "Firestore fetch failed", task.getException());
                        String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        
                        // Fallback: If server fetch fails, try cache before giving up
                        if (error.contains("offline")) {
                            db.collection("users").document(user.getUid()).get(com.google.firebase.firestore.Source.CACHE)
                                .addOnCompleteListener(cacheTask -> {
                                    if (cacheTask.isSuccessful() && cacheTask.getResult().exists()) {
                                        checkUserRole(user); // Retry with cache data
                                    } else {
                                        Toast.makeText(Signinpage.this, "Network Error: Please check your internet connection and try again.", Toast.LENGTH_LONG).show();
                                    }
                                });
                        } else {
                            Toast.makeText(Signinpage.this, "Error: " + error, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void createUserInFirestore(FirebaseUser user) {
        if (loader != null) loader.setVisibility(View.VISIBLE);
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", user.getEmail());
        userData.put("role", "student");
        userData.put("isProfileComplete", false);
        userData.put("uid", user.getUid());

        db.collection("users").document(user.getUid())
                .set(userData, SetOptions.merge())
                .addOnCompleteListener(task -> {
                    if (loader != null) loader.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        startActivity(new Intent(Signinpage.this, RegisterInfoActivity.class));
                        finish();
                    } else {
                        Log.e(TAG, "Failed to create user doc", task.getException());
                        Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
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
                        Log.e(TAG, "Firebase Auth with Google failed", task.getException());
                        Toast.makeText(this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}