package com.beemdevelopment.aegis.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.biometric.BiometricPrompt;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.crypto.KeyStoreHandle;
import com.beemdevelopment.aegis.crypto.KeyStoreHandleException;
import com.beemdevelopment.aegis.vault.VaultFileCredentials;
import com.beemdevelopment.aegis.vault.slots.BiometricSlot;
import com.beemdevelopment.aegis.vault.slots.PasswordSlot;
import com.beemdevelopment.aegis.vault.slots.Slot;
import com.beemdevelopment.aegis.vault.slots.SlotException;
import com.beemdevelopment.aegis.vault.slots.SlotList;
import com.beemdevelopment.aegis.helpers.BiometricSlotInitializer;
import com.beemdevelopment.aegis.helpers.BiometricsHelper;
import com.beemdevelopment.aegis.ui.views.SlotAdapter;

import javax.crypto.Cipher;

public class SlotManagerActivity extends AegisActivity implements SlotAdapter.Listener {
    private VaultFileCredentials _creds;
    private SlotAdapter _adapter;

    private boolean _edited;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slots);
        _edited = false;

        ActionBar bar = getSupportActionBar();
        bar.setHomeAsUpIndicator(R.drawable.ic_close);
        bar.setDisplayHomeAsUpEnabled(true);

        findViewById(R.id.button_add_biometric).setOnClickListener(view -> {
            if (BiometricsHelper.isAvailable(this)) {
                BiometricSlotInitializer initializer = new BiometricSlotInitializer(SlotManagerActivity.this, new RegisterBiometricsListener());
                BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                        .setTitle(getString(R.string.add_biometric_slot))
                        .setNegativeButtonText(getString(android.R.string.cancel))
                        .build();
                initializer.authenticate(info);
            }
        });

        findViewById(R.id.button_add_password).setOnClickListener(view -> {
            Dialogs.showSetPasswordDialog(this, new PasswordListener());
        });

        // set up the recycler view
        _adapter = new SlotAdapter(this);
        RecyclerView slotsView = findViewById(R.id.list_slots);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        slotsView.setLayoutManager(layoutManager);
        slotsView.setAdapter(_adapter);
        slotsView.setNestedScrollingEnabled(false);

        // load the slots and masterKey
        _creds = (VaultFileCredentials) getIntent().getSerializableExtra("creds");
        for (Slot slot : _creds.getSlots()) {
            _adapter.addSlot(slot);
        }

        updateBiometricsButton();
    }

    private void updateBiometricsButton() {
        // only show the biometrics option if we can get an instance of the biometrics manager
        // and if none of the slots in the collection has a matching alias in the keystore
        int visibility = View.VISIBLE;
        if (BiometricsHelper.isAvailable(this)) {
            try {
                KeyStoreHandle keyStore = new KeyStoreHandle();
                for (BiometricSlot slot : _creds.getSlots().findAll(BiometricSlot.class)) {
                    if (keyStore.containsKey(slot.getUUID().toString())) {
                        visibility = View.GONE;
                        break;
                    }
                }
            } catch (KeyStoreHandleException e) {
                visibility = View.GONE;
            }
        } else {
            visibility = View.GONE;
        }
        findViewById(R.id.button_add_biometric).setVisibility(visibility);
    }

    private void onSave() {
        Intent intent = new Intent();
        intent.putExtra("creds", _creds);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.action_save:
                onSave();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_slots, menu);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (!_edited) {
            super.onBackPressed();
            return;
        }

        Dialogs.showDiscardDialog(this,
                (dialog, which) -> onSave(),
                (dialog, which) -> super.onBackPressed()
        );
    }

    @Override
    public void onEditSlot(Slot slot) {
        /*EditText textName = new EditText(this);
        textName.setHint("Name");

        new AlertDialog.Builder(this)
                .setTitle("Edit slot name")
                .setView(textName)
                .setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {
                    String name = textName.getText().toString();
                    _edited = true;
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();*/
    }

    @Override
    public void onRemoveSlot(Slot slot) {
        SlotList slots = _creds.getSlots();
        if (slot instanceof PasswordSlot && slots.findAll(PasswordSlot.class).size() <= 1) {
            Toast.makeText(this, R.string.password_slot_error, Toast.LENGTH_SHORT).show();
            return;
        }

        Dialogs.showSecureDialog(new AlertDialog.Builder(this)
                .setTitle(R.string.remove_slot)
                .setMessage(R.string.remove_slot_description)
                .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                    slots.remove(slot);
                    _adapter.removeSlot(slot);
                    _edited = true;
                    updateBiometricsButton();
                })
                .setNegativeButton(android.R.string.no, null)
                .create());
    }

    private void addSlot(Slot slot) {
        _creds.getSlots().add(slot);
        _adapter.addSlot(slot);
        _edited = true;
        updateBiometricsButton();
    }

    private void showSlotError(String error) {
        Toast.makeText(SlotManagerActivity.this, getString(R.string.adding_new_slot_error) + error, Toast.LENGTH_SHORT).show();
    }

    private class RegisterBiometricsListener implements BiometricSlotInitializer.Listener {

        @Override
        public void onInitializeSlot(BiometricSlot slot, Cipher cipher) {
            try {
                slot.setKey(_creds.getKey(), cipher);
                addSlot(slot);
            } catch (SlotException e) {
                onSlotInitializationFailed(0, e.toString());
            }
        }

        @Override
        public void onSlotInitializationFailed(int errorCode, @NonNull CharSequence errString) {
            if (!BiometricsHelper.isCanceled(errorCode)) {
                showSlotError(errString.toString());
            }
        }
    }

    private class PasswordListener implements Dialogs.SlotListener {

        @Override
        public void onSlotResult(Slot slot, Cipher cipher) {
            try {
                slot.setKey(_creds.getKey(), cipher);
            } catch (SlotException e) {
                onException(e);
                return;
            }

            addSlot(slot);
        }

        @Override
        public void onException(Exception e) {
            showSlotError(e.toString());
        }
    }
}
