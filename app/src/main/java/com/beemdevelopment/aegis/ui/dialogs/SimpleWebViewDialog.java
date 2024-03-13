package com.beemdevelopment.aegis.ui.dialogs;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.Theme;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.common.io.CharStreams;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public abstract class SimpleWebViewDialog extends DialogFragment {
    private Theme _theme;
    private final @StringRes int _title;

    protected SimpleWebViewDialog(@StringRes int title) {
        _title = title;
    }

    protected abstract String getContent(Context context);

    @SuppressLint("InflateParams")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View view;
        try {
            view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_web_view, null);
        } catch (InflateException e) {
            e.printStackTrace();
            return new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(android.R.string.dialog_alert_title)
                    .setMessage(getString(R.string.webview_error))
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(_title)
                .setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .show();

        String content = getContent(requireContext());
        final WebView webView = view.findViewById(R.id.web_view);
        webView.loadData(content, "text/html", "UTF-8");
        return dialog;
    }

    public SimpleWebViewDialog setTheme(Theme theme) {
        _theme = theme;
        return this;
    }

    protected String getBackgroundColor() {
        int color = MaterialColors.getColor(
                requireContext(),
                com.google.android.material.R.attr.colorSurfaceContainerHigh,
                getClass().getCanonicalName()
        );
        return colorToCSS(color);
    }

    protected String getTextColor() {
        int color = MaterialColors.getColor(
                requireContext(),
                com.google.android.material.R.attr.colorOnSurface,
                getClass().getCanonicalName()
        );
        return colorToCSS(0xFFFFFF & color);
    }

    @SuppressLint("DefaultLocale")
    private static String colorToCSS(int color) {
        return String.format("rgb(%d, %d, %d)", Color.red(color), Color.green(color), Color.blue(color));
    }

    protected static String readAssetAsString(Context context, String name) {
        try (InputStream inStream = context.getAssets().open(name);
             InputStreamReader reader = new InputStreamReader(inStream, StandardCharsets.UTF_8)) {
            return CharStreams.toString(reader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
