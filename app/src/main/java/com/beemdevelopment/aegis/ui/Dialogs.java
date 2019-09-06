package com.beemdevelopment.aegis.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.beemdevelopment.aegis.Preferences;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.crypto.KeyStoreHandle;
import com.beemdevelopment.aegis.crypto.KeyStoreHandleException;
import com.beemdevelopment.aegis.db.slots.FingerprintSlot;
import com.beemdevelopment.aegis.db.slots.PasswordSlot;
import com.beemdevelopment.aegis.db.slots.Slot;
import com.beemdevelopment.aegis.db.slots.SlotException;
import com.beemdevelopment.aegis.helpers.EditTextHelper;
import com.beemdevelopment.aegis.helpers.FingerprintHelper;
import com.beemdevelopment.aegis.helpers.FingerprintUiHelper;
import com.beemdevelopment.aegis.ui.tasks.DerivationTask;
import com.mattprecious.swirl.SwirlView;

import java.util.concurrent.atomic.AtomicReference;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

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
                DerivationTask task = new DerivationTask(activity, key -> {
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
                task.execute(new DerivationTask.Params(slot, password));
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

    private static void showTextInputDialog(Context context, @StringRes int titleId, @StringRes int hintId, TextInputListener listener, boolean isSecret) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_text_input, null);
        EditText input = view.findViewById(R.id.text_input);
        if (isSecret) {
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        input.setHint(hintId);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(titleId)
                .setView(view)
                .setPositiveButton(android.R.string.ok, (dialog1, which) -> {
                        char[] text = EditTextHelper.getEditTextChars(input);
                        listener.onTextInputResult(text);
                })
                .create();

        showSecureDialog(dialog);
    }

    public static void showTextInputDialog(Context context, @StringRes int titleId, @StringRes int hintId, TextInputListener listener) {
        showTextInputDialog(context, titleId, hintId, listener, false);
    }

    public static void showPasswordInputDialog(Context context, TextInputListener listener) {
        showTextInputDialog(context, R.string.set_password, R.string.password, listener, true);
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

    @RequiresApi(api = Build.VERSION_CODES.M)
    public static void showFingerprintDialog(Activity activity, Dialogs.SlotListener listener) {
        View view = activity.getLayoutInflater().inflate(R.layout.dialog_fingerprint, null);
        TextView textFingerprint = view.findViewById(R.id.text_fingerprint);
        SwirlView imgFingerprint = view.findViewById(R.id.img_fingerprint);

        FingerprintManager.CryptoObject obj;
        FingerprintSlot slot;
        final AtomicReference<FingerprintUiHelper> helper = new AtomicReference<>();
        FingerprintManager manager = FingerprintHelper.getManager(activity);

        try {
            slot = new FingerprintSlot();
            SecretKey key = new KeyStoreHandle().generateKey(slot.getUUID().toString());
            Cipher cipher = Slot.createEncryptCipher(key);
            obj = new FingerprintManager.CryptoObject(cipher);
        } catch (KeyStoreHandleException | SlotException e) {
            throw new RuntimeException(e);
        }

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.register_fingerprint)
                .setView(view)
                .setNegativeButton(android.R.string.cancel, null)
                .setOnDismissListener(d -> {
                    helper.get().stopListening();
                })
                .create();

        helper.set(new FingerprintUiHelper(manager, imgFingerprint, textFingerprint, new FingerprintUiHelper.Callback() {
            @Override
            public void onAuthenticated() {
                listener.onSlotResult(slot, obj.getCipher());
                dialog.dismiss();
            }

            @Override
            public void onError() {

            }
        }));

        helper.get().startListening(obj);
        showSecureDialog(dialog);
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
