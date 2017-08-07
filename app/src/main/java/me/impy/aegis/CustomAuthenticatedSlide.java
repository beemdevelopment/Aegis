package me.impy.aegis;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import agency.tango.materialintroscreen.SlideFragment;
import me.impy.aegis.crypto.CryptoUtils;
import me.impy.aegis.crypto.slots.PasswordSlot;
import me.impy.aegis.crypto.slots.Slot;

public class CustomAuthenticatedSlide extends SlideFragment {
    private int cryptType;
    private EditText textPassword;
    private EditText textPasswordConfirm;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_authenticated_slide, container, false);
        textPassword = (EditText) view.findViewById(R.id.text_password);
        textPasswordConfirm = (EditText) view.findViewById(R.id.text_password_confirm);
        return view;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (!isVisibleToUser) {
            return;
        }

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

    @Override
    public int backgroundColor() {
        return R.color.colorHeaderSuccess;
    }

    @Override
    public int buttonsColor() {
        return R.color.colorAccent;
    }

    @Override
    public boolean canMoveFurther() {
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
    public String cantMoveFurtherErrorMessage() {
        return "Passwords should be equal and non-empty";
    }

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
}
