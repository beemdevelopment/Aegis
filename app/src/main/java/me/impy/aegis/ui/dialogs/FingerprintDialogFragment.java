package me.impy.aegis.ui.dialogs;

import android.app.Dialog;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.TextView;

import com.mattprecious.swirl.SwirlView;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import me.impy.aegis.R;
import me.impy.aegis.crypto.KeyStoreHandle;
import me.impy.aegis.crypto.KeyStoreHandleException;
import me.impy.aegis.db.slots.FingerprintSlot;
import me.impy.aegis.db.slots.Slot;
import me.impy.aegis.db.slots.SlotException;
import me.impy.aegis.helpers.FingerprintHelper;
import me.impy.aegis.helpers.FingerprintUiHelper;

public class FingerprintDialogFragment extends SlotDialogFragment implements FingerprintUiHelper.Callback {
    private Cipher _cipher;
    private FingerprintUiHelper _helper;
    private FingerprintSlot _slot;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_fingerprint, null);
        TextView textFingerprint = view.findViewById(R.id.text_fingerprint);
        SwirlView imgFingerprint = view.findViewById(R.id.img_fingerprint);

        FingerprintManager manager = FingerprintHelper.getManager(getContext());
        try {
            _slot = new FingerprintSlot();
            SecretKey key = new KeyStoreHandle().generateKey(_slot.getUUID().toString());
            _cipher = Slot.createCipher(key, Cipher.ENCRYPT_MODE);
            _helper = new FingerprintUiHelper(manager, imgFingerprint, textFingerprint, this);
        } catch (KeyStoreHandleException | SlotException e) {
            throw new RuntimeException(e);
        }

        return new AlertDialog.Builder(getActivity())
                .setTitle("Register a new fingerprint")
                .setView(view)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (_helper != null) {
            _helper.startListening(new FingerprintManager.CryptoObject(_cipher));
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (_helper != null) {
            _helper.stopListening();
        }
    }

    @Override
    public void onAuthenticated() {
        getListener().onSlotResult(_slot, _cipher);
        dismiss();
    }

    @Override
    public void onError() {

    }
}
