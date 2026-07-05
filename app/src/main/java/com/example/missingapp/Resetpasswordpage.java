package com.example.missingapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class Resetpasswordpage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Force status bar to be transparent and nav bar to be WHITE
        EdgeToEdge.enable(this, 
            SystemBarStyle.dark(Color.TRANSPARENT), 
            SystemBarStyle.light(Color.WHITE, Color.WHITE));

        setContentView(R.layout.activity_resetpasswordpage);
        
        View mainView = findViewById(R.id.main);
        View backBtn = findViewById(R.id.backBtn);

        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            
            if (backBtn != null) {
                backBtn.setPadding(backBtn.getPaddingLeft(), systemBars.top, backBtn.getPaddingRight(), backBtn.getPaddingBottom());
            }
            
            return insets;
        });

        TextView signupText = findViewById(R.id.signupText);
        EditText emailEditText = findViewById(R.id.emailEditText);
        Button sendBtn = findViewById(R.id.sendCodeButton);
        ProgressBar loader = findViewById(R.id.loader);

        if (sendBtn != null) {
            sendBtn.setOnClickListener(v -> {
                String email = emailEditText != null ? emailEditText.getText().toString().trim() : "";
                if (TextUtils.isEmpty(email)) {
                    if (emailEditText != null) emailEditText.setError("Email required");
                    return;
                }

                if (loader != null) loader.setVisibility(View.VISIBLE);
                FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                        .addOnCompleteListener(task -> {
                            if (loader != null) loader.setVisibility(View.GONE);
                            if (task.isSuccessful()) {
                                Toast.makeText(Resetpasswordpage.this, "Reset link sent to your email", Toast.LENGTH_LONG).show();
                                finish();
                            } else {
                                Toast.makeText(Resetpasswordpage.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
            });
        }

        if (backBtn != null) backBtn.setOnClickListener(v -> finish());

        if (signupText != null) {
            signupText.setOnClickListener(v -> {
                startActivity(new Intent(Resetpasswordpage.this, Signuppage.class));
            });
        }
    }
}