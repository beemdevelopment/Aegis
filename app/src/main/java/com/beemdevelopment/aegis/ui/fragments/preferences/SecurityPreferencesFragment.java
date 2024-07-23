package com.beemdevelopment.aegis.ui.fragments.preferences;

import static android.text.TextUtils.isDigitsOnly;

import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.biometric.BiometricPrompt;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.beemdevelopment.aegis.PassReminderFreq;
import com.beemdevelopment.aegis.Preferences;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.crypto.KeyStoreHandle;
import com.beemdevelopment.aegis.crypto.KeyStoreHandleException;
import com.beemdevelopment.aegis.helpers.BiometricSlotInitializer;
import com.beemdevelopment.aegis.helpers.BiometricsHelper;
import com.beemdevelopment.aegis.ui.dialogs.Dialogs;
import com.beemdevelopment.aegis.ui.preferences.SwitchPreference;
import com.beemdevelopment.aegis.ui.tasks.PasswordSlotDecryptTask;
import com.beemdevelopment.aegis.vault.VaultFileCredentials;
import com.beemdevelopment.aegis.vault.VaultRepositoryException;
import com.beemdevelopment.aegis.vault.slots.BiometricSlot;
import com.beemdevelopment.aegis.vault.slots.PasswordSlot;
import com.beemdevelopment.aegis.vault.slots.Slot;
import com.beemdevelopment.aegis.vault.slots.SlotException;
import com.beemdevelopment.aegis.vault.slots.SlotList;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Arrays;
import java.util.List;

import javax.crypto.Cipher;

public class SecurityPreferencesFragment extends PreferencesFragment {
    private SwitchPreference _encryptionPreference;
    private SwitchPreference _biometricsPreference;
    private Preference _autoLockPreference;
    private Preference _setPasswordPreference;
    private Preference _passwordReminderPreference;
    private SwitchPreferenceCompat _pinKeyboardPreference;
    private SwitchPreference _backupPasswordPreference;
    private Preference _backupPasswordChangePreference;

    @Override
    public void onResume() {
        super.onResume();
        updateEncryptionPreferences();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_security);

        Preference screenPreference = requirePreference("pref_secure_screen");
        screenPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            Window window = requireActivity().getWindow();
            if ((boolean) newValue) {
                window.addFlags(WindowManager.LayoutParams.FLAG_SECURE);
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
            }
            return true;
        });

        Preference tapToRevealTimePreference = requirePreference("pref_tap_to_reveal_time");
        tapToRevealTimePreference.setSummary(_prefs.getTapToRevealTime() + " seconds");
        tapToRevealTimePreference.setOnPreferenceClickListener(preference -> {
            Dialogs.showTapToRevealTimeoutPickerDialog(requireContext(), _prefs.getTapToRevealTime(), number -> {
                _prefs.setTapToRevealTime(number);
                tapToRevealTimePreference.setSummary(number + " seconds");
            });
            return false;
        });

        _encryptionPreference = requirePreference("pref_encryption");
        _encryptionPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            if (!_vaultManager.getVault().isEncryptionEnabled()) {
                Dialogs.showSetPasswordDialog(requireActivity(), new EnableEncryptionListener());
            } else {
                Dialogs.showSecureDialog(new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_Aegis_AlertDialog_Warning)
                        .setTitle(R.string.disable_encryption)
                        .setMessage(getText(R.string.disable_encryption_description))
                        .setIconAttribute(android.R.attr.alertDialogIcon)
                        .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                            try {
                                _vaultManager.disableEncryption();
                            } catch (VaultRepositoryException e) {
                                e.printStackTrace();
                                Dialogs.showErrorDialog(requireContext(), R.string.disable_encryption_error, e);
                                return;
                            }

                            _prefs.setIsBackupsEnabled(false);
                            _prefs.setIsAndroidBackupsEnabled(false);
                            updateEncryptionPreferences();
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .create());
            }

            return false;
        });

        _biometricsPreference = requirePreference("pref_biometrics");
        _biometricsPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            VaultFileCredentials creds = _vaultManager.getVault().getCredentials();
            SlotList slots = creds.getSlots();

            if (!slots.has(BiometricSlot.class)) {
                if (BiometricsHelper.isAvailable(requireContext())) {
                    BiometricSlotInitializer initializer = new BiometricSlotInitializer(SecurityPreferencesFragment.this, new RegisterBiometricsListener());
                    BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                            .setTitle(getString(R.string.set_up_biometric))
                            .setNegativeButtonText(getString(android.R.string.cancel))
                            .build();
                    initializer.authenticate(info);
                }
            } else {
                // remove the biometric slot
                BiometricSlot slot = slots.find(BiometricSlot.class);
                slots.remove(slot);
                _vaultManager.getVault().setCredentials(creds);

                // remove the KeyStore key
                try {
                    KeyStoreHandle handle = new KeyStoreHandle();
                    handle.deleteKey(slot.getUUID().toString());
                } catch (KeyStoreHandleException e) {
                    e.printStackTrace();
                }

                saveAndBackupVault();
                updateEncryptionPreferences();
            }

            return false;
        });

        _setPasswordPreference = requirePreference("pref_password");
        _setPasswordPreference.setOnPreferenceClickListener(preference -> {
            Dialogs.showSetPasswordDialog(requireActivity(), new SetPasswordListener());
            return false;
        });

        _pinKeyboardPreference = requirePreference("pref_pin_keyboard");
        _pinKeyboardPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            if (!(boolean) newValue) {
                return true;
            }

            Dialogs.showPasswordInputDialog(requireContext(), R.string.set_password_confirm, R.string.pin_keyboard_description, password -> {
                if (isDigitsOnly(new String(password))) {
                    List<PasswordSlot> slots = _vaultManager.getVault().getCredentials().getSlots().findRegularPasswordSlots();
                    PasswordSlotDecryptTask.Params params = new PasswordSlotDecryptTask.Params(slots, password);
                    PasswordSlotDecryptTask task = new PasswordSlotDecryptTask(requireContext(), new PasswordConfirmationListener());
                    task.execute(getLifecycle(), params);
                } else {
                    _pinKeyboardPreference.setChecked(false);
                    Dialogs.showSecureDialog(new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_Aegis_AlertDialog_Error)
                            .setTitle(R.string.pin_keyboard_error)
                            .setMessage(R.string.pin_keyboard_error_description)
                            .setIconAttribute(android.R.attr.alertDialogIcon)
                            .setCancelable(false)
                            .setPositiveButton(android.R.string.ok, null)
                            .create());
                }
            }, dialog -> {
                _pinKeyboardPreference.setChecked(false);
            });
            return false;
        });

        _autoLockPreference = requirePreference("pref_auto_lock");
        _autoLockPreference.setSummary(getAutoLockSummary());
        _autoLockPreference.setOnPreferenceClickListener((preference) -> {
            final int[] items = Preferences.AUTO_LOCK_SETTINGS;
            final String[] textItems = getResources().getStringArray(R.array.pref_auto_lock_types);
            final boolean[] checkedItems = new boolean[items.length];
            for (int i = 0; i < items.length; i++) {
                checkedItems[i] = _prefs.isAutoLockTypeEnabled(items[i]);
            }

            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.pref_auto_lock_prompt)
                    .setMultiChoiceItems(textItems, checkedItems, (dialog, index, isChecked) -> checkedItems[index] = isChecked)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        int autoLock = Preferences.AUTO_LOCK_OFF;
                        for (int i = 0; i < checkedItems.length; i++) {
                            if (checkedItems[i]) {
                                autoLock |= items[i];
                            }
                        }

                        _prefs.setAutoLockMask(autoLock);
                        _autoLockPreference.setSummary(getAutoLockSummary());
                    })
                    .setNegativeButton(android.R.string.cancel, null);
            Dialogs.showSecureDialog(builder.create());

            return false;
        });

        _passwordReminderPreference = requirePreference("pref_password_reminder_freq");
        _passwordReminderPreference.setSummary(getPasswordReminderSummary());
        _passwordReminderPreference.setOnPreferenceClickListener((preference) -> {
            final PassReminderFreq currFreq = _prefs.getPasswordReminderFrequency();
            final PassReminderFreq[] items = PassReminderFreq.values();
            final String[] textItems = Arrays.stream(items)
                    .map(f -> getString(f.getStringRes()))
                    .toArray(String[]::new);

            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.pref_password_reminder_title)
                    .setSingleChoiceItems(textItems, currFreq.ordinal(), (dialog, which) -> {
                        int i = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                        PassReminderFreq freq = PassReminderFreq.fromInteger(i);
                        _prefs.setPasswordReminderFrequency(freq);
                        _passwordReminderPreference.setSummary(getPasswordReminderSummary());
                        dialog.dismiss();
                    })
                    .setNegativeButton(android.R.string.cancel, null);
            Dialogs.showSecureDialog(builder.create());
            return false;
        });

        _backupPasswordPreference = requirePreference("pref_backup_password");
        _backupPasswordPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            if (!_vaultManager.getVault().isBackupPasswordSet()) {
                Dialogs.showSetPasswordDialog(requireActivity(), new SetBackupPasswordListener());
            } else {
                VaultFileCredentials creds = _vaultManager.getVault().getCredentials();
                SlotList slots = creds.getSlots();
                for (Slot slot : slots.findBackupPasswordSlots()) {
                    slots.remove(slot);
                }
                _vaultManager.getVault().setCredentials(creds);

                saveAndBackupVault();
                updateEncryptionPreferences();
            }

            return false;
        });

        _backupPasswordChangePreference = requirePreference("pref_backup_password_change");
        _backupPasswordChangePreference.setOnPreferenceClickListener(preference -> {
            Dialogs.showSetPasswordDialog(requireActivity(), new SetBackupPasswordListener());
            return false;
        });
    }

    private void updateEncryptionPreferences() {
        boolean encrypted = _vaultManager.getVault().isEncryptionEnabled();
        boolean backupPasswordSet = _vaultManager.getVault().isBackupPasswordSet();
        _encryptionPreference.setChecked(encrypted, true);
        _setPasswordPreference.setVisible(encrypted);
        _biometricsPreference.setVisible(encrypted);
        _autoLockPreference.setVisible(encrypted);
        _pinKeyboardPreference.setVisible(encrypted);
        _backupPasswordPreference.getParent().setVisible(encrypted);
        _backupPasswordPreference.setChecked(backupPasswordSet, true);
        _backupPasswordChangePreference.setVisible(backupPasswordSet);

        if (encrypted) {
            SlotList slots = _vaultManager.getVault().getCredentials().getSlots();
            boolean multiBackupPassword = slots.findBackupPasswordSlots().size() > 1;
            boolean multiPassword = slots.findRegularPasswordSlots().size() > 1;
            boolean multiBio = slots.findAll(BiometricSlot.class).size() > 1;
            boolean canUseBio = BiometricsHelper.isAvailable(requireContext());
            _setPasswordPreference.setEnabled(!multiPassword);
            _biometricsPreference.setEnabled(canUseBio && !multiBio);
            _biometricsPreference.setChecked(slots.has(BiometricSlot.class), true);
            _passwordReminderPreference.setVisible(slots.has(BiometricSlot.class));
            _backupPasswordChangePreference.setEnabled(!multiBackupPassword);
        } else {
            _setPasswordPreference.setEnabled(false);
            _biometricsPreference.setEnabled(false);
            _biometricsPreference.setChecked(false, true);
            _passwordReminderPreference.setVisible(false);
            _backupPasswordChangePreference.setEnabled(false);
        }
    }

    private String getPasswordReminderSummary() {
        PassReminderFreq freq = _prefs.getPasswordReminderFrequency();
        if (freq == PassReminderFreq.NEVER) {
            return getString(R.string.pref_password_reminder_summary_disabled);
        }

        String freqString = getString(freq.getStringRes()).toLowerCase();
        return getString(R.string.pref_password_reminder_summary, freqString);
    }

    private String getAutoLockSummary() {
        final int[] settings = Preferences.AUTO_LOCK_SETTINGS;
        final String[] descriptions = getResources().getStringArray(R.array.pref_auto_lock_types);

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < settings.length; i++) {
            if (_prefs.isAutoLockTypeEnabled(settings[i])) {
                if (builder.length() != 0) {
                    builder.append(", ");
                }

                builder.append(descriptions[i].toLowerCase());
            }
        }

        if (builder.length() == 0) {
            return getString(R.string.pref_auto_lock_summary_disabled);
        }

        return getString(R.string.pref_auto_lock_summary, builder.toString());
    }

    private class SetPasswordListener implements Dialogs.PasswordSlotListener {
        @Override
        public void onSlotResult(PasswordSlot slot, Cipher cipher) {
            VaultFileCredentials creds = _vaultManager.getVault().getCredentials();
            SlotList slots = creds.getSlots();

            try {
                // encrypt the master key for this slot
                slot.setKey(creds.getKey(), cipher);

                // remove the old master password slot
                List<PasswordSlot> passSlots = creds.getSlots().findRegularPasswordSlots();
                if (passSlots.size() != 0) {
                    slots.remove(passSlots.get(0));
                }

                // add the new master password slot
                slots.add(slot);
            } catch (SlotException e) {
                onException(e);
                return;
            }

            _vaultManager.getVault().setCredentials(creds);
            saveAndBackupVault();

            if (_prefs.isPinKeyboardEnabled()) {
                _pinKeyboardPreference.setChecked(false);
                Toast.makeText(requireContext(), R.string.pin_keyboard_disabled, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onException(Exception e) {
            e.printStackTrace();
            updateEncryptionPreferences();
            Dialogs.showErrorDialog(requireContext(), R.string.encryption_set_password_error, e);
        }
    }

    private class SetBackupPasswordListener implements Dialogs.PasswordSlotListener {
        @Override
        public void onSlotResult(PasswordSlot slot, Cipher cipher) {
            slot.setIsBackup(true);

            VaultFileCredentials creds = _vaultManager.getVault().getCredentials();
            SlotList slots = creds.getSlots();

            try {
                // encrypt the master key for this slot
                slot.setKey(creds.getKey(), cipher);

                // remove the old backup password slot
                for (Slot oldSlot : slots.findBackupPasswordSlots()) {
                    slots.remove(oldSlot);
                }

                // add the new backup password slot
                slots.add(slot);
            } catch (SlotException e) {
                onException(e);
                return;
            }

            _vaultManager.getVault().setCredentials(creds);
            saveAndBackupVault();
            updateEncryptionPreferences();
        }

        @Override
        public void onException(Exception e) {
            e.printStackTrace();
            updateEncryptionPreferences();
            Dialogs.showErrorDialog(requireContext(), R.string.encryption_set_password_error, e);
        }
    }

    private class RegisterBiometricsListener implements BiometricSlotInitializer.Listener {
        @Override
        public void onInitializeSlot(BiometricSlot slot, Cipher cipher) {
            VaultFileCredentials creds = _vaultManager.getVault().getCredentials();
            try {
                slot.setKey(creds.getKey(), cipher);
            } catch (SlotException e) {
                e.printStackTrace();
                onSlotInitializationFailed(0, e.toString());
                return;
            }
            creds.getSlots().add(slot);
            _vaultManager.getVault().setCredentials(creds);

            saveAndBackupVault();
            updateEncryptionPreferences();
        }

        @Override
        public void onSlotInitializationFailed(int errorCode, @NonNull CharSequence errString) {
            if (!BiometricsHelper.isCanceled(errorCode)) {
                Dialogs.showErrorDialog(requireContext(), R.string.encryption_enable_biometrics_error, errString);
            }
        }
    }

    private class EnableEncryptionListener implements Dialogs.PasswordSlotListener {
        @Override
        public void onSlotResult(PasswordSlot slot, Cipher cipher) {
            VaultFileCredentials creds = new VaultFileCredentials();

            try {
                slot.setKey(creds.getKey(), cipher);
                creds.getSlots().add(slot);
                _vaultManager.enableEncryption(creds);
            } catch (VaultRepositoryException | SlotException e) {
                onException(e);
                return;
            }

            _pinKeyboardPreference.setChecked(false);
            updateEncryptionPreferences();
        }

        @Override
        public void onException(Exception e) {
            e.printStackTrace();
            Dialogs.showErrorDialog(requireContext(), R.string.encryption_set_password_error, e);
        }
    }

    private class PasswordConfirmationListener implements PasswordSlotDecryptTask.Callback {
        @Override
        public void onTaskFinished(PasswordSlotDecryptTask.Result result) {
            if (result != null) {
                _pinKeyboardPreference.setChecked(true);
            } else {
                Dialogs.showSecureDialog(new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_Aegis_AlertDialog_Error)
                        .setTitle(R.string.pin_keyboard_error)
                        .setMessage(R.string.invalid_password)
                        .setIconAttribute(android.R.attr.alertDialogIcon)
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok, null)
                        .create());
                _pinKeyboardPreference.setChecked(false);
            }
        }
    }
}
