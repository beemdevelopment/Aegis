package me.impy.aegis;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Collections;
import java.util.List;

import me.impy.aegis.crypto.MasterKey;
import me.impy.aegis.db.DatabaseEntry;
import me.impy.aegis.db.DatabaseManager;
import me.impy.aegis.helpers.PermissionHelper;
import me.impy.aegis.importers.DatabaseImporter;
import me.impy.aegis.helpers.SimpleItemTouchHelperCallback;
import me.impy.aegis.util.ByteInputStream;

public class MainActivity extends AppCompatActivity implements KeyProfileAdapter.Listener {
    // activity request codes
    private static final int CODE_GET_KEYINFO = 0;
    private static final int CODE_ADD_KEYINFO = 1;
    private static final int CODE_DO_INTRO = 2;
    private static final int CODE_DECRYPT = 3;
    private static final int CODE_IMPORT = 4;
    private static final int CODE_PREFERENCES = 5;

    // permission request codes
    private static final int CODE_PERM_EXPORT = 0;
    private static final int CODE_PERM_IMPORT = 1;
    private static final int CODE_PERM_CAMERA = 2;

    private KeyProfileAdapter _keyProfileAdapter;
    private DatabaseManager _db;

    private boolean _nightMode = false;
    private Menu _menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _db = new DatabaseManager(getApplicationContext());

        SharedPreferences prefs = this.getSharedPreferences("me.impy.aegis", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("passedIntro", false)) {
            Intent intro = new Intent(this, IntroActivity.class);
            startActivityForResult(intro, CODE_DO_INTRO);
        } else {
            try {
                _db.load();
                if (!_db.isDecrypted()) {
                    Intent intent = new Intent(this, AuthActivity.class);
                    intent.putExtra("slots", _db.getFile().getSlots());
                    startActivityForResult(intent, CODE_DECRYPT);
                }
            } catch (FileNotFoundException e) {
                // start the intro if the db file was not found
                Toast.makeText(this, "Database file not found, starting over...", Toast.LENGTH_SHORT).show();
                Intent intro = new Intent(this, IntroActivity.class);
                startActivityForResult(intro, CODE_DO_INTRO);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "An error occurred while trying to deserialize the database", Toast.LENGTH_LONG).show();
                throw new UndeclaredThrowableException(e);
            }
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPreferences.getBoolean("pref_night_mode", false)) {
            _nightMode = true;
            setTheme(R.style.AppTheme_Dark_NoActionBar);
        } else {
            setPreferredTheme();
        }

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // init the app shortcuts and execute any pending actions
        initializeAppShortcuts();
        doShortcutActions();

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setEnabled(true);
        fab.setOnClickListener(view -> {
            onGetKeyInfo();
        });

        RecyclerView rvKeyProfiles = findViewById(R.id.rvKeyProfiles);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        rvKeyProfiles.setLayoutManager(mLayoutManager);

        _keyProfileAdapter = new KeyProfileAdapter(this);
        if (_db.isDecrypted()) {
            loadKeyProfiles();
        }

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(_keyProfileAdapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(rvKeyProfiles);
        rvKeyProfiles.setAdapter(_keyProfileAdapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case CODE_GET_KEYINFO:
                onGetKeyInfoResult(resultCode, data);
                break;
            case CODE_ADD_KEYINFO:
                onAddKeyInfoResult(resultCode, data);
                break;
            case CODE_DO_INTRO:
                onDoIntroResult(resultCode, data);
                break;
            case CODE_DECRYPT:
                onDecryptResult(resultCode, data);
                break;
            case CODE_IMPORT:
                onImportResult(resultCode, data);
                break;
            case CODE_PREFERENCES:
                onPreferencesResult(resultCode, data);
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
            case CODE_PERM_EXPORT:
                onExport();
                break;
            case CODE_PERM_IMPORT:
                onImport();
                break;
            case CODE_PERM_CAMERA:
                onGetKeyInfo();
                break;
        }
    }

    private void onPreferencesResult(int resultCode, Intent data) {
        // refresh the entire key profile list if needed
        if (data.getBooleanExtra("needsRefresh", false)) {
            _keyProfileAdapter.notifyDataSetChanged();
        }

        // perform any pending actions
        int action = data.getIntExtra("action", -1);
        switch (action) {
            case PreferencesActivity.ACTION_EXPORT:
                onExport();
                break;
        }
    }

    private void onExport() {
        if (!PermissionHelper.request(this, CODE_PERM_EXPORT, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            return;
        }

        // TODO: create a custom layout to show a message AND a checkbox
        final boolean[] checked = {true};
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
                .setTitle("Export the database")
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String filename;
                    try {
                        filename = _db.export(checked[0]);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "An error occurred while trying to export the database", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // make sure the new file is visible
                    MediaScannerConnection.scanFile(this, new String[]{filename}, null, null);

                    Toast.makeText(this, "The database has been exported to: " + filename, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null);
        if (_db.getFile().isEncrypted()) {
            final String[] items = {"Keep the database encrypted"};
            final boolean[] checkedItems = {true};
            builder.setMultiChoiceItems(items, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int index, boolean isChecked) {
                    checked[0] = isChecked;
                }
            });
        } else {
            builder.setMessage("This action will export the database out of Android's private storage.");
        }
        builder.show();
    }

    private void onImport() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, CODE_IMPORT);
    }

    private void onImportResult(int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }

        InputStream fileStream = null;
        try {
            try {
                fileStream = getContentResolver().openInputStream(data.getData());
            } catch (Exception e) {
                Toast.makeText(this, "An error occurred while trying to open the file", Toast.LENGTH_SHORT).show();
                return;
            }

            ByteInputStream stream;
            try {
                int read;
                byte[] buf = new byte[4096];
                ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                while ((read = fileStream.read(buf, 0, buf.length)) != -1) {
                    outStream.write(buf, 0, read);
                }
                stream = new ByteInputStream(outStream.toByteArray());
            } catch (Exception e) {
                Toast.makeText(this, "An error occurred while trying to read the file", Toast.LENGTH_SHORT).show();
                return;
            }

            List<DatabaseEntry> entries = null;
            for (DatabaseImporter converter : DatabaseImporter.create(stream)) {
                try {
                    entries = converter.convert();
                    break;
                } catch (Exception e) {
                    stream.reset();
                }
            }

            if (entries == null) {
                Toast.makeText(this, "An error occurred while trying to parse the file", Toast.LENGTH_SHORT).show();
                return;
            }

            for (DatabaseEntry entry : entries) {
                addKey(new KeyProfile(entry));
            }
        } finally {
            if (fileStream != null) {
                try {
                    fileStream.close();
                } catch (Exception e) {
                }
            }
        }

        saveDatabase();
    }

    private void onGetKeyInfo() {
        if (!PermissionHelper.request(this, CODE_PERM_CAMERA, Manifest.permission.CAMERA)) {
            return;
        }

        Intent scannerActivity = new Intent(getApplicationContext(), ScannerActivity.class);
        startActivityForResult(scannerActivity, CODE_GET_KEYINFO);
    }

    private void onGetKeyInfoResult(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            KeyProfile keyProfile = (KeyProfile)data.getSerializableExtra("KeyProfile");
            Intent intent = new Intent(this, AddProfileActivity.class);
            intent.putExtra("KeyProfile", keyProfile);
            startActivityForResult(intent, CODE_ADD_KEYINFO);
        }
    }

    private void onAddKeyInfoResult(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            KeyProfile profile = (KeyProfile) data.getSerializableExtra("KeyProfile");
            addKey(profile);
            saveDatabase();
        }
    }

    private void addKey(KeyProfile profile) {
        profile.refreshCode();

        DatabaseEntry entry = profile.getEntry();
        entry.setName(entry.getInfo().getAccountName());
        try {
            _db.addKey(entry);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "An error occurred while trying to add an entry", Toast.LENGTH_SHORT).show();
            return;
        }

        _keyProfileAdapter.addKey(profile);
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
            if (!_db.isDecrypted()) {
                _db.setMasterKey(key);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "An error occurred while trying to load/decrypt the database", Toast.LENGTH_LONG).show();
            recreate();
            return;
        }

        loadKeyProfiles();
    }

    private void onDecryptResult(int resultCode, Intent data) {
        MasterKey key = (MasterKey) data.getSerializableExtra("key");
        try {
            _db.setMasterKey(key);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "An error occurred while trying to decrypt the database", Toast.LENGTH_LONG).show();
            recreate();
            return;
        }

        loadKeyProfiles();
        doShortcutActions();
    }

    private void doShortcutActions() {
        Intent intent = getIntent();
        String mode = intent.getStringExtra("Action");
        if (mode == null || !_db.isDecrypted()) {
            return;
        }

        switch (mode) {
            case "Scan":
                Intent scannerActivity = new Intent(getApplicationContext(), ScannerActivity.class);
                startActivityForResult(scannerActivity, CODE_GET_KEYINFO);
                break;
        }

        intent.removeExtra("Action");
    }

    public void startActivityForResult(Intent intent, int requestCode) {
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        super.startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setPreferredTheme();
    }

    private BottomSheetDialog createBottomSheet(KeyProfile profile) {
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
            Toast.makeText(this.getApplicationContext(), "Coming soon", Toast.LENGTH_SHORT).show();
        });

        return bottomDialog;
    }

    private void deleteProfile(KeyProfile profile) {
        new AlertDialog.Builder(MainActivity.this)
            .setTitle("Delete entry")
            .setMessage("Are you sure you want to delete this profile?")
            .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                try {
                    _db.removeKey(profile.getEntry());
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "An error occurred while trying to delete an entry", Toast.LENGTH_SHORT).show();
                    return;
                }
                saveDatabase();

                _keyProfileAdapter.removeKey(profile);
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
                Intent preferencesActivity = new Intent(this, PreferencesActivity.class);
                startActivityForResult(preferencesActivity, CODE_PREFERENCES);
                return true;
            case R.id.action_import:
                if (PermissionHelper.request(this, CODE_PERM_IMPORT, Manifest.permission.CAMERA)) {
                    onImport();
                }
                return true;
            case R.id.action_lock:
                // TODO: properly close the database
                recreate();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void initializeAppShortcuts() {
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            return;
        }

        ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
        if (shortcutManager == null) {
            return;
        }

        // TODO: Remove this line
        shortcutManager.removeAllDynamicShortcuts();
        if (shortcutManager.getDynamicShortcuts().size() == 0) {
            // Application restored. Need to re-publish dynamic shortcuts.
            Intent intent = new Intent(this.getBaseContext(), MainActivity.class);
            intent.putExtra("Action", "Scan");
            intent.setAction(Intent.ACTION_MAIN);

            ShortcutInfo shortcut = new ShortcutInfo.Builder(this, "id1")
                    .setShortLabel("New profile")
                    .setLongLabel("Add new profile")
                    .setIcon(Icon.createWithResource(this.getApplicationContext(), R.drawable.intro_scanner))
                    .setIntent(intent)
                    .build();

            shortcutManager.setDynamicShortcuts(Collections.singletonList(shortcut));
        }
    }

    private void setPreferredTheme() {
        boolean restart = false;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPreferences.getBoolean("pref_night_mode", false)) {
            if (!_nightMode) {
                setTheme(R.style.AppTheme_Dark_NoActionBar);
                restart = true;
            }
        } else if (_nightMode) {
            setTheme(R.style.AppTheme_Default_NoActionBar);
            restart = true;
        }

        if (restart) {
            recreate();
        }
    }

    private void saveDatabase() {
        try {
            _db.save();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "An error occurred while trying to save the database", Toast.LENGTH_LONG).show();
        }
    }

    private void loadKeyProfiles() {
        updateLockIcon();

        try {
            for (DatabaseEntry entry : _db.getKeys()) {
                _keyProfileAdapter.addKey(new KeyProfile(entry));
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "An error occurred while trying to load database entries", Toast.LENGTH_SHORT).show();
            return;
        }
    }

    private void updateLockIcon() {
        // hide the lock icon if the database is not encrypted
        if (_menu != null && _db.isDecrypted()) {
            MenuItem item = _menu.findItem(R.id.action_lock);
            item.setVisible(_db.getFile().isEncrypted());
        }
    }

    @Override
    public void onKeyProfileClick(KeyProfile profile) {
        createBottomSheet(profile).show();
    }

    @Override
    public boolean onLongKeyProfileClick(KeyProfile profile) {
        return false;
    }

    @Override
    public void onKeyProfileMove(KeyProfile profile1, KeyProfile profile2) {
        try {
            _db.swapKeys(profile1.getEntry(), profile2.getEntry());
        } catch (Exception e) {
            e.printStackTrace();
            throw new UndeclaredThrowableException(e);
        }
    }

    @Override
    public void onKeyProfileDrop(KeyProfile profile) {
        saveDatabase();
    }
}
