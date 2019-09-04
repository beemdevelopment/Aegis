package com.beemdevelopment.aegis.ui;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.Theme;
import com.beemdevelopment.aegis.helpers.ThemeHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

public class ChangelogDialog extends DialogFragment {
    private Theme _themeStyle;

    public static ChangelogDialog create() {
        return new ChangelogDialog();
    }

    @SuppressLint("InflateParams")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View customView;
        try {
            customView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_web_view, null);
        } catch (InflateException e) {
            e.printStackTrace();
            return new AlertDialog.Builder(getActivity())
                    .setTitle(android.R.string.dialog_alert_title)
                    .setMessage(getString(R.string.webview_error))
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }
        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setTitle("Changelog")
                .setView(customView)
                .setPositiveButton(android.R.string.ok, null)
                .show();

        final WebView webView = customView.findViewById(R.id.web_view);
        StringBuilder buf = new StringBuilder();

        try (InputStream html = getActivity().getAssets().open("changelog.html")) {
            BufferedReader in = new BufferedReader(new InputStreamReader(html, "UTF-8"));
            String str;
            while ((str = in.readLine()) != null)
                buf.append(str);

            in.close();
            String changelog = buf.toString();
            changelog = replaceStylesheet(changelog);
            webView.loadData(changelog, "text/html", "UTF-8");
        } catch (IOException e) {
            webView.loadData("<h1>Unable to load</h1><p>" + e.getLocalizedMessage() + "</p>", "text/html", "UTF-8");
        }
        return dialog;
    }

    private String replaceStylesheet(String changelog) {
        int backgroundColorResource = _themeStyle == Theme.AMOLED ? R.attr.cardBackgroundFocused : R.attr.cardBackground;
        String backgroundColor = String.format("%06X", (0xFFFFFF & ThemeHelper.getThemeColor(backgroundColorResource, getContext().getTheme())));
        String textColor = String.format("%06X", (0xFFFFFF & ThemeHelper.getThemeColor(R.attr.primaryText, getContext().getTheme())));

        return String.format(changelog, backgroundColor, textColor);
    }

    public ChangelogDialog setTheme(Theme theme) {
        _themeStyle = theme;

        return this;
    }
}

