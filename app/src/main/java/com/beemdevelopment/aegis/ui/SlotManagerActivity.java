package com.beemdevelopment.aegis.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.crypto.KeyStoreHandle;
import com.beemdevelopment.aegis.crypto.KeyStoreHandleException;
import com.beemdevelopment.aegis.db.DatabaseFileCredentials;
import com.beemdevelopment.aegis.db.slots.FingerprintSlot;
import com.beemdevelopment.aegis.db.slots.PasswordSlot;
import com.beemdevelopment.aegis.db.slots.Slot;
import com.beemdevelopment.aegis.db.slots.SlotException;
import com.beemdevelopment.aegis.db.slots.SlotList;
import com.beemdevelopment.aegis.helpers.FingerprintHelper;
import com.beemdevelopment.aegis.ui.views.SlotAdapter;

import javax.crypto.Cipher;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class SlotManagerActivity extends AegisActivity implements SlotAdapter.Listener, Dialogs.SlotListener {
    private DatabaseFileCredentials _creds;
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

        findViewById(R.id.button_add_fingerprint).setOnClickListener(view -> {
            if (FingerprintHelper.isSupported() && FingerprintHelper.isAvailable(this)) {
                Dialogs.showFingerprintDialog(this ,this);
            }
        });

        findViewById(R.id.button_add_password).setOnClickListener(view -> {
            Dialogs.showSetPasswordDialog(this, this);
        });

        // set up the recycler view
        _adapter = new SlotAdapter(this);
        RecyclerView slotsView = findViewById(R.id.list_slots);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        slotsView.setLayoutManager(layoutManager);
        slotsView.setAdapter(_adapter);
        slotsView.setNestedScrollingEnabled(false);

        // load the slots and masterKey
        _creds = (DatabaseFileCredentials) getIntent().getSerializableExtra("creds");
        for (Slot slot : _creds.getSlots()) {
            _adapter.addSlot(slot);
        }

        updateFingerprintButton();
    }

    private void updateFingerprintButton() {
        // only show the fingerprint option if we can get an instance of the fingerprint manager
        // and if none of the slots in the collection has a matching alias in the keystore
        int visibility = View.VISIBLE;
        if (FingerprintHelper.isSupported() && FingerprintHelper.isAvailable(this)) {
            try {
                KeyStoreHandle keyStore = new KeyStoreHandle();
                for (FingerprintSlot slot : _creds.getSlots().findAll(FingerprintSlot.class)) {
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
        findViewById(R.id.button_add_fingerprint).setVisibility(visibility);
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
                    updateFingerprintButton();
                })
                .setNegativeButton(android.R.string.no, null)
                .create());
    }

    @Override
    public void onSlotResult(Slot slot, Cipher cipher) {
        try {
            slot.setKey(_creds.getKey(), cipher);
        } catch (SlotException e) {
            onException(e);
            return;
        }

        _creds.getSlots().add(slot);
        _adapter.addSlot(slot);
        _edited = true;
        updateFingerprintButton();
    }

    @Override
    public void onException(Exception e) {
        Toast.makeText(this, getString(R.string.adding_new_slot_error) + e.getMessage(), Toast.LENGTH_SHORT).show();
    }
}
