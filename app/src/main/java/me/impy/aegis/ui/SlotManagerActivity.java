package me.impy.aegis.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import javax.crypto.Cipher;

import me.impy.aegis.R;
import me.impy.aegis.crypto.KeyStoreHandle;
import me.impy.aegis.crypto.KeyStoreHandleException;
import me.impy.aegis.crypto.MasterKey;
import me.impy.aegis.db.slots.FingerprintSlot;
import me.impy.aegis.db.slots.PasswordSlot;
import me.impy.aegis.db.slots.Slot;
import me.impy.aegis.db.slots.SlotCollection;
import me.impy.aegis.helpers.FingerprintHelper;
import me.impy.aegis.ui.dialogs.FingerprintDialogFragment;
import me.impy.aegis.ui.dialogs.PasswordDialogFragment;
import me.impy.aegis.ui.views.SlotAdapter;
import me.impy.aegis.ui.dialogs.SlotDialogFragment;

public class SlotManagerActivity extends AegisActivity implements SlotAdapter.Listener, SlotDialogFragment.Listener {
    private MasterKey _masterKey;
    private SlotCollection _slots;
    private SlotAdapter _adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set up the view
        setContentView(R.layout.activity_slots);
        setSupportActionBar(findViewById(R.id.toolbar));
        ActionBar bar = getSupportActionBar();
        bar.setHomeAsUpIndicator(R.drawable.ic_close);
        bar.setDisplayHomeAsUpEnabled(true);

        findViewById(R.id.button_add_fingerprint).setOnClickListener(view -> {
            FingerprintDialogFragment dialog = new FingerprintDialogFragment();
            dialog.show(getSupportFragmentManager(), null);
        });

        findViewById(R.id.button_add_password).setOnClickListener(view -> {
            PasswordDialogFragment dialog = new PasswordDialogFragment();
            dialog.show(getSupportFragmentManager(), null);
        });

        // set up the recycler view
        _adapter = new SlotAdapter(this);
        RecyclerView slotsView = findViewById(R.id.list_slots);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        slotsView.setLayoutManager(layoutManager);
        slotsView.setAdapter(_adapter);
        slotsView.setNestedScrollingEnabled(false);

        // load the slots and masterKey
        _masterKey = (MasterKey) getIntent().getSerializableExtra("masterKey");
        _slots = (SlotCollection) getIntent().getSerializableExtra("slots");
        for (Slot slot : _slots) {
            _adapter.addSlot(slot);
        }

        updateFingerprintButton();
    }

    private void updateFingerprintButton() {
        // only show the fingerprint option if we can get an instance of the fingerprint manager
        // and if none of the slots in the collection has a matching alias in the keystore
        int visibility = View.VISIBLE;
        if (FingerprintHelper.isSupported()) {
            try {
                KeyStoreHandle keyStore = new KeyStoreHandle();
                for (FingerprintSlot slot : _slots.findAll(FingerprintSlot.class)) {
                    if (keyStore.containsKey(slot.getUUID().toString()) && FingerprintHelper.getManager(this) != null) {
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

    private boolean onSave() {
        Intent intent = new Intent();
        intent.putExtra("slots", _slots);
        setResult(RESULT_OK, intent);
        finish();
        return true;
    }

    @Override
    protected void setPreferredTheme(boolean nightMode) {
        if (nightMode) {
            setTheme(R.style.AppTheme_Dark_NoActionBar);
        } else {
            setTheme(R.style.AppTheme_Default_NoActionBar);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_save:
                return onSave();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_slots, menu);
        return true;
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
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();*/
    }

    @Override
    public void onRemoveSlot(Slot slot) {
        if (slot instanceof PasswordSlot && _slots.findAll(PasswordSlot.class).size() <= 1) {
            Toast.makeText(this, "You must have at least one password slot", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Remove slot")
                .setMessage("Are you sure you want to remove this slot?")
                .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                    _slots.remove(slot);
                    _adapter.removeSlot(slot);
                    updateFingerprintButton();
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    @Override
    public void onSlotResult(Slot slot, Cipher cipher) {
        try {
            _slots.encrypt(slot, _masterKey, cipher);
        } catch (Exception e) {
            onException(e);
            return;
        }

        _slots.add(slot);
        _adapter.addSlot(slot);
        updateFingerprintButton();
    }

    @Override
    public void onException(Exception e) {
        Toast.makeText(this, "An error occurred while trying to add a new slot: " + e.getMessage(), Toast.LENGTH_SHORT).show();
    }
}
