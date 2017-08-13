package me.impy.aegis;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.github.paolorotolo.appintro.ISlidePolicy;
import com.github.paolorotolo.appintro.ISlideSelectionListener;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import me.impy.aegis.crypto.CryptoUtils;
import me.impy.aegis.crypto.slots.PasswordSlot;
import me.impy.aegis.crypto.slots.Slot;

public class CustomAuthenticatedSlide extends Fragment implements ISlidePolicy, ISlideSelectionListener {
    private int cryptType;
    private EditText textPassword;
    private EditText textPasswordConfirm;
    private int bgColor;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_authenticated_slide, container, false);
        textPassword = (EditText) view.findViewById(R.id.text_password);
        textPasswordConfirm = (EditText) view.findViewById(R.id.text_password_confirm);
        view.findViewById(R.id.main).setBackgroundColor(bgColor);
        return view;
    }

    /*@Override
    public int buttonsColor() {
        return R.color.colorAccent;
    }*/

    public int getCryptType() {
        return cryptType;
    }

    public Cipher getCipher(PasswordSlot slot, int mode)
            throws InvalidKeySpecException, NoSuchAlgorithmException,
            InvalidKeyException, NoSuchPaddingException {
        char[] password = getPassword(true);
        byte[] salt = CryptoUtils.generateSalt();
        SecretKey key = slot.deriveKey(password, salt, CryptoUtils.CRYPTO_SCRYPT_N, CryptoUtils.CRYPTO_SCRYPT_r, CryptoUtils.CRYPTO_SCRYPT_p);
        CryptoUtils.zero(password);

        return Slot.createCipher(key, mode);
    }

    private char[] getPassword(boolean clear) {
        char[] password = getEditTextChars(textPassword);
        if (clear) {
            textPassword.getText().clear();
        }
        return password;
    }

    private static char[] getEditTextChars(EditText text) {
        Editable editable = text.getText();
        char[] chars = new char[editable.length()];
        editable.getChars(0, editable.length(), chars, 0);
        return chars;
    }

    @Override
    public void onSlideSelected() {
        Intent intent = getActivity().getIntent();
        cryptType = intent.getIntExtra("cryptType", 1337);

        switch(cryptType) {
            case CustomAuthenticationSlide.CRYPT_TYPE_NONE:
                break;
            case CustomAuthenticationSlide.CRYPT_TYPE_PASS:
                break;
            case CustomAuthenticationSlide.CRYPT_TYPE_FINGER:
                break;
            default:
                throw new RuntimeException();
        }
    }

    public void setBgColor(int color) {
        bgColor = color;
    }

    @Override
    public void onSlideDeselected() {
    }

    @Override
    public boolean isPolicyRespected() {
        switch(cryptType) {
            case CustomAuthenticationSlide.CRYPT_TYPE_NONE:
                return true;
            case CustomAuthenticationSlide.CRYPT_TYPE_PASS:
                char[] password = getEditTextChars(textPassword);
                char[] passwordConfirm = getEditTextChars(textPasswordConfirm);
                boolean equal = password.length != 0 && Arrays.equals(password, passwordConfirm);
                CryptoUtils.zero(password);
                CryptoUtils.zero(passwordConfirm);
                return equal;
            case CustomAuthenticationSlide.CRYPT_TYPE_FINGER:
                return false;
            default:
                throw new RuntimeException();
        }
    }

    @Override
    public void onUserIllegallyRequestedNextPage() {
        View view = getView();
        if (view != null) {
            Snackbar snackbar = Snackbar.make(getView(), "Passwords should be equal and non-empty", Snackbar.LENGTH_LONG);
            snackbar.show();
        }
    }
}
