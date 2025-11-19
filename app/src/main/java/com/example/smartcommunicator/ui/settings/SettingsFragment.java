package com.example.smartcommunicator.ui.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.example.smartcommunicator.R;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);

        // --- Handle Theme Selection ---
        ListPreference themePreference = findPreference("theme_preference");
        if (themePreference != null) {
            themePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                String themeValue = (String) newValue;
                applyTheme(themeValue);
                return true;
            });
        }

        // --- Handle Clicks on "About" items ---
        Preference feedbackPreference = findPreference("feedback_preference");
        if (feedbackPreference != null) {
            feedbackPreference.setOnPreferenceClickListener(preference -> {
                sendFeedbackEmail();
                return true;
            });
        }

        Preference sharePreference = findPreference("share_preference");
        if (sharePreference != null) {
            sharePreference.setOnPreferenceClickListener(preference -> {
                shareTheApp();
                return true;
            });
        }

        // --- THIS IS THE NEW LOGIC FOR "RATE APP" ---
        Preference rateAppPreference = findPreference("rate_app_preference");
        if (rateAppPreference != null) {
            rateAppPreference.setOnPreferenceClickListener(preference -> {
                rateTheApp();
                return true;
            });
        }
    }

    private void applyTheme(String themeValue) {
        // ... (this method is unchanged)
        switch (themeValue) {
            case "light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default: // "system"
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    private void sendFeedbackEmail() {
        // ... (this method is unchanged)
        String myEmailAddress = "communicatorsmart@gmail.com";
        String subject = "Feedback for Smart Communicator App";
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{myEmailAddress});
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        try {
            startActivity(Intent.createChooser(intent, "Send Feedback..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(getContext(), "There are no email clients installed.", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareTheApp() {
        // ... (this method is unchanged)
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        String shareBody = "Check out the Smart Communicator app! [Your App's Google Play Store Link Here]";
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(shareIntent, "Share using"));
    }

    // --- THIS WHOLE METHOD IS NEW ---
    private void rateTheApp() {
        // To get the package name dynamically
        final String appPackageName = requireContext().getPackageName();
        try {
            // Try to open the app page in the Google Play Store app
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
        } catch (android.content.ActivityNotFoundException anfe) {
            // If the Play Store app is not installed, open it in a web browser
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
        }
    }
}
