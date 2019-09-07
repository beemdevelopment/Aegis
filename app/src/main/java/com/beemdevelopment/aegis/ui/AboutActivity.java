package com.beemdevelopment.aegis.ui;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.beemdevelopment.aegis.Preferences;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.Theme;
import com.beemdevelopment.aegis.helpers.ThemeHelper;
import com.beemdevelopment.aegis.ui.glide.GlideLicense;
import com.mikepenz.iconics.Iconics;
import com.mikepenz.iconics.context.IconicsLayoutInflater2;
import com.mikepenz.material_design_iconic_typeface_library.MaterialDesignIconic;

import androidx.core.view.LayoutInflaterCompat;

import de.psdev.licensesdialog.LicenseResolver;
import de.psdev.licensesdialog.LicensesDialog;
import de.psdev.licensesdialog.licenses.License;

public class AboutActivity extends AegisActivity {

    private static String GITHUB = "https://github.com/beemdevelopment/Aegis";
    private static String WEBSITE_ALEXANDER = "https://alexbakker.me";
    private static String GITHUB_MICHAEL = "https://github.com/michaelschattgen";

    private static String MAIL_BEEMDEVELOPMENT = "beemdevelopment@gmail.com";
    private static String WEBSITE_BEEMDEVELOPMENT = "https://beem.dev/";
    private static String PLAYSTORE_BEEMDEVELOPMENT = "https://play.google.com/store/apps/details?id=com.beemdevelopment.aegis";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LayoutInflaterCompat.setFactory2(getLayoutInflater(), new IconicsLayoutInflater2(getDelegate()));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        Iconics.init(getApplicationContext());
        Iconics.registerFont(new MaterialDesignIconic());

        View btnLicenses = findViewById(R.id.btn_licenses);
        btnLicenses.setOnClickListener(v -> showLicenseDialog());

        TextView appVersion = findViewById(R.id.app_version);
        appVersion.setText(getCurrentVersion());

        View btnGithub = findViewById(R.id.btn_github);
        btnGithub.setOnClickListener(v -> openUrl(GITHUB));

        View btnAlexander = findViewById(R.id.btn_alexander);
        btnAlexander.setOnClickListener(v -> openUrl(WEBSITE_ALEXANDER));

        View btnMichael = findViewById(R.id.btn_michael);
        btnMichael.setOnClickListener(v -> openUrl(GITHUB_MICHAEL));

        View btnMail = findViewById(R.id.btn_email);
        btnMail.setOnClickListener(v -> openMail(MAIL_BEEMDEVELOPMENT));

        View btnWebsite = findViewById(R.id.btn_website);
        btnWebsite.setOnClickListener(v -> openUrl(WEBSITE_BEEMDEVELOPMENT));

        View btnRate = findViewById(R.id.btn_rate);
        btnRate.setOnClickListener(v -> openUrl(PLAYSTORE_BEEMDEVELOPMENT ));

        View btnChangelog = findViewById(R.id.btn_changelog);
        btnChangelog.setOnClickListener(v -> {
            ChangelogDialog.create().setTheme(getCurrentTheme()).show(getSupportFragmentManager(), "CHANGELOG_DIALOG");
        });
    }

    private String getCurrentVersion() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return "Unknown version";
    }

    private void openUrl(String url) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW);
        browserIntent.setData(Uri.parse(url));
        browserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        startActivity(browserIntent);
    }

    private void openMail(String mailaddress) {
        Intent mailIntent = new Intent(Intent.ACTION_SENDTO);
        mailIntent.setData(Uri.parse("mailto:" + mailaddress));
        mailIntent.putExtra(Intent.EXTRA_EMAIL, mailaddress);
        mailIntent.putExtra(Intent.EXTRA_SUBJECT, R.string.app_name_full);

        startActivity(Intent.createChooser(mailIntent, this.getString(R.string.email)));
    }

    private void showLicenseDialog() {
        String stylesheet = getString(R.string.custom_notices_format_style);
        int backgroundColorResource = getCurrentTheme() == Theme.AMOLED ? R.attr.cardBackgroundFocused : R.attr.cardBackground;
        String backgroundColor = String.format("%06X", (0xFFFFFF & ThemeHelper.getThemeColor(backgroundColorResource, getTheme())));
        String textColor = String.format("%06X", (0xFFFFFF & ThemeHelper.getThemeColor(R.attr.primaryText, getTheme())));
        String licenseColor = String.format("%06X", (0xFFFFFF & ThemeHelper.getThemeColor(R.attr.cardBackgroundFocused, getTheme())));

        stylesheet = String.format(stylesheet, backgroundColor, textColor, licenseColor);

        LicenseResolver.registerLicense(new GlideLicense());
        new LicensesDialog.Builder(this)
                .setNotices(R.raw.notices)
                .setTitle(R.string.licenses)
                .setNoticesCssStyle(stylesheet)
                .setIncludeOwnLicense(true)
                .build()
                .show();
    }
}
