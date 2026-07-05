package com.example.missingapp;

import android.os.Bundle;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class TermsOfUseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_terms_of_use);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.backBtn).setOnClickListener(v -> finish());

        TextView termsText = findViewById(R.id.termsText);
        String terms = "MissingMarks APP Site Terms\n" +
                "Last Updated: May 18, 2026\n\n" +
                "Welcome to the MissingMarks APP site (the “MissingMarks APP Site”). Ryvo Limited and/or its affiliates (“Ryvo”) provides the MissingMarks APP Site to you subject to the following terms of use (“Site Terms”). By visiting the MissingMarks APP Site, you accept the Site Terms. Please read them carefully. MissingMarks APP operates under the flag of Ryvo Limited.\n\n" +
                "Privacy\n" +
                "Please review our Privacy Notice, which also governs your visit to the MissingMarks APP Site.\n\n" +
                "Electronic Communications\n" +
                "When you visit the MissingMarks APP Site or send e-mails to us, you are communicating with us electronically. You consent to receive communications from us electronically.\n\n" +
                "Copyright\n" +
                "All content included on the MissingMarks APP Site is the property of Ryvo or its content suppliers and protected by Kenyan and international copyright laws.\n\n" +
                "Trademarks\n" +
                "“MissingMarks APP”, and all related marks are trademarks of Ryvo in Kenya and other countries.\n\n" +
                "License and Site Access\n" +
                "Ryvo grants you a limited license to access and make personal use of the MissingMarks APP Site.\n\n" +
                "Your Account\n" +
                "If you use the MissingMarks APP Site, you are responsible for maintaining the confidentiality of your account and password.\n\n" +
                "Applicable Law\n" +
                "By visiting the MissingMarks APP Site, you agree that the laws of the Republic of Kenya will govern these Site Terms.\n\n" +
                "Disputes\n" +
                "Any dispute relating to your visit to the MissingMarks APP Site shall be adjudicated in the courts of Nairobi, Kenya.\n\n" +
                "Our Address\n" +
                "Ryvo Limited\n" +
                "Kakamega, Kenya\n" +
                "Website: https://ryvo.life\n" +
                "Email: hello@ryvo.life\n\n" +
                "© 2026 Ryvo Limited. All rights reserved.";
        termsText.setText(terms);
    }
}