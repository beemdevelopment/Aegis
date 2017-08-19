package me.impy.aegis;

import android.content.Context;
import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.paolorotolo.appintro.ISlidePolicy;
import com.github.paolorotolo.appintro.ISlideSelectionListener;
import com.mattprecious.swirl.SwirlView;

import java.lang.reflect.UndeclaredThrowableException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import me.impy.aegis.crypto.CryptoUtils;
import me.impy.aegis.crypto.KeyStoreHandle;
import me.impy.aegis.crypto.slots.FingerprintSlot;
import me.impy.aegis.crypto.slots.PasswordSlot;
import me.impy.aegis.crypto.slots.Slot;
import me.impy.aegis.finger.FingerprintUiHelper;
import me.impy.aegis.helpers.AuthHelper;

public class CustomAuthenticatedSlide extends Fragment implements FingerprintUiHelper.Callback, ISlidePolicy, ISlideSelectionListener {
    private int cryptType;
    private EditText textPassword;
    private EditText textPasswordConfirm;
    private int bgColor;

    private LinearLayout boxFingerprint;
    private SwirlView imgFingerprint;
    private TextView textFingerprint;
    private FingerprintUiHelper fingerHelper;
    private KeyStoreHandle storeHandle;
    private Cipher fingerCipher;
    private boolean fingerAuthenticated;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_authenticated_slide, container, false);
        textPassword = (EditText) view.findViewById(R.id.text_password);
        textPasswordConfirm = (EditText) view.findViewById(R.id.text_password_confirm);
        boxFingerprint = (LinearLayout) view.findViewById(R.id.box_fingerprint);
        imgFingerprint = (SwirlView) view.findViewById(R.id.img_fingerprint);
        textFingerprint = (TextView) view.findViewById(R.id.text_fingerprint);
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

    public Cipher getCipher(Slot slot) throws Exception {
        if (slot instanceof PasswordSlot) {
            char[] password = AuthHelper.getPassword(textPassword, true);
            byte[] salt = CryptoUtils.generateSalt();
            SecretKey key = ((PasswordSlot)slot).deriveKey(password, salt, CryptoUtils.CRYPTO_SCRYPT_N, CryptoUtils.CRYPTO_SCRYPT_r, CryptoUtils.CRYPTO_SCRYPT_p);
            CryptoUtils.zero(password);
            return Slot.createCipher(key, Cipher.ENCRYPT_MODE);
        } else if (slot instanceof FingerprintSlot) {
            return fingerCipher;
        } else {
            throw new RuntimeException();
        }
    }

    public void setBgColor(int color) {
        bgColor = color;
    }

    @Override
    public void onSlideSelected() {
        Intent intent = getActivity().getIntent();
        cryptType = intent.getIntExtra("cryptType", CustomAuthenticationSlide.CRYPT_TYPE_INVALID);

        switch(cryptType) {
            case CustomAuthenticationSlide.CRYPT_TYPE_NONE:
            case CustomAuthenticationSlide.CRYPT_TYPE_PASS:
                break;
            case CustomAuthenticationSlide.CRYPT_TYPE_FINGER:
                boxFingerprint.setVisibility(View.VISIBLE);

                SecretKey key;
                try {
                    if (storeHandle == null) {
                        storeHandle = new KeyStoreHandle();
                    }
                    // TODO: consider regenerating the key if it exists
                    if (!storeHandle.keyExists()) {
                        key = storeHandle.generateKey(true);
                    } else {
                        key = storeHandle.getKey();
                    }
                } catch (Exception e) {
                    throw new UndeclaredThrowableException(e);
                }

                if (fingerHelper == null) {
                    FingerprintManager fingerManager = (FingerprintManager) getContext().getSystemService(Context.FINGERPRINT_SERVICE);
                    fingerHelper = new FingerprintUiHelper(fingerManager, imgFingerprint, textFingerprint, this);
                }

                try {
                    fingerCipher = Slot.createCipher(key, Cipher.ENCRYPT_MODE);
                } catch (Exception e) {
                    throw new UndeclaredThrowableException(e);
                }
                fingerHelper.startListening(new FingerprintManager.CryptoObject(fingerCipher));
                break;
            default:
                throw new RuntimeException();
        }
    }

    @Override
    public void onSlideDeselected() {
        if (fingerHelper != null) {
            fingerAuthenticated = false;
            boxFingerprint.setVisibility(View.INVISIBLE);
            fingerHelper.stopListening();
        }
    }

    @Override
    public boolean isPolicyRespected() {
        switch(cryptType) {
            case CustomAuthenticationSlide.CRYPT_TYPE_NONE:
                return true;
            case CustomAuthenticationSlide.CRYPT_TYPE_FINGER:
                if (!fingerAuthenticated) {
                    return false;
                }
                // intentional fallthrough
            case CustomAuthenticationSlide.CRYPT_TYPE_PASS:
                return AuthHelper.arePasswordsEqual(textPassword, textPasswordConfirm);
            default:
                throw new RuntimeException();
        }
    }

    @Override
    public void onUserIllegallyRequestedNextPage() {
        String message;
        if (!AuthHelper.arePasswordsEqual(textPassword, textPasswordConfirm)) {
            message = "Passwords should be equal and non-empty";
        } else if (!fingerAuthenticated) {
            message = "Register your fingerprint";
        } else {
            return;
        }

        View view = getView();
        if (view != null) {
            Snackbar snackbar = Snackbar.make(getView(), message, Snackbar.LENGTH_LONG);
            snackbar.show();
        }
    }

    @Override
    public void onAuthenticated() {
        fingerAuthenticated = true;
    }

    @Override
    public void onError() {

    }
}
