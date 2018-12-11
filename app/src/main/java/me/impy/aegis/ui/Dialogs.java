package me.impy.aegis.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.fingerprint.FingerprintManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.mattprecious.swirl.SwirlView;

import java.util.concurrent.atomic.AtomicReference;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import me.impy.aegis.Preferences;
import me.impy.aegis.R;
import me.impy.aegis.crypto.KeyStoreHandle;
import me.impy.aegis.crypto.KeyStoreHandleException;
import me.impy.aegis.db.slots.FingerprintSlot;
import me.impy.aegis.db.slots.PasswordSlot;
import me.impy.aegis.db.slots.Slot;
import me.impy.aegis.db.slots.SlotException;
import me.impy.aegis.helpers.EditTextHelper;
import me.impy.aegis.helpers.FingerprintHelper;
import me.impy.aegis.helpers.FingerprintUiHelper;
import me.impy.aegis.ui.tasks.DerivationTask;

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

    public static void showTextInputDialog(Context context, @StringRes int titleId, TextInputListener listener) {
        EditText input = new EditText(context);

        showSecureDialog(new AlertDialog.Builder(context)
                .setTitle(titleId)
                .setView(input)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        listener.onTextInputResult(input.getText().toString());
                    }
                })
                .create());
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
            public void beforeTextChanged(CharSequence c, int start, int count, int after) { }
            public void afterTextChanged(Editable c) { }
        };
        textPassword.addTextChangedListener(watcher);
        textPasswordConfirm.addTextChangedListener(watcher);

        showSecureDialog(dialog);
    }

    public static void showFingerprintDialog(Activity activity, Dialogs.SlotListener listener) {
        View view = activity.getLayoutInflater().inflate(R.layout.dialog_fingerprint, null);
        TextView textFingerprint = view.findViewById(R.id.text_fingerprint);
        SwirlView imgFingerprint = view.findViewById(R.id.img_fingerprint);

        Cipher cipher;
        FingerprintSlot slot;
        final AtomicReference<FingerprintUiHelper> helper = new AtomicReference<>();
        FingerprintManager manager = FingerprintHelper.getManager(activity);

        try {
            slot = new FingerprintSlot();
            SecretKey key = new KeyStoreHandle().generateKey(slot.getUUID().toString());
            cipher = Slot.createEncryptCipher(key);
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
                listener.onSlotResult(slot, cipher);
                dialog.dismiss();
            }

            @Override
            public void onError() {

            }
        }));

        helper.get().startListening(new FingerprintManager.CryptoObject(cipher));
        showSecureDialog(dialog);
    }

    public interface TextInputListener {
        void onTextInputResult(String text);
    }

    public interface SlotListener {
        void onSlotResult(Slot slot, Cipher cipher);
        void onException(Exception e);
    }
}
