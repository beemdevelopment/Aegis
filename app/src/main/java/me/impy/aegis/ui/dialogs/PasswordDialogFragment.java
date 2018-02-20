package me.impy.aegis.ui.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import javax.crypto.Cipher;

import me.impy.aegis.R;
import me.impy.aegis.db.slots.PasswordSlot;
import me.impy.aegis.db.slots.Slot;
import me.impy.aegis.helpers.EditTextHelper;
import me.impy.aegis.ui.tasks.DerivationTask;

public class PasswordDialogFragment extends SlotDialogFragment {
    private Button _buttonOK;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_password, null);
        EditText textPassword = view.findViewById(R.id.text_password);
        EditText textPasswordConfirm = view.findViewById(R.id.text_password_confirm);

        AlertDialog alert = new AlertDialog.Builder(getActivity())
                .setTitle("Enter a new password")
                .setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        alert.setOnShowListener(dialog -> {
            _buttonOK = alert.getButton(AlertDialog.BUTTON_POSITIVE);
            _buttonOK.setEnabled(false);

            // replace the default listener
            _buttonOK.setOnClickListener(v -> {
                if (!EditTextHelper.areEditTextsEqual(textPassword, textPasswordConfirm)) {
                    return;
                }

                char[] password = EditTextHelper.getEditTextChars(textPassword);
                PasswordSlot slot = new PasswordSlot();
                DerivationTask task = new DerivationTask(getContext(), key -> {
                    Cipher cipher;
                    try {
                        cipher = Slot.createCipher(key, Cipher.ENCRYPT_MODE);
                    } catch (Exception e) {
                        getListener().onException(e);
                        dialog.cancel();
                        return;
                    }
                    getListener().onSlotResult(slot, cipher);
                    dialog.dismiss();
                });
                task.execute(new DerivationTask.Params() {{
                    Slot = slot;
                    Password = password;
                }});
            });
        });

        TextWatcher watcher = new TextWatcher() {
            public void onTextChanged(CharSequence c, int start, int before, int count) {
                boolean equal = EditTextHelper.areEditTextsEqual(textPassword, textPasswordConfirm);
                _buttonOK.setEnabled(equal);
            }
            public void beforeTextChanged(CharSequence c, int start, int count, int after) { }
            public void afterTextChanged(Editable c) { }
        };
        textPassword.addTextChangedListener(watcher);
        textPasswordConfirm.addTextChangedListener(watcher);

        return alert;
    }
}
