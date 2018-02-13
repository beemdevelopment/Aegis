package me.impy.aegis.helpers;

import android.text.Editable;
import android.widget.EditText;

import java.util.Arrays;

import me.impy.aegis.crypto.CryptoUtils;

public class AuthHelper {
    private AuthHelper() {
    }

    public static void clearEditText(EditText text) {
        text.getText().clear();
    }

    public static char[] getEditTextChars(EditText text) {
        Editable editable = text.getText();
        char[] chars = new char[editable.length()];
        editable.getChars(0, editable.length(), chars, 0);
        return chars;
    }

    public static boolean arePasswordsEqual(EditText text1, EditText text2) {
        char[] password = getEditTextChars(text1);
        char[] passwordConfirm = getEditTextChars(text2);
        return password.length != 0 && Arrays.equals(password, passwordConfirm);
    }
}
