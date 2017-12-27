package me.impy.aegis.helpers;

import android.content.Context;
import android.support.annotation.ArrayRes;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class SpinnerHelper {
    private SpinnerHelper() {

    }

    public static void fillSpinner(Context context, Spinner spinner, @ArrayRes int textArrayResId) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context, textArrayResId, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.invalidate();
    }
}
