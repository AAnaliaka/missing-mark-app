package com.example.missingapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Fully transparent for both bars to allow image bleed
        EdgeToEdge.enable(this, 
            SystemBarStyle.dark(Color.TRANSPARENT), 
            SystemBarStyle.dark(Color.TRANSPARENT));
            
        setContentView(R.layout.activity_main);
        
        View mainView = findViewById(R.id.main);
        View contentContainer = findViewById(R.id.contentContainer);

        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // No padding on root so image fills 100% of the screen
            v.setPadding(0, 0, 0, 0);
            
            // Apply padding to content so it stays in safe areas
            if (contentContainer != null) {
                contentContainer.setPadding(
                    contentContainer.getPaddingLeft(),
                    systemBars.top,
                    contentContainer.getPaddingRight(),
                    systemBars.bottom
                );
            }
            return insets;
        });

        findViewById(R.id.loginbtn).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, Signinpage.class));
        });

        findViewById(R.id.signup).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, Signuppage.class));
        });

        findViewById(R.id.privacyPolicyLink).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, PrivacyPolicyActivity.class));
        });

        findViewById(R.id.termsOfUseLink).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, TermsOfUseActivity.class));
        });
    }
}