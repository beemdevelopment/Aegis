package com.beemdevelopment.aegis.helpers;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.util.List;

import androidx.annotation.ArrayRes;

public class SpinnerHelper {
    private SpinnerHelper() {

    }

    public static void fillSpinner(Context context, Spinner spinner, @ArrayRes int textArrayResId) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context, textArrayResId, android.R.layout.simple_spinner_item);
        initSpinner(spinner, adapter);
    }

    public static <T> void fillSpinner(Context context, Spinner spinner, List<T> items) {
        ArrayAdapter adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, items);
        initSpinner(spinner, adapter);
    }

    private static void initSpinner(Spinner spinner, ArrayAdapter adapter) {
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.invalidate();
    }
}
