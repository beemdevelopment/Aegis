package com.beemdevelopment.aegis.helpers;

import android.text.Editable;
import android.widget.EditText;

import java.util.Arrays;

public class EditTextHelper {
    private EditTextHelper() {
    }

    public static char[] getEditTextChars(EditText text) {
        Editable editable = text.getText();
        char[] chars = new char[editable.length()];
        editable.getChars(0, editable.length(), chars, 0);
        return chars;
    }

    public static boolean areEditTextsEqual(EditText text1, EditText text2) {
        char[] password = getEditTextChars(text1);
        char[] passwordConfirm = getEditTextChars(text2);
        return password.length != 0 && Arrays.equals(password, passwordConfirm);
    }
}
