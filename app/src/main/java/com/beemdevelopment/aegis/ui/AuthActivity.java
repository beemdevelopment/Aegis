package com.beemdevelopment.aegis.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.biometric.BiometricPrompt;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.crypto.KeyStoreHandle;
import com.beemdevelopment.aegis.crypto.KeyStoreHandleException;
import com.beemdevelopment.aegis.crypto.MasterKey;
import com.beemdevelopment.aegis.helpers.BiometricsHelper;
import com.beemdevelopment.aegis.helpers.EditTextHelper;
import com.beemdevelopment.aegis.helpers.MetricsHelper;
import com.beemdevelopment.aegis.helpers.UiThreadExecutor;
import com.beemdevelopment.aegis.ui.dialogs.Dialogs;
import com.beemdevelopment.aegis.ui.tasks.PasswordSlotDecryptTask;
import com.beemdevelopment.aegis.vault.VaultFile;
import com.beemdevelopment.aegis.vault.VaultFileCredentials;
import com.beemdevelopment.aegis.vault.VaultRepository;
import com.beemdevelopment.aegis.vault.VaultRepositoryException;
import com.beemdevelopment.aegis.vault.slots.BiometricSlot;
import com.beemdevelopment.aegis.vault.slots.PasswordSlot;
import com.beemdevelopment.aegis.vault.slots.Slot;
import com.beemdevelopment.aegis.vault.slots.SlotException;
import com.beemdevelopment.aegis.vault.slots.SlotIntegrityException;
import com.beemdevelopment.aegis.vault.slots.SlotList;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

public class AuthActivity extends AegisActivity {
    // Permission request codes
    private static final int CODE_PERM_NOTIFICATIONS = 0;

    private EditText _textPassword;

    private VaultFile _vaultFile;
    private SlotList _slots;

    private SecretKey _bioKey;
    private BiometricSlot _bioSlot;
    private BiometricPrompt _bioPrompt;
    private Button _decryptButton;

    private int _failedUnlockAttempts;

    // the first time this activity is resumed after creation, it's possible to inhibit showing the
    // biometric prompt by setting 'inhibitBioPrompt' to true through the intent
    private boolean _inhibitBioPrompt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
        _textPassword = findViewById(R.id.text_password);
        LinearLayout boxBiometricInfo = findViewById(R.id.box_biometric_info);
        _decryptButton = findViewById(R.id.button_decrypt);
        TextView biometricsButton = findViewById(R.id.button_biometrics);

        getOnBackPressedDispatcher().addCallback(this, new BackPressHandler());

        _textPassword.setOnEditorActionListener((v, actionId, event) -> {
            if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                _decryptButton.performClick();
            }
            return false;
        });

        if (_prefs.isPinKeyboardEnabled()) {
            _textPassword.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        }

        Intent intent = getIntent();
        if (savedInstanceState == null) {
            _inhibitBioPrompt = intent.getBooleanExtra("inhibitBioPrompt", false);

            // A persistent notification is shown to let the user know that the vault is unlocked. Permission
            // to do so is required since API 33, so for existing users, we have to request permission here
            // in order to be able to show the notification after unlock.
            //
            // NOTE: Disabled for now. See issue: #1047
            /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PermissionHelper.request(this, CODE_PERM_NOTIFICATIONS, Manifest.permission.POST_NOTIFICATIONS);
            }*/
        } else {
            _inhibitBioPrompt = savedInstanceState.getBoolean("inhibitBioPrompt", false);
        }

        try {
            _vaultFile = VaultRepository.readVaultFile(this);
        } catch (VaultRepositoryException e) {
            Dialogs.showErrorDialog(this, R.string.vault_load_error, e, (dialog, which) -> {
                getOnBackPressedDispatcher().onBackPressed();
            });
            return;
        }

        // only show the biometric prompt if the api version is new enough, permission is granted, a scanner is found and a biometric slot is found
        _slots = _vaultFile.getHeader().getSlots();
        if (_slots.has(BiometricSlot.class) && BiometricsHelper.isAvailable(this)) {
            boolean invalidated = false;

            try {
                // find a biometric slot with an id that matches an alias in the keystore
                for (BiometricSlot slot : _slots.findAll(BiometricSlot.class)) {
                    String id = slot.getUUID().toString();
                    KeyStoreHandle handle = new KeyStoreHandle();
                    if (handle.containsKey(id)) {
                        SecretKey key = handle.getKey(id);
                        // if 'key' is null, it was permanently invalidated
                        if (key == null) {
                            invalidated = true;
                            continue;
                        }

                        _bioSlot = slot;
                        _bioKey = key;
                        biometricsButton.setVisibility(View.VISIBLE);
                        invalidated = false;
                        break;
                    }
                }
            } catch (KeyStoreHandleException e) {
                e.printStackTrace();
                Dialogs.showErrorDialog(this, R.string.biometric_init_error, e);
            }

            // display a help message if a matching invalidated keystore entry was found
            if (invalidated) {
                boxBiometricInfo.setVisibility(View.VISIBLE);
                biometricsButton.setVisibility(View.GONE);
            }
        }

        _decryptButton.setOnClickListener(v -> {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

            char[] password = EditTextHelper.getEditTextChars(_textPassword);
            List<PasswordSlot> slots = _slots.findAll(PasswordSlot.class);
            PasswordSlotDecryptTask.Params params = new PasswordSlotDecryptTask.Params(slots, password);
            PasswordSlotDecryptTask task = new PasswordSlotDecryptTask(AuthActivity.this, new PasswordDerivationListener());
            task.execute(getLifecycle(), params);

            _decryptButton.setEnabled(false);
        });

        biometricsButton.setOnClickListener(v -> {
            if (_prefs.isPasswordReminderNeeded()) {
                Dialogs.showSecureDialog(new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Aegis_AlertDialog_Warning)
                        .setTitle(getString(R.string.password_reminder_dialog_title))
                        .setMessage(getString(R.string.password_reminder_dialog_message))
                        .setCancelable(false)
                        .setIconAttribute(android.R.attr.alertDialogIcon)
                        .setPositiveButton(android.R.string.ok, (dialog1, which) -> {
                            showBiometricPrompt();
                        })
                        .create());
            } else {
                showBiometricPrompt();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("inhibitBioPrompt", _inhibitBioPrompt);
    }

    private void selectPassword() {
        _textPassword.selectAll();

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    @Override
    public void onResume() {
        super.onResume();

        boolean remindPassword = _prefs.isPasswordReminderNeeded();
        if (_bioKey == null || remindPassword) {
            focusPasswordField();
        }

        if (_bioKey != null && _bioPrompt == null && !_inhibitBioPrompt && !remindPassword) {
            _bioPrompt = showBiometricPrompt();
        }

        _inhibitBioPrompt = false;
    }

    @Override
    public void onPause() {
        if (!isChangingConfigurations() && _bioPrompt != null) {
            _bioPrompt.cancelAuthentication();
            _bioPrompt = null;
        }

        super.onPause();
    }

    @Override
    public void onAttachedToWindow() {
        if (_bioKey != null && _prefs.isPasswordReminderNeeded()) {
            showPasswordReminder();
        }
    }

    private void focusPasswordField() {
        _textPassword.requestFocus();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    private void showPasswordReminder() {
        View popupLayout = getLayoutInflater().inflate(R.layout.popup_password, null);
        popupLayout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

        PopupWindow popup = new PopupWindow(popupLayout, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        popup.setFocusable(false);
        popup.setOutsideTouchable(true);
        _textPassword.post(() -> {
            if (isFinishing() || !_textPassword.isAttachedToWindow()) {
                return;
            }

            // calculating the actual height of the popup window does not seem possible
            // adding 25dp seems to look good enough
            int yoff = _textPassword.getHeight()
                    + popupLayout.getMeasuredHeight()
                    + MetricsHelper.convertDpToPixels(this, 25);
            popup.showAsDropDown(_textPassword, 0, -yoff);
        });
        _textPassword.postDelayed(popup::dismiss, 5000);
    }

    public BiometricPrompt showBiometricPrompt() {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(_textPassword.getWindowToken(), 0);

        Cipher cipher;
        try {
            cipher = _bioSlot.createDecryptCipher(_bioKey);
        } catch (SlotException e) {
            e.printStackTrace();
            Dialogs.showErrorDialog(this, R.string.biometric_init_error, e);
            return null;
        }

        BiometricPrompt.CryptoObject cryptoObj = new BiometricPrompt.CryptoObject(cipher);
        BiometricPrompt prompt = new BiometricPrompt(this, new UiThreadExecutor(), new BiometricPromptListener());

        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.authentication))
                .setNegativeButtonText(getString(android.R.string.cancel))
                .setConfirmationRequired(false)
                .build();
        prompt.authenticate(info, cryptoObj);
        return prompt;
    }

    private void finish(MasterKey key, boolean isSlotRepaired) {
        VaultFileCredentials creds = new VaultFileCredentials(key, _slots);

        try {
            _vaultManager.loadFrom(_vaultFile, creds);
            if (isSlotRepaired) {
                saveAndBackupVault();
            }
        } catch (VaultRepositoryException e) {
            e.printStackTrace();
            Dialogs.showErrorDialog(this, R.string.decryption_corrupt_error, e);
            return;
        }

        setResult(RESULT_OK);
        finish();
    }

    private void onInvalidPassword() {
        Dialogs.showSecureDialog(new MaterialAlertDialogBuilder(AuthActivity.this, R.style.ThemeOverlay_Aegis_AlertDialog_Error)
                .setTitle(getString(R.string.unlock_vault_error))
                .setMessage(getString(R.string.unlock_vault_error_description))
                .setCancelable(false)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> selectPassword())
                .create());

        _failedUnlockAttempts ++;

        if (_failedUnlockAttempts >= 3) {
            _textPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
    }

    private class BackPressHandler extends OnBackPressedCallback {
        public BackPressHandler() {
            super(true);
        }

        @Override
        public void handleOnBackPressed() {
            // This breaks predictive back gestures, but it doesn't make sense
            // to go back to MainActivity when cancelling auth
            setResult(RESULT_CANCELED);
            finishAffinity();
        }
    }

    private class PasswordDerivationListener implements PasswordSlotDecryptTask.Callback {
        @Override
        public void onTaskFinished(PasswordSlotDecryptTask.Result result) {
            if (result != null) {
                // replace the old slot with the repaired one
                if (result.isSlotRepaired()) {
                    _slots.replace(result.getSlot());
                }

                if (result.getSlot().getType() == Slot.TYPE_PASSWORD) {
                    _prefs.resetPasswordReminderTimestamp();
                }

                finish(result.getKey(), result.isSlotRepaired());
            } else {
                _decryptButton.setEnabled(true);

                _auditLogRepository.addVaultUnlockFailedPasswordEvent();
                onInvalidPassword();
            }
        }
    }

    private class BiometricPromptListener extends BiometricPrompt.AuthenticationCallback {
        @Override
        public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
            super.onAuthenticationError(errorCode, errString);
            _bioPrompt = null;

            if (!BiometricsHelper.isCanceled(errorCode)) {
                _auditLogRepository.addVaultUnlockFailedBiometricsEvent();
                Toast.makeText(AuthActivity.this, errString, Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
            super.onAuthenticationSucceeded(result);
            _bioPrompt = null;

            MasterKey key;
            BiometricSlot slot = _slots.find(BiometricSlot.class);

            try {
                key = slot.getKey(result.getCryptoObject().getCipher());
            } catch (SlotException | SlotIntegrityException e) {
                e.printStackTrace();
                Dialogs.showErrorDialog(AuthActivity.this, R.string.biometric_decrypt_error, e);
                return;
            }

            finish(key, false);
        }

        @Override
        public void onAuthenticationFailed() {
            super.onAuthenticationFailed();
        }
    }
}
