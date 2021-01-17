package com.beemdevelopment.aegis.helpers;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.annotation.ArrayRes;

import com.beemdevelopment.aegis.R;

import java.util.List;

public class DropdownHelper {
    private DropdownHelper() {

    }

    public static void fillDropdown(Context context, AutoCompleteTextView dropdown, @ArrayRes int textArrayResId) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context, textArrayResId, R.layout.dropdown_list_item);
        dropdown.setAdapter(adapter);
    }

    public static <T> void fillDropdown(Context context, AutoCompleteTextView dropdown, List<T> items) {
        ArrayAdapter<T> adapter = new ArrayAdapter<>(context, R.layout.dropdown_list_item, items);
        dropdown.setAdapter(adapter);
    }
}
