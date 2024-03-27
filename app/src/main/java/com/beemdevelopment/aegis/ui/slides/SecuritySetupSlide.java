package com.beemdevelopment.aegis.ui.slides;

import static com.beemdevelopment.aegis.ui.slides.SecurityPickerSlide.CRYPT_TYPE_BIOMETRIC;
import static com.beemdevelopment.aegis.ui.slides.SecurityPickerSlide.CRYPT_TYPE_INVALID;
import static com.beemdevelopment.aegis.ui.slides.SecurityPickerSlide.CRYPT_TYPE_NONE;
import static com.beemdevelopment.aegis.ui.slides.SecurityPickerSlide.CRYPT_TYPE_PASS;

import android.os.Bundle;
import android.text.Editable;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricPrompt;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.helpers.BiometricSlotInitializer;
import com.beemdevelopment.aegis.helpers.BiometricsHelper;
import com.beemdevelopment.aegis.helpers.EditTextHelper;
import com.beemdevelopment.aegis.helpers.PasswordStrengthHelper;
import com.beemdevelopment.aegis.helpers.SimpleTextWatcher;
import com.beemdevelopment.aegis.ui.dialogs.Dialogs;
import com.beemdevelopment.aegis.ui.intro.SlideFragment;
import com.beemdevelopment.aegis.ui.tasks.KeyDerivationTask;
import com.beemdevelopment.aegis.vault.VaultFileCredentials;
import com.beemdevelopment.aegis.vault.slots.BiometricSlot;
import com.beemdevelopment.aegis.vault.slots.PasswordSlot;
import com.beemdevelopment.aegis.vault.slots.Slot;
import com.beemdevelopment.aegis.vault.slots.SlotException;
import com.google.android.material.textfield.TextInputLayout;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

public class SecuritySetupSlide extends SlideFragment {
    private EditText _textPassword;
    private EditText _textPasswordConfirm;
    private CheckBox _checkPasswordVisibility;
    private ProgressBar _barPasswordStrength;
    private TextView _textPasswordStrength;
    private TextInputLayout _textPasswordWrapper;

    private int _cryptType;
    private VaultFileCredentials _creds;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_security_setup_slide, container, false);
        _textPassword = view.findViewById(R.id.text_password);
        _textPasswordConfirm = view.findViewById(R.id.text_password_confirm);
        _checkPasswordVisibility = view.findViewById(R.id.check_toggle_visibility);
        _barPasswordStrength = view.findViewById(R.id.progressBar);
        _textPasswordStrength = view.findViewById(R.id.text_password_strength);
        _textPasswordWrapper = view.findViewById(R.id.text_password_wrapper);

        _checkPasswordVisibility.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                _textPassword.setTransformationMethod(null);
                _textPasswordConfirm.setTransformationMethod(null);
                _textPassword.clearFocus();
                _textPasswordConfirm.clearFocus();
            } else {
                _textPassword.setTransformationMethod(new PasswordTransformationMethod());
                _textPasswordConfirm.setTransformationMethod(new PasswordTransformationMethod());
            }
        });

        _textPassword.addTextChangedListener(new SimpleTextWatcher(new SimpleTextWatcher.Listener() {
            private final PasswordStrengthHelper passStrength = new PasswordStrengthHelper(
                    _textPassword, _barPasswordStrength, _textPasswordStrength, _textPasswordWrapper);

            @Override
            public void afterTextChanged(Editable s) {
                passStrength.measure(requireContext());
            }
        }));

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        _cryptType = getState().getInt("cryptType", CRYPT_TYPE_INVALID);
        if (_cryptType == CRYPT_TYPE_INVALID || _cryptType == CRYPT_TYPE_NONE) {
            throw new RuntimeException(String.format("State of SecuritySetupSlide not properly propagated, cryptType: %d", _cryptType));
        }

        _creds = new VaultFileCredentials();
    }

    private void showBiometricPrompt() {
        BiometricSlotInitializer initializer = new BiometricSlotInitializer(this, new BiometricsListener());
        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.set_up_biometric))
                .setNegativeButtonText(getString(android.R.string.cancel))
                .build();
        initializer.authenticate(info);
    }

    private void deriveKey() {
        PasswordSlot slot = new PasswordSlot();
        KeyDerivationTask.Params params = new KeyDerivationTask.Params(slot, EditTextHelper.getEditTextChars(_textPassword));
        KeyDerivationTask task = new KeyDerivationTask(requireContext(), new PasswordDerivationListener());
        task.execute(getLifecycle(), params);
    }

    @Override
    public boolean isFinished() {
        switch (_cryptType) {
            case CRYPT_TYPE_NONE:
                return true;
            case CRYPT_TYPE_BIOMETRIC:
                if (!_creds.getSlots().has(BiometricSlot.class)) {
                    return false;
                }
                // intentional fallthrough
            case CRYPT_TYPE_PASS:
                if (EditTextHelper.areEditTextsEqual(_textPassword, _textPasswordConfirm)) {
                    return _creds.getSlots().has(PasswordSlot.class);
                }

                return false;
            default:
                return false;
        }
    }

    @Override
    public void onNotFinishedError() {
        if (!EditTextHelper.areEditTextsEqual(_textPassword, _textPasswordConfirm)) {
            Toast.makeText(requireContext(), R.string.password_equality_error, Toast.LENGTH_SHORT).show();
        } else if (_cryptType != SecurityPickerSlide.CRYPT_TYPE_BIOMETRIC) {
            deriveKey();
        } else if (!_creds.getSlots().has(BiometricSlot.class)) {
            showBiometricPrompt();
        }
    }

    @Override
    public void onSaveIntroState(@NonNull Bundle introState) {
        introState.putSerializable("creds", _creds);
    }

    private class PasswordDerivationListener implements KeyDerivationTask.Callback {
        @Override
        public void onTaskFinished(PasswordSlot slot, SecretKey key) {
            try {
                Cipher cipher = Slot.createEncryptCipher(key);
                slot.setKey(_creds.getKey(), cipher);
                _creds.getSlots().add(slot);
            } catch (SlotException e) {
                e.printStackTrace();
                Dialogs.showErrorDialog(requireContext(), R.string.enable_encryption_error, e);
                return;
            }

            goToNextSlide();
        }
    }

    private class BiometricsListener implements BiometricSlotInitializer.Listener {
        @Override
        public void onInitializeSlot(BiometricSlot slot, Cipher cipher) {
            try {
                slot.setKey(_creds.getKey(), cipher);
                _creds.getSlots().add(slot);
            } catch (SlotException e) {
                e.printStackTrace();
                onSlotInitializationFailed(0, e.toString());
                return;
            }

            deriveKey();
        }

        @Override
        public void onSlotInitializationFailed(int errorCode, @NonNull CharSequence errString) {
            if (!BiometricsHelper.isCanceled(errorCode)) {
                Dialogs.showErrorDialog(requireContext(), R.string.encryption_enable_biometrics_error, errString);
            }
        }
    }
}
