package com.beemdevelopment.aegis.ui.fragments;

import static android.text.TextUtils.isDigitsOnly;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.biometric.BiometricPrompt;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.beemdevelopment.aegis.BuildConfig;
import com.beemdevelopment.aegis.PassReminderFreq;
import com.beemdevelopment.aegis.Preferences;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.crypto.KeyStoreHandle;
import com.beemdevelopment.aegis.crypto.KeyStoreHandleException;
import com.beemdevelopment.aegis.helpers.BiometricSlotInitializer;
import com.beemdevelopment.aegis.helpers.BiometricsHelper;
import com.beemdevelopment.aegis.services.NotificationService;
import com.beemdevelopment.aegis.ui.SlotManagerActivity;
import com.beemdevelopment.aegis.ui.dialogs.Dialogs;
import com.beemdevelopment.aegis.ui.preferences.SwitchPreference;
import com.beemdevelopment.aegis.ui.tasks.PasswordSlotDecryptTask;
import com.beemdevelopment.aegis.vault.VaultFileCredentials;
import com.beemdevelopment.aegis.vault.VaultManagerException;
import com.beemdevelopment.aegis.vault.slots.BiometricSlot;
import com.beemdevelopment.aegis.vault.slots.PasswordSlot;
import com.beemdevelopment.aegis.vault.slots.Slot;
import com.beemdevelopment.aegis.vault.slots.SlotException;
import com.beemdevelopment.aegis.vault.slots.SlotList;

import java.util.Arrays;
import java.util.List;

import javax.crypto.Cipher;

public class SecurityPreferencesFragment extends PreferencesFragment {
    private SwitchPreference _encryptionPreference;
    private SwitchPreference _biometricsPreference;
    private Preference _autoLockPreference;
    private Preference _setPasswordPreference;
    private Preference _slotsPreference;
    private Preference _passwordReminderPreference;
    private SwitchPreferenceCompat _pinKeyboardPreference;

    @Override
    public void onResume() {
        super.onResume();
        updateEncryptionPreferences();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        addPreferencesFromResource(R.xml.preferences_security);

        Preference tapToRevealPreference = findPreference("pref_tap_to_reveal");
        tapToRevealPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            getResult().putExtra("needsRefresh", true);
            return true;
        });

        Preference screenPreference = findPreference("pref_secure_screen");
        screenPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            getResult().putExtra("needsRecreate", true);
            Window window = getActivity().getWindow();
            if ((boolean) newValue) {
                window.addFlags(WindowManager.LayoutParams.FLAG_SECURE);
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
            }
            return true;
        });

        Preference tapToRevealTimePreference = findPreference("pref_tap_to_reveal_time");
        tapToRevealTimePreference.setSummary(getPreferences().getTapToRevealTime() + " seconds");
        tapToRevealTimePreference.setOnPreferenceClickListener(preference -> {
            Dialogs.showNumberPickerDialog(getActivity(), number -> {
                getPreferences().setTapToRevealTime(number);
                tapToRevealTimePreference.setSummary(number + " seconds");
                getResult().putExtra("needsRefresh", true);
            });
            return false;
        });

        _encryptionPreference = findPreference("pref_encryption");
        _encryptionPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            if (!getVault().isEncryptionEnabled()) {
                Dialogs.showSetPasswordDialog(getActivity(), new EnableEncryptionListener());
            } else {
                Dialogs.showSecureDialog(new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.disable_encryption)
                        .setMessage(getText(R.string.disable_encryption_description))
                        .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                            try {
                                getVault().disableEncryption();
                            } catch (VaultManagerException e) {
                                e.printStackTrace();
                                Dialogs.showErrorDialog(getContext(), R.string.disable_encryption_error, e);
                                return;
                            }

                            // clear the KeyStore
                            try {
                                KeyStoreHandle handle = new KeyStoreHandle();
                                handle.clear();
                            } catch (KeyStoreHandleException e) {
                                e.printStackTrace();
                            }

                            getActivity().stopService(new Intent(getActivity(), NotificationService.class));
                            getPreferences().setIsBackupsEnabled(false);
                            getPreferences().setIsAndroidBackupsEnabled(false);
                            updateEncryptionPreferences();
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .create());
            }

            return false;
        });

        _biometricsPreference = findPreference("pref_biometrics");
        _biometricsPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            VaultFileCredentials creds = getVault().getCredentials();
            SlotList slots = creds.getSlots();

            if (!slots.has(BiometricSlot.class)) {
                if (BiometricsHelper.isAvailable(getContext())) {
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
                getVault().setCredentials(creds);

                // remove the KeyStore key
                try {
                    KeyStoreHandle handle = new KeyStoreHandle();
                    handle.deleteKey(slot.getUUID().toString());
                } catch (KeyStoreHandleException e) {
                    e.printStackTrace();
                }

                saveVault();
                updateEncryptionPreferences();
            }

            return false;
        });

        _setPasswordPreference = findPreference("pref_password");
        _setPasswordPreference.setOnPreferenceClickListener(preference -> {
            Dialogs.showSetPasswordDialog(getActivity(), new SetPasswordListener());
            return false;
        });

        _slotsPreference = findPreference("pref_slots");
        _slotsPreference.setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), SlotManagerActivity.class);
            intent.putExtra("creds", getVault().getCredentials());
            startActivityForResult(intent, CODE_SLOTS);
            return true;
        });

        _pinKeyboardPreference = findPreference("pref_pin_keyboard");
        _pinKeyboardPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            if (!(boolean) newValue) {
                return true;
            }

            Dialogs.showPasswordInputDialog(getActivity(), R.string.set_password_confirm, R.string.pin_keyboard_description, password -> {
                if (isDigitsOnly(new String(password))) {
                    List<PasswordSlot> slots = getVault().getCredentials().getSlots().findAll(PasswordSlot.class);
                    PasswordSlotDecryptTask.Params params = new PasswordSlotDecryptTask.Params(slots, password);
                    PasswordSlotDecryptTask task = new PasswordSlotDecryptTask(getActivity(), new PasswordConfirmationListener());
                    task.execute(getLifecycle(), params);
                } else {
                    _pinKeyboardPreference.setChecked(false);
                    Dialogs.showSecureDialog(new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.pin_keyboard_error)
                            .setMessage(R.string.pin_keyboard_error_description)
                            .setCancelable(false)
                            .setPositiveButton(android.R.string.ok, null)
                            .create());
                }
            }, dialog -> {
                _pinKeyboardPreference.setChecked(false);
            });
            return false;
        });

        _autoLockPreference = findPreference("pref_auto_lock");
        _autoLockPreference.setSummary(getAutoLockSummary());
        _autoLockPreference.setOnPreferenceClickListener((preference) -> {
            final int[] items = Preferences.AUTO_LOCK_SETTINGS;
            final String[] textItems = getResources().getStringArray(R.array.pref_auto_lock_types);
            final boolean[] checkedItems = new boolean[items.length];
            for (int i = 0; i < items.length; i++) {
                checkedItems[i] = getPreferences().isAutoLockTypeEnabled(items[i]);
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.pref_auto_lock_prompt)
                    .setMultiChoiceItems(textItems, checkedItems, (dialog, index, isChecked) -> checkedItems[index] = isChecked)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        int autoLock = Preferences.AUTO_LOCK_OFF;
                        for (int i = 0; i < checkedItems.length; i++) {
                            if (checkedItems[i]) {
                                autoLock |= items[i];
                            }
                        }

                        getPreferences().setAutoLockMask(autoLock);
                        _autoLockPreference.setSummary(getAutoLockSummary());
                    })
                    .setNegativeButton(android.R.string.cancel, null);
            Dialogs.showSecureDialog(builder.create());

            return false;
        });

        _passwordReminderPreference = findPreference("pref_password_reminder_freq");
        _passwordReminderPreference.setSummary(getPasswordReminderSummary());
        _passwordReminderPreference.setOnPreferenceClickListener((preference) -> {
            final PassReminderFreq currFreq = getPreferences().getPasswordReminderFrequency();
            final PassReminderFreq[] items = PassReminderFreq.values();
            final String[] textItems = Arrays.stream(items)
                    .map(f -> getString(f.getStringRes()))
                    .toArray(String[]::new);

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.pref_password_reminder_title)
                    .setSingleChoiceItems(textItems, currFreq.ordinal(), (dialog, which) -> {
                        int i = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                        PassReminderFreq freq = PassReminderFreq.fromInteger(i);
                        getPreferences().setPasswordReminderFrequency(freq);
                        _passwordReminderPreference.setSummary(getPasswordReminderSummary());
                        dialog.dismiss();
                    })
                    .setNegativeButton(android.R.string.cancel, null);
            Dialogs.showSecureDialog(builder.create());
            return false;
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data != null && requestCode == CODE_SLOTS) {
            onSlotManagerResult(resultCode, data);
        }
    }

    private void onSlotManagerResult(int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        VaultFileCredentials creds = (VaultFileCredentials) data.getSerializableExtra("creds");
        getVault().setCredentials(creds);
        saveVault();
        updateEncryptionPreferences();
    }

    private void updateEncryptionPreferences() {
        boolean encrypted = getVault().isEncryptionEnabled();
        _encryptionPreference.setChecked(encrypted, true);
        _setPasswordPreference.setVisible(encrypted);
        _biometricsPreference.setVisible(encrypted);
        _slotsPreference.setEnabled(encrypted);
        _autoLockPreference.setVisible(encrypted);
        _pinKeyboardPreference.setVisible(encrypted);

        if (encrypted) {
            SlotList slots = getVault().getCredentials().getSlots();
            boolean multiPassword = slots.findAll(PasswordSlot.class).size() > 1;
            boolean multiBio = slots.findAll(BiometricSlot.class).size() > 1;
            boolean showSlots = BuildConfig.DEBUG || multiPassword || multiBio;
            boolean canUseBio = BiometricsHelper.isAvailable(getContext());
            _setPasswordPreference.setEnabled(!multiPassword);
            _biometricsPreference.setEnabled(canUseBio && !multiBio);
            _biometricsPreference.setChecked(slots.has(BiometricSlot.class), true);
            _slotsPreference.setVisible(showSlots);
            _passwordReminderPreference.setVisible(slots.has(BiometricSlot.class));
        } else {
            _setPasswordPreference.setEnabled(false);
            _biometricsPreference.setEnabled(false);
            _biometricsPreference.setChecked(false, true);
            _slotsPreference.setVisible(false);
            _passwordReminderPreference.setVisible(false);
        }
    }

    private String getPasswordReminderSummary() {
        PassReminderFreq freq = getPreferences().getPasswordReminderFrequency();
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
            if (getPreferences().isAutoLockTypeEnabled(settings[i])) {
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

    private class SetPasswordListener implements Dialogs.SlotListener {
        @Override
        public void onSlotResult(Slot slot, Cipher cipher) {
            VaultFileCredentials creds = getVault().getCredentials();
            SlotList slots = creds.getSlots();

            try {
                // encrypt the master key for this slot
                slot.setKey(creds.getKey(), cipher);

                // remove the old master password slot
                PasswordSlot oldSlot = creds.getSlots().find(PasswordSlot.class);
                if (oldSlot != null) {
                    slots.remove(oldSlot);
                }

                // add the new master password slot
                slots.add(slot);
            } catch (SlotException e) {
                onException(e);
                return;
            }

            getVault().setCredentials(creds);
            saveVault();

            if (getPreferences().isPinKeyboardEnabled()) {
                _pinKeyboardPreference.setChecked(false);
                Toast.makeText(getContext(), R.string.pin_keyboard_disabled, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onException(Exception e) {
            e.printStackTrace();
            updateEncryptionPreferences();
            Dialogs.showErrorDialog(getContext(), R.string.encryption_set_password_error, e);
        }
    }

    private class RegisterBiometricsListener implements BiometricSlotInitializer.Listener {
        @Override
        public void onInitializeSlot(BiometricSlot slot, Cipher cipher) {
            VaultFileCredentials creds = getVault().getCredentials();
            try {
                slot.setKey(creds.getKey(), cipher);
            } catch (SlotException e) {
                e.printStackTrace();
                onSlotInitializationFailed(0, e.toString());
                return;
            }
            creds.getSlots().add(slot);
            getVault().setCredentials(creds);

            saveVault();
            updateEncryptionPreferences();
        }

        @Override
        public void onSlotInitializationFailed(int errorCode, @NonNull CharSequence errString) {
            if (!BiometricsHelper.isCanceled(errorCode)) {
                Dialogs.showErrorDialog(getContext(), R.string.encryption_enable_biometrics_error, errString);
            }
        }
    }

    private class EnableEncryptionListener implements Dialogs.SlotListener {
        @Override
        public void onSlotResult(Slot slot, Cipher cipher) {
            VaultFileCredentials creds = new VaultFileCredentials();

            try {
                slot.setKey(creds.getKey(), cipher);
                creds.getSlots().add(slot);
                getVault().enableEncryption(creds);
            } catch (VaultManagerException | SlotException e) {
                onException(e);
                return;
            }

            getActivity().startService(new Intent(getActivity(), NotificationService.class));
            _pinKeyboardPreference.setChecked(false);
            updateEncryptionPreferences();
        }

        @Override
        public void onException(Exception e) {
            e.printStackTrace();
            Dialogs.showErrorDialog(getContext(), R.string.encryption_set_password_error, e);
        }
    }

    private class PasswordConfirmationListener implements PasswordSlotDecryptTask.Callback {
        @Override
        public void onTaskFinished(PasswordSlotDecryptTask.Result result) {
            if (result != null) {
                _pinKeyboardPreference.setChecked(true);
            } else {
                Dialogs.showSecureDialog(new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.pin_keyboard_error)
                        .setMessage(R.string.invalid_password)
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok, null)
                        .create());
                _pinKeyboardPreference.setChecked(false);
            }
        }
    }
}
