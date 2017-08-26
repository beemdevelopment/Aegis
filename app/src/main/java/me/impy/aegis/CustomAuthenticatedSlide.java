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
    private int _cryptType;
    private EditText _textPassword;
    private EditText _textPasswordConfirm;
    private int _bgColor;

    private LinearLayout _boxFingerprint;
    private SwirlView _imgFingerprint;
    private TextView _textFingerprint;
    private FingerprintUiHelper _fingerHelper;
    private KeyStoreHandle _storeHandle;
    private Cipher _fingerCipher;
    private boolean _fingerAuthenticated;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_authenticated_slide, container, false);
        _textPassword = (EditText) view.findViewById(R.id.text_password);
        _textPasswordConfirm = (EditText) view.findViewById(R.id.text_password_confirm);
        _boxFingerprint = (LinearLayout) view.findViewById(R.id.box_fingerprint);
        _imgFingerprint = (SwirlView) view.findViewById(R.id.img_fingerprint);
        _textFingerprint = (TextView) view.findViewById(R.id.text_fingerprint);
        view.findViewById(R.id.main).setBackgroundColor(_bgColor);
        return view;
    }

    public int getCryptType() {
        return _cryptType;
    }

    public Cipher getCipher(Slot slot) throws Exception {
        if (slot instanceof PasswordSlot) {
            char[] password = AuthHelper.getPassword(_textPassword, true);
            byte[] salt = CryptoUtils.generateSalt();
            SecretKey key = ((PasswordSlot)slot).deriveKey(password, salt, CryptoUtils.CRYPTO_SCRYPT_N, CryptoUtils.CRYPTO_SCRYPT_r, CryptoUtils.CRYPTO_SCRYPT_p);
            CryptoUtils.zero(password);
            return Slot.createCipher(key, Cipher.ENCRYPT_MODE);
        } else if (slot instanceof FingerprintSlot) {
            return _fingerCipher;
        } else {
            throw new RuntimeException();
        }
    }

    public void setBgColor(int color) {
        _bgColor = color;
    }

    @Override
    public void onSlideSelected() {
        Intent intent = getActivity().getIntent();
        _cryptType = intent.getIntExtra("cryptType", CustomAuthenticationSlide.CRYPT_TYPE_INVALID);

        switch(_cryptType) {
            case CustomAuthenticationSlide.CRYPT_TYPE_NONE:
            case CustomAuthenticationSlide.CRYPT_TYPE_PASS:
                break;
            case CustomAuthenticationSlide.CRYPT_TYPE_FINGER:
                _boxFingerprint.setVisibility(View.VISIBLE);

                SecretKey key;
                try {
                    if (_storeHandle == null) {
                        _storeHandle = new KeyStoreHandle();
                    }
                    // TODO: consider regenerating the key if it exists
                    if (!_storeHandle.keyExists()) {
                        key = _storeHandle.generateKey(true);
                    } else {
                        key = _storeHandle.getKey();
                    }
                } catch (Exception e) {
                    throw new UndeclaredThrowableException(e);
                }

                if (_fingerHelper == null) {
                    FingerprintManager fingerManager = (FingerprintManager) getContext().getSystemService(Context.FINGERPRINT_SERVICE);
                    _fingerHelper = new FingerprintUiHelper(fingerManager, _imgFingerprint, _textFingerprint, this);
                }

                try {
                    _fingerCipher = Slot.createCipher(key, Cipher.ENCRYPT_MODE);
                } catch (Exception e) {
                    throw new UndeclaredThrowableException(e);
                }
                _fingerHelper.startListening(new FingerprintManager.CryptoObject(_fingerCipher));
                break;
            default:
                throw new RuntimeException();
        }
    }

    @Override
    public void onSlideDeselected() {
        if (_fingerHelper != null) {
            _fingerAuthenticated = false;
            _boxFingerprint.setVisibility(View.INVISIBLE);
            _fingerHelper.stopListening();
        }
    }

    @Override
    public boolean isPolicyRespected() {
        switch(_cryptType) {
            case CustomAuthenticationSlide.CRYPT_TYPE_NONE:
                return true;
            case CustomAuthenticationSlide.CRYPT_TYPE_FINGER:
                if (!_fingerAuthenticated) {
                    return false;
                }
                // intentional fallthrough
            case CustomAuthenticationSlide.CRYPT_TYPE_PASS:
                return AuthHelper.arePasswordsEqual(_textPassword, _textPasswordConfirm);
            default:
                throw new RuntimeException();
        }
    }

    @Override
    public void onUserIllegallyRequestedNextPage() {
        String message;
        if (!AuthHelper.arePasswordsEqual(_textPassword, _textPasswordConfirm)) {
            message = "Passwords should be equal and non-empty";
        } else if (!_fingerAuthenticated) {
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
        _fingerAuthenticated = true;
    }

    @Override
    public void onError() {

    }
}
