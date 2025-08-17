package com.beemdevelopment.aegis.ui;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.os.Parcelable;
import android.widget.Toast;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.vault.VaultRepositoryException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class NfcTransferActivity extends AegisActivity implements NfcAdapter.CreateNdefMessageCallback {

    private NfcAdapter nfcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc_transfer);
        setSupportActionBar(findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle("NFC Transfer");

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC is not available on this device.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        nfcAdapter.setNdefPushMessageCallback((NfcAdapter.CreateNdefMessageCallback) this, this);
    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            _vaultManager.getVault().export(out);
            byte[] vaultBytes = out.toByteArray();
            NdefRecord record = NdefRecord.createMime("application/vnd.com.beemdevelopment.aegis", vaultBytes);
            return new NdefMessage(record);
        } catch (VaultRepositoryException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_MUTABLE);
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            Parcelable[] rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMessages != null && rawMessages.length > 0) {
                NdefMessage message = (NdefMessage) rawMessages[0];
                NdefRecord[] records = message.getRecords();
                if (records != null && records.length > 0) {
                    byte[] payload = records[0].getPayload();
                    try {
                        com.beemdevelopment.aegis.vault.VaultFile vaultFile = com.beemdevelopment.aegis.vault.VaultFile.fromBytes(payload);
                        if (vaultFile.isEncrypted()) {
                            com.beemdevelopment.aegis.ui.dialogs.Dialogs.showPasswordInputDialog(this, password -> {
                                try {
                                    java.util.List<com.beemdevelopment.aegis.vault.slots.PasswordSlot> slots = vaultFile.getHeader().getSlots().findAll(com.beemdevelopment.aegis.vault.slots.PasswordSlot.class);
                                    com.beemdevelopment.aegis.crypto.MasterKey masterKey = com.beemdevelopment.aegis.ui.tasks.PasswordSlotDecryptTask.decryptWithPassword(slots, password);
                                    com.beemdevelopment.aegis.vault.VaultFileCredentials creds = new com.beemdevelopment.aegis.vault.VaultFileCredentials(masterKey, vaultFile.getHeader().getSlots());
                                    com.beemdevelopment.aegis.vault.Vault receivedVault = com.beemdevelopment.aegis.vault.Vault.fromJson(vaultFile.getContent(creds));
                                    showImportConfirmationDialog(receivedVault);
                                } catch (com.beemdevelopment.aegis.vault.slots.SlotException | com.beemdevelopment.aegis.vault.slots.SlotIntegrityException | com.beemdevelopment.aegis.vault.VaultFileException | org.json.JSONException | com.beemdevelopment.aegis.vault.VaultException e) {
                                    e.printStackTrace();
                                    Toast.makeText(this, "Failed to decrypt vault.", Toast.LENGTH_LONG).show();
                                }
                            });
                        } else {
                            com.beemdevelopment.aegis.vault.Vault receivedVault = com.beemdevelopment.aegis.vault.Vault.fromJson(vaultFile.getContent());
                            showImportConfirmationDialog(receivedVault);
                        }
                    } catch (com.beemdevelopment.aegis.vault.VaultFileException | org.json.JSONException | com.beemdevelopment.aegis.vault.VaultException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Failed to import vault.", Toast.LENGTH_LONG).show();
                    }
                }
            }
        }
    }

    private void showImportConfirmationDialog(com.beemdevelopment.aegis.vault.Vault receivedVault) {
        int entryCount = receivedVault.getEntries().getValues().size();

        new android.app.AlertDialog.Builder(this)
                .setTitle("Vault Received")
                .setMessage("Received a vault with " + entryCount + " entries. Do you want to import them?")
                .setPositiveButton("Import", (dialog, which) -> {
                    for (com.beemdevelopment.aegis.vault.VaultEntry entry : receivedVault.getEntries().getValues()) {
                        if (!_vaultManager.getVault().isEntryDuplicate(entry)) {
                            _vaultManager.getVault().addEntry(entry);
                        }
                    }
                    saveAndBackupVault();
                    Toast.makeText(this, "Vault imported successfully!", Toast.LENGTH_LONG).show();
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
