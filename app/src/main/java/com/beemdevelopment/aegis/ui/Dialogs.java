package com.beemdevelopment.aegis.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import com.beemdevelopment.aegis.Preferences;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.helpers.EditTextHelper;
import com.beemdevelopment.aegis.ui.tasks.KeyDerivationTask;
import com.beemdevelopment.aegis.vault.slots.PasswordSlot;
import com.beemdevelopment.aegis.vault.slots.Slot;
import com.beemdevelopment.aegis.vault.slots.SlotException;

import java.util.concurrent.atomic.AtomicReference;

import javax.crypto.Cipher;

public class Dialogs {
    private Dialogs() {

    }

    public static void secureDialog(Dialog dialog) {
        dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
    }

    public static void showSecureDialog(Dialog dialog) {
        if (new Preferences(dialog.getContext()).isSecureScreenEnabled()) {
            secureDialog(dialog);
        }
        dialog.show();
    }

    public static void showDeleteEntryDialog(Activity activity, DialogInterface.OnClickListener onDelete) {
        showSecureDialog(new AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.delete_entry))
                .setMessage(activity.getString(R.string.delete_entry_description))
                .setPositiveButton(android.R.string.yes, onDelete)
                .setNegativeButton(android.R.string.no, null)
                .create());
    }

    public static void showDeleteEntriesDialog(Activity activity, DialogInterface.OnClickListener onDelete, int totalEntries) {
        String title, message;
        if (totalEntries > 1) {
            title = activity.getString(R.string.delete_entries);
            message = String.format(activity.getString(R.string.delete_entries_description), totalEntries);
        } else {
            title = activity.getString(R.string.delete_entry);
            message = activity.getString(R.string.delete_entry_description);
        }

        showSecureDialog(new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.yes, onDelete)
                .setNegativeButton(android.R.string.no, null)
                .create());
    }

    public static void showDiscardDialog(Activity activity, DialogInterface.OnClickListener onSave, DialogInterface.OnClickListener onDiscard) {
        showSecureDialog(new AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.discard_changes))
                .setMessage(activity.getString(R.string.discard_changes_description))
                .setPositiveButton(R.string.save, onSave)
                .setNegativeButton(R.string.discard, onDiscard)
                .create());
    }

    public static void showSetPasswordDialog(Activity activity, Dialogs.SlotListener listener) {
        View view = activity.getLayoutInflater().inflate(R.layout.dialog_password, null);
        EditText textPassword = view.findViewById(R.id.text_password);
        EditText textPasswordConfirm = view.findViewById(R.id.text_password_confirm);
        CheckBox switchToggleVisibility = view.findViewById(R.id.check_toggle_visibility);

        switchToggleVisibility.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener) (buttonView, isChecked) -> {
            if (isChecked) {
                textPassword.setTransformationMethod(null);
                textPasswordConfirm.setTransformationMethod(null);
                textPassword.clearFocus();
                textPasswordConfirm.clearFocus();
            } else {
                textPassword.setTransformationMethod(new PasswordTransformationMethod());
                textPasswordConfirm.setTransformationMethod(new PasswordTransformationMethod());
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.set_password)
                .setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        final AtomicReference<Button> buttonOK = new AtomicReference<>();
        dialog.setOnShowListener(d -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setEnabled(false);
            buttonOK.set(button);

            // replace the default listener
            button.setOnClickListener(v -> {
                if (!EditTextHelper.areEditTextsEqual(textPassword, textPasswordConfirm)) {
                    return;
                }

                char[] password = EditTextHelper.getEditTextChars(textPassword);
                PasswordSlot slot = new PasswordSlot();
                KeyDerivationTask task = new KeyDerivationTask(activity, (passSlot, key) -> {
                    Cipher cipher;
                    try {
                        cipher = Slot.createEncryptCipher(key);
                    } catch (SlotException e) {
                        listener.onException(e);
                        dialog.cancel();
                        return;
                    }
                    listener.onSlotResult(slot, cipher);
                    dialog.dismiss();
                });
                task.execute(new KeyDerivationTask.Params(slot, password));
            });
        });

        TextWatcher watcher = new TextWatcher() {
            public void onTextChanged(CharSequence c, int start, int before, int count) {
                boolean equal = EditTextHelper.areEditTextsEqual(textPassword, textPasswordConfirm);
                buttonOK.get().setEnabled(equal);
            }

            public void beforeTextChanged(CharSequence c, int start, int count, int after) {
            }

            public void afterTextChanged(Editable c) {
            }
        };
        textPassword.addTextChangedListener(watcher);
        textPasswordConfirm.addTextChangedListener(watcher);

        showSecureDialog(dialog);
    }

    private static void showTextInputDialog(Context context, @StringRes int titleId, @StringRes int messageId, @StringRes int hintId, TextInputListener listener, boolean isSecret) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_text_input, null);
        EditText input = view.findViewById(R.id.text_input);
        if (isSecret) {
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        input.setHint(hintId);

        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(titleId)
                .setView(view)
                .setPositiveButton(android.R.string.ok, (dialog1, which) -> {
                    char[] text = EditTextHelper.getEditTextChars(input);
                    listener.onTextInputResult(text);
                });
        if (messageId != 0) {
            builder.setMessage(messageId);
        }

        AlertDialog dialog = builder.create();
        showSecureDialog(dialog);
    }

    private static void showTextInputDialog(Context context, @StringRes int titleId, @StringRes int hintId, TextInputListener listener, boolean isSecret) {
        showTextInputDialog(context, titleId, 0, hintId, listener, isSecret);
    }

    public static void showTextInputDialog(Context context, @StringRes int titleId, @StringRes int hintId, TextInputListener listener) {
        showTextInputDialog(context, titleId, hintId, listener, false);
    }

    public static void showPasswordInputDialog(Context context, TextInputListener listener) {
        showTextInputDialog(context, R.string.set_password, R.string.password, listener, true);
    }

    public static void showPasswordInputDialog(Context context, @StringRes int messageId, TextInputListener listener) {
        showTextInputDialog(context, R.string.set_password, messageId, R.string.password, listener, true);
    }

    public static void showNumberPickerDialog(Activity activity, NumberInputListener listener) {
        View view = activity.getLayoutInflater().inflate(R.layout.dialog_number_picker, null);
        NumberPicker numberPicker = view.findViewById(R.id.numberPicker);
        numberPicker.setMinValue(3);
        numberPicker.setMaxValue(60);
        numberPicker.setValue(new Preferences(activity.getApplicationContext()).getTapToRevealTime());
        numberPicker.setWrapSelectorWheel(true);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.set_number)
                .setView(view)
                .setPositiveButton(android.R.string.ok, (dialog1, which) ->
                        listener.onNumberInputResult(numberPicker.getValue()))
                .create();

        showSecureDialog(dialog);
    }

    public static void showBackupVersionsPickerDialog(Activity activity, NumberInputListener listener) {
        final int max = 30;
        String[] numbers = new String[max / 5];
        for (int i = 0; i < numbers.length; i++) {
            numbers[i] = Integer.toString(i * 5 + 5);
        }

        View view = activity.getLayoutInflater().inflate(R.layout.dialog_number_picker, null);
        NumberPicker numberPicker = view.findViewById(R.id.numberPicker);
        numberPicker.setDisplayedValues(numbers);
        numberPicker.setMaxValue(numbers.length - 1);
        numberPicker.setMinValue(0);
        numberPicker.setValue(new Preferences(activity.getApplicationContext()).getBackupsVersionCount() / 5 - 1);
        numberPicker.setWrapSelectorWheel(false);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.set_number)
                .setView(view)
                .setPositiveButton(android.R.string.ok, (dialog1, which) ->
                        listener.onNumberInputResult(numberPicker.getValue()))
                .create();

        showSecureDialog(dialog);
    }

    public static void showErrorDialog(Context context, @StringRes int message, Exception e) {
        showErrorDialog(context, message, e, null);
    }

    public static void showErrorDialog(Context context, @StringRes int message, CharSequence error) {
        showErrorDialog(context, message, error, null);
    }

    public static void showErrorDialog(Context context, @StringRes int message, Exception e, DialogInterface.OnClickListener listener) {
        showErrorDialog(context, message, e.toString(), listener);
    }

    public static void showErrorDialog(Context context, @StringRes int message, CharSequence error, DialogInterface.OnClickListener listener) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_error, null);
        TextView textDetails = view.findViewById(R.id.error_details);
        textDetails.setText(error);
        TextView textMessage = view.findViewById(R.id.error_message);
        textMessage.setText(message);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.error_occurred)
                .setView(view)
                .setPositiveButton(android.R.string.ok, (dialog1, which) -> {
                    if (listener != null) {
                        listener.onClick(dialog1, which);
                    }
                })
                .setNeutralButton(R.string.details, (dialog1, which) -> {
                    textDetails.setVisibility(View.VISIBLE);
                })
                .create();

        dialog.setOnShowListener(d -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            button.setOnClickListener(v -> {
                if (textDetails.getVisibility() == View.GONE) {
                    textDetails.setVisibility(View.VISIBLE);
                    button.setText(R.string.copy);
                } else {
                    ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboard != null) {
                        ClipData clip = ClipData.newPlainText("text/plain", error);
                        clipboard.setPrimaryClip(clip);
                    }
                }
            });
        });

        Dialogs.showSecureDialog(dialog);
    }

    public static void showTimeSyncWarningDialog(Context context, Dialog.OnClickListener listener) {
        Preferences prefs = new Preferences(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_time_sync, null);
        CheckBox checkBox = view.findViewById(R.id.check_warning_disable);

        showSecureDialog(new AlertDialog.Builder(context)
                .setTitle(R.string.time_sync_warning_title)
                .setView(view)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    if (checkBox.isChecked()) {
                        prefs.setIsTimeSyncWarningEnabled(false);
                    }
                    if (listener != null) {
                        listener.onClick(dialog, which);
                    }
                })
                .setNegativeButton(R.string.no, (dialog, which) -> {
                    if (checkBox.isChecked()) {
                        prefs.setIsTimeSyncWarningEnabled(false);
                    }
                })
                .create());
    }

    public interface NumberInputListener {
        void onNumberInputResult(int number);
    }

    public interface TextInputListener {
        void onTextInputResult(char[] text);
    }

    public interface SlotListener {
        void onSlotResult(Slot slot, Cipher cipher);
        void onException(Exception e);
    }
}
