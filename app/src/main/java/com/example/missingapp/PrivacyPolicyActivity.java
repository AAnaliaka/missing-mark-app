package com.example.missingapp;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;

public class PrivacyPolicyActivity extends AppCompatActivity {

    private NestedScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_privacy_policy);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.backBtn).setOnClickListener(v -> finish());
        scrollView = findViewById(R.id.scrollView);

        setupTOC();
        populateContent();
    }

    private void setupTOC() {
        LinearLayout toc = findViewById(R.id.toc);
        String[] sections = {
            "Personal Information We Collect",
            "How We Use Personal Information",
            "Cookies",
            "How We Share Personal Information",
            "Location of Personal Information",
            "How We Secure Information",
            "Advertising",
            "Access and Choice",
            "Children’s Personal Information",
            "Third Party Sites and Services",
            "Retention of Personal Information",
            "Contacts, Notices, and Revisions",
            "Additional Information for Certain Jurisdictions (Kenya)",
            "Examples of Information Collected"
        };

        int[] sectionIds = {
            R.id.section1, R.id.section2, R.id.section3, R.id.section4, R.id.section5,
            R.id.section6, R.id.section7, R.id.section8, R.id.section9, R.id.section10,
            R.id.section11, R.id.section12, R.id.section13, R.id.section14
        };

        for (int i = 0; i < sections.length; i++) {
            TextView link = new TextView(this);
            link.setText(sections[i]);
            link.setTextColor(Color.parseColor("#00D2FF")); // accent_blue
            link.setPadding(0, 8, 0, 8);
            link.setTextSize(14);
            link.setTypeface(null, Typeface.BOLD); // Make them BOLD
            final int targetId = sectionIds[i];
            link.setOnClickListener(v -> {
                View targetView = findViewById(targetId);
                if (targetView != null) {
                    scrollView.smoothScrollTo(0, targetView.getTop());
                }
            });
            toc.addView(link);
        }
    }

    private void populateContent() {
        ((TextView)findViewById(R.id.text1)).setText("We collect your personal information in the course of providing MissingMarks APP Offerings to you.\n\nInformation You Give Us: We collect any information you provide in relation to MissingMarks APP Offerings.\n\nAutomatic Information: We automatically collect certain types of information when you interact with MissingMarks APP Offerings.\n\nInformation from Other Sources: We might collect information about you from other sources, including service providers, partners, and publicly available sources.");
        ((TextView)findViewById(R.id.text2)).setText("We use your personal information to operate, provide, and improve MissingMarks APP Offerings. Our purposes for using personal information include providing and delivering offerings, measuring and supporting performance, personalization, complying with legal obligations, communicating with you, marketing, and fraud prevention.");
        ((TextView)findViewById(R.id.text3)).setText("To enable our systems to recognize your browser or device, and to provide, market, and improve MissingMarks APP Offerings, we and approved third parties use cookies and other identifiers.");
        ((TextView)findViewById(R.id.text4)).setText("Information about our customers is an important part of our business and we are not in the business of selling our customers’ personal information to others. We share personal information only as described in this notice and with Ryvo Limited and its affiliates.");
        ((TextView)findViewById(R.id.text5)).setText("Ryvo Limited is located in Kakamega, Kenya. Depending on the scope of your interactions, your personal information may be stored in or accessed from multiple countries in accordance with the Data Protection Act, 2019 of Kenya.");
        ((TextView)findViewById(R.id.text6)).setText("At MissingMarks APP, security is our highest priority. We protect the security of your information during transmission using encryption protocols and maintain physical, electronic, and procedural safeguards.");
        ((TextView)findViewById(R.id.text7)).setText("To help you receive more useful and relevant ads, MissingMarks APP shares limited personal information with our advertising partners, using identifiers that don't directly identify you by name.");
        ((TextView)findViewById(R.id.text8)).setText("You can view, update, and delete certain information about your account and your interactions with MissingMarks APP Offerings. You have choices about the collection and use of your personal information.");
        ((TextView)findViewById(R.id.text9)).setText("We don’t provide MissingMarks APP Offerings for purchase by children. If you’re under 18, you may use our offerings only with the involvement of a parent or guardian.");
        ((TextView)findViewById(R.id.text10)).setText("Our offerings may embed content or links to other websites. We are not responsible for the practices of these third-party companies.");
        ((TextView)findViewById(R.id.text11)).setText("We keep your personal information to enable your continued use of MissingMarks APP Offerings, for as long as it is required to fulfill relevant purposes or as required by law.");
        ((TextView)findViewById(R.id.text12)).setText("If you have any concerns about privacy at MissingMarks APP, please contact us at hello@ryvo.life. Our business changes constantly, and our Privacy Notice may also change.");
        ((TextView)findViewById(R.id.text13)).setText("For customers located in Kenya, Ryvo Limited is the data controller. We process data in compliance with the Data Protection Act, 2019. You have rights to be informed, access, object, correct, and delete your data.");
        ((TextView)findViewById(R.id.text14)).setText("Examples of information include your name, email, registration number, and usage metrics like IP address and device information.");
    }
}