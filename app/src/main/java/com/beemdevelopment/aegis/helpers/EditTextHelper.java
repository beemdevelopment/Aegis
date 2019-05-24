package com.beemdevelopment.aegis.helpers;

import android.text.Editable;
import android.widget.EditText;

import java.util.Arrays;

public class EditTextHelper {
    private EditTextHelper() {
    }

    public static void clearEditText(EditText text) {
        text.getText().clear();
    }

    public static char[] getEditTextChars(EditText text) {
        return getEditTextChars(text, false);
    }

    public static char[] getEditTextChars(EditText text, boolean removeSpaces) {
        String editTextString = text.getText().toString();
        if (removeSpaces) {
            editTextString = editTextString.replaceAll("\\s","");
        }

        char[] chars = new char[editTextString.length()];
        editTextString.getChars(0, editTextString.length(), chars, 0);
        return chars;
    }

    public static boolean areEditTextsEqual(EditText text1, EditText text2) {
        char[] password = getEditTextChars(text1);
        char[] passwordConfirm = getEditTextChars(text2);
        return password.length != 0 && Arrays.equals(password, passwordConfirm);
    }
}
