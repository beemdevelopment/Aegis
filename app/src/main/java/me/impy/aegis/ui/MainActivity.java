package me.impy.aegis.ui;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Rect;
import android.media.MediaScannerConnection;
import android.support.design.widget.BottomSheetDialog;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionsMenu;

import java.lang.reflect.UndeclaredThrowableException;

import me.impy.aegis.AegisApplication;
import me.impy.aegis.R;
import me.impy.aegis.crypto.MasterKey;
import me.impy.aegis.db.DatabaseManagerException;
import me.impy.aegis.db.slots.SlotCollection;
import me.impy.aegis.db.DatabaseEntry;
import me.impy.aegis.db.DatabaseManager;
import me.impy.aegis.helpers.PermissionHelper;
import me.impy.aegis.ui.views.KeyProfile;
import me.impy.aegis.ui.views.KeyProfileView;

public class MainActivity extends AegisActivity implements KeyProfileView.Listener {
    // activity request codes
    private static final int CODE_SCAN_KEYINFO = 0;
    private static final int CODE_ADD_KEYINFO = 1;
    private static final int CODE_EDIT_KEYINFO = 2;
    private static final int CODE_ENTER_KEYINFO = 3;
    private static final int CODE_DO_INTRO = 4;
    private static final int CODE_DECRYPT = 5;
    private static final int CODE_PREFERENCES = 6;
    private static final int CODE_SLOTS = 7;

    // permission request codes
    private static final int CODE_PERM_CAMERA = 0;

    private AegisApplication _app;
    private DatabaseManager _db;
    private KeyProfileView _keyProfileView;

    private boolean _nightMode = false;
    private Menu _menu;
    private FloatingActionsMenu _fabMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _app = (AegisApplication) getApplication();
        _db = _app.getDatabaseManager();

        // set up the main view
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.toolbar));

        // set up the key profile view
        _keyProfileView = (KeyProfileView) getSupportFragmentManager().findFragmentById(R.id.key_profiles);
        _keyProfileView.setListener(this);
        _keyProfileView.setShowIssuer(_app.getPreferences().getBoolean("pref_issuer", false));

        // set up the floating action button
        _fabMenu = findViewById(R.id.fab);
        findViewById(R.id.fab_enter).setOnClickListener(view -> {
            _fabMenu.collapse();
            onEnterKeyInfo();
        });
        findViewById(R.id.fab_scan).setOnClickListener(view -> {
            _fabMenu.collapse();
            onScanKeyInfo();
        });

        // skip this part if this is the not initial startup and the database has been unlocked
        if (!_app.isRunning() && _db.isLocked()) {
            if (!_db.fileExists()) {
                // the db doesn't exist, start the intro
                if (_app.getPreferences().getBoolean("pref_intro", false)) {
                    Toast.makeText(this, "Database file not found, starting over...", Toast.LENGTH_SHORT).show();
                }
                Intent intro = new Intent(this, IntroActivity.class);
                startActivityForResult(intro, CODE_DO_INTRO);
            } else {
                // the db exists, load the database
                // if the database is still encrypted, start the auth activity
                try {
                    if (!_db.isLoaded()) {
                        _db.load();
                    }
                    if (_db.isLocked()) {
                        startAuthActivity();
                    }
                } catch (DatabaseManagerException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "An error occurred while trying to deserialize the database", Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        }

        // if the database has been decrypted at this point, we can load the key profiles
        if (!_db.isLocked()) {
            loadKeyProfiles();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        // collapse the fab menu on touch
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (_fabMenu.isExpanded()) {
                Rect rect = new Rect();
                _fabMenu.getGlobalVisibleRect(rect);

                if (!rect.contains((int) event.getRawX(), (int) event.getRawY())) {
                    _fabMenu.collapse();
                }
            }
        }

        return super.dispatchTouchEvent(event);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        if (!doShortcutActions() || _db.isLocked()) {
            startAuthActivity();
        }
    }

    @Override
    protected void setPreferredTheme(boolean nightMode) {
        if (nightMode) {
            setTheme(R.style.AppTheme_Dark_NoActionBar);
        } else if (_nightMode) {
            setTheme(R.style.AppTheme_Default_NoActionBar);
        }
        _nightMode = nightMode;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data == null) {
            return;
        }

        switch (requestCode) {
            case CODE_SCAN_KEYINFO:
                onScanKeyInfoResult(resultCode, data);
                break;
            case CODE_ADD_KEYINFO:
                onAddKeyInfoResult(resultCode, data);
                break;
            case CODE_EDIT_KEYINFO:
                onEditKeyInfoResult(resultCode, data);
                break;
            case CODE_ENTER_KEYINFO:
                onEnterKeyInfoResult(resultCode, data);
                break;
            case CODE_DO_INTRO:
                onDoIntroResult(resultCode, data);
                break;
            case CODE_DECRYPT:
                onDecryptResult(resultCode, data);
                break;
            case CODE_PREFERENCES:
                onPreferencesResult(resultCode, data);
                break;
            case CODE_SLOTS:
                onSlotManagerResult(resultCode, data);
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (!PermissionHelper.checkResults(grantResults)) {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            return;
        }

        switch (requestCode) {
            case CODE_PERM_CAMERA:
                onScanKeyInfo();
                break;
        }
    }

    private void onSlotManagerResult(int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }

        SlotCollection slots = (SlotCollection) data.getSerializableExtra("slots");
        _db.getFile().setSlots(slots);
        saveDatabase();
    }

    private void onPreferencesResult(int resultCode, Intent data) {
        // refresh the entire key profile list if needed
        if (data.getBooleanExtra("needsReload", false)) {
            _keyProfileView.clearKeys();
            for (DatabaseEntry entry : _db.getKeys()) {
                _keyProfileView.addKey(new KeyProfile(entry));
            }
        }
        if (data.getBooleanExtra("needsRefresh", false)) {
            boolean showIssuer = _app.getPreferences().getBoolean("pref_issuer", false);
            _keyProfileView.setShowIssuer(showIssuer);
        }

        // perform any pending actions
        int action = data.getIntExtra("action", -1);
        switch (action) {
            case PreferencesFragment.ACTION_SLOTS:
                MasterKey masterKey = _db.getMasterKey();
                Intent intent = new Intent(this, SlotManagerActivity.class);
                intent.putExtra("masterKey", masterKey);
                intent.putExtra("slots", _db.getFile().getSlots());
                startActivityForResult(intent, CODE_SLOTS);
                break;
        }
    }

    private void startEditProfileActivity(int requestCode, KeyProfile profile, boolean isNew) {
        Intent intent = new Intent(this, EditProfileActivity.class);
        if (profile != null) {
            intent.putExtra("KeyProfile", profile);
        }
        intent.putExtra("isNew", isNew);
        startActivityForResult(intent, requestCode);
    }

    private void onEnterKeyInfo() {
        startEditProfileActivity(CODE_ENTER_KEYINFO, null, true);
    }

    private void onScanKeyInfo() {
        if (!PermissionHelper.request(this, CODE_PERM_CAMERA, Manifest.permission.CAMERA)) {
            return;
        }

        startScanActivity();
    }

    private void onScanKeyInfoResult(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            KeyProfile profile = (KeyProfile)data.getSerializableExtra("KeyProfile");
            startEditProfileActivity(CODE_ADD_KEYINFO, profile, true);
        }
    }

    private void onAddKeyInfoResult(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            KeyProfile profile = (KeyProfile) data.getSerializableExtra("KeyProfile");
            addKey(profile);
            saveDatabase();
        }
    }

    private void onEditKeyInfoResult(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            KeyProfile profile = (KeyProfile) data.getSerializableExtra("KeyProfile");
            if (!data.getBooleanExtra("delete", false)) {
                // this profile has been serialized/deserialized and is no longer the same instance it once was
                // to deal with this, the replaceKey functions are used
                _db.replaceKey(profile.getEntry());
                _keyProfileView.replaceKey(profile);
                saveDatabase();
            } else {
                deleteProfile(profile);
            }
        }
    }

    private void onEnterKeyInfoResult(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            KeyProfile profile = (KeyProfile) data.getSerializableExtra("KeyProfile");
            addKey(profile);
            saveDatabase();
        }
    }

    private void addKey(KeyProfile profile) {
        DatabaseEntry entry = profile.getEntry();
        entry.setName(entry.getInfo().getAccountName());
        _db.addKey(entry);
        _keyProfileView.addKey(profile);
    }

    private void onDoIntroResult(int resultCode, Intent data) {
        if (resultCode == IntroActivity.RESULT_EXCEPTION) {
            // TODO: user feedback
            Exception e = (Exception) data.getSerializableExtra("exception");
            throw new UndeclaredThrowableException(e);
        }

        MasterKey key = (MasterKey) data.getSerializableExtra("key");
        try {
            _db.load();
            if (_db.isLocked()) {
                _db.unlock(key);
            }
        } catch (DatabaseManagerException e) {
            e.printStackTrace();
            Toast.makeText(this, "An error occurred while trying to load/decrypt the database", Toast.LENGTH_LONG).show();
            startAuthActivity();
            return;
        }

        loadKeyProfiles();
    }

    private void onDecryptResult(int resultCode, Intent intent) {
        MasterKey key = (MasterKey) intent.getSerializableExtra("key");
        try {
            _db.unlock(key);
        } catch (DatabaseManagerException e) {
            e.printStackTrace();
            Toast.makeText(this, "An error occurred while trying to decrypt the database", Toast.LENGTH_LONG).show();
            startAuthActivity();
            return;
        }

        loadKeyProfiles();
        doShortcutActions();
    }

    private void startScanActivity() {
        Intent scannerActivity = new Intent(getApplicationContext(), ScannerActivity.class);
        startActivityForResult(scannerActivity, CODE_SCAN_KEYINFO);
    }

    private boolean doShortcutActions() {
        // return false if an action was blocked by a locked database
        // otherwise, always return true
        Intent intent = getIntent();
        String action = intent.getStringExtra("action");
        if (action == null) {
            return true;
        } else if (_db.isLocked()) {
            return false;
        }

        switch (action) {
            case "scan":
                startScanActivity();
                break;
        }

        intent.removeExtra("action");
        return true;
    }

    public void startActivityForResult(Intent intent, int requestCode) {
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        super.startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onResume() {
        super.onResume();

        boolean nightMode = _app.getPreferences().getBoolean("pref_dark_mode", false);
        if (nightMode != _nightMode) {
            setPreferredTheme(nightMode);
            recreate();
        }

        // refresh all codes to prevent showing old ones
        _keyProfileView.refresh();
    }

    private BottomSheetDialog createBottomSheet(final KeyProfile profile) {
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_edit_profile, null);
        LinearLayout copyLayout = bottomSheetView.findViewById(R.id.copy_button);
        LinearLayout deleteLayout = bottomSheetView.findViewById(R.id.delete_button);
        LinearLayout editLayout = bottomSheetView.findViewById(R.id.edit_button);
        bottomSheetView.findViewById(R.id.edit_button);
        BottomSheetDialog bottomDialog = new BottomSheetDialog(this);
        bottomDialog.setContentView(bottomSheetView);
        bottomDialog.setCancelable(true);
        bottomDialog.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        bottomDialog.show();

        copyLayout.setOnClickListener(view -> {
            bottomDialog.dismiss();
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("text/plain", profile.getCode());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this.getApplicationContext(), "Code copied to the clipboard", Toast.LENGTH_SHORT).show();
        });

        deleteLayout.setOnClickListener(view -> {
            bottomDialog.dismiss();
            deleteProfile(profile);
        });

        editLayout.setOnClickListener(view -> {
            bottomDialog.dismiss();
            startEditProfileActivity(CODE_EDIT_KEYINFO, profile, false);
        });

        return bottomDialog;
    }

    private void deleteProfile(KeyProfile profile) {
        new AlertDialog.Builder(MainActivity.this)
            .setTitle("Delete entry")
            .setMessage("Are you sure you want to delete this profile?")
            .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                _db.removeKey(profile.getEntry());
                saveDatabase();

                _keyProfileView.removeKey(profile);
            })
            .setNegativeButton(android.R.string.no, null)
            .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        _menu = menu;
        getMenuInflater().inflate(R.menu.menu_main, menu);
        updateLockIcon();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent(this, PreferencesActivity.class);
                intent.putExtra("encrypted", _db.getFile().isEncrypted());
                startActivityForResult(intent, CODE_PREFERENCES);
                return true;
            case R.id.action_lock:
                _keyProfileView.clearKeys();
                _db.lock();
                startAuthActivity();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void startAuthActivity() {
        Intent intent = new Intent(this, AuthActivity.class);
        intent.putExtra("slots", _db.getFile().getSlots());
        startActivityForResult(intent, CODE_DECRYPT);
    }

    private void saveDatabase() {
        try {
            _db.save();
        } catch (DatabaseManagerException e) {
            e.printStackTrace();
            Toast.makeText(this, "An error occurred while trying to save the database", Toast.LENGTH_LONG).show();
        }
    }

    private void loadKeyProfiles() {
        updateLockIcon();

        for (DatabaseEntry entry : _db.getKeys()) {
            _keyProfileView.addKey(new KeyProfile(entry));
        }
    }

    private void updateLockIcon() {
        // hide the lock icon if the database is not unlocked
        if (_menu != null && !_db.isLocked()) {
            MenuItem item = _menu.findItem(R.id.action_lock);
            item.setVisible(_db.getFile().isEncrypted());
        }
    }

    @Override
    public void onEntryClick(KeyProfile profile) {
        createBottomSheet(profile).show();
    }

    @Override
    public void onEntryMove(DatabaseEntry entry1, DatabaseEntry entry2) {
        _db.swapKeys(entry1, entry2);
    }

    @Override
    public void onEntryDrop(DatabaseEntry entry) {
        saveDatabase();
    }
}
