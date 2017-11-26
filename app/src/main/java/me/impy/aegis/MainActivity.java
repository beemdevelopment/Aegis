package me.impy.aegis;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;

import me.impy.aegis.crypto.MasterKey;
import me.impy.aegis.db.DatabaseEntry;
import me.impy.aegis.db.DatabaseManager;
import me.impy.aegis.ext.FreeOTPImporter;
import me.impy.aegis.helpers.SimpleItemTouchHelperCallback;

public class MainActivity extends AppCompatActivity {
    private static final int CODE_GET_KEYINFO = 0;
    private static final int CODE_ADD_KEYINFO = 1;
    private static final int CODE_DO_INTRO = 2;
    private static final int CODE_DECRYPT = 3;
    private static final int CODE_IMPORT = 4;

    private KeyProfileAdapter _keyProfileAdapter;
    private ArrayList<KeyProfile> _keyProfiles = new ArrayList<>();
    private DatabaseManager _db;

    private boolean _nightMode = false;
    private int _clickedItemPosition = -1;

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
                } else {
                    loadKeyProfiles();
                }
            } catch (FileNotFoundException e) {
                // start the intro if the db file was not found
                Toast.makeText(this, "Database file not found, starting over...", Toast.LENGTH_SHORT).show();
                Intent intro = new Intent(this, IntroActivity.class);
                startActivityForResult(intro, CODE_DO_INTRO);
            } catch (Exception e) {
                // TODO: feedback
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
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        initializeAppShortcuts();
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setEnabled(true);
        fab.setOnClickListener(view -> {
            Intent scannerActivity = new Intent(getApplicationContext(), ScannerActivity.class);
            startActivityForResult(scannerActivity, CODE_GET_KEYINFO);
        });

        RecyclerView rvKeyProfiles = (RecyclerView) findViewById(R.id.rvKeyProfiles);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        rvKeyProfiles.setLayoutManager(mLayoutManager);

        _keyProfileAdapter = new KeyProfileAdapter(_keyProfiles);
        _keyProfileAdapter.setOnItemClickListener((position, v) -> {
            _clickedItemPosition = position;
            InitializeBottomSheet().show();
        });
        _keyProfileAdapter.setOnLongItemClickListener((position, v) -> {

        });

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(_keyProfileAdapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(rvKeyProfiles);

        rvKeyProfiles.setAdapter(_keyProfileAdapter);
        Comparator<KeyProfile> comparator = new Comparator<KeyProfile>() {
            @Override
            public int compare(KeyProfile keyProfile, KeyProfile t1) {
                return keyProfile.getEntry().getOrder() - t1.getEntry().getOrder();
            }
        };
        Collections.sort(_keyProfiles, comparator);
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
        }
    }

    private void onImportResult(int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }

        InputStream stream = null;
        try {
            try {
                stream = getContentResolver().openInputStream(data.getData());
            } catch (Exception e) {
                Toast.makeText(this, "An error occurred while trying to open the file", Toast.LENGTH_SHORT).show();
                return;
            }

            FreeOTPImporter importer = new FreeOTPImporter(stream);
            try {
                for (DatabaseEntry profile : importer.convert()) {
                    addKey(new KeyProfile(profile));
                }
            } catch (Exception e) {
                Toast.makeText(this, "An error occurred while trying to parse the file", Toast.LENGTH_SHORT).show();
                return;
            }
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception e) {
                }
            }
        }

        saveDatabase();
    }

    private void onGetKeyInfoResult(int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }

        final KeyProfile keyProfile = (KeyProfile)data.getSerializableExtra("KeyProfile");

        Intent intent = new Intent(this, AddProfileActivity.class);
        intent.putExtra("KeyProfile", keyProfile);
        startActivityForResult(intent, CODE_ADD_KEYINFO);
    }

    private void onAddKeyInfoResult(int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }

        KeyProfile profile = (KeyProfile) data.getSerializableExtra("KeyProfile");
        addKey(profile);

        saveDatabase();
    }

    private void addKey(KeyProfile profile) {
        profile.refreshCode();

        DatabaseEntry entry = profile.getEntry();
        entry.setName(entry.getInfo().getAccountName());
        entry.setOrder(_keyProfiles.size() + 1);
        try {
            _db.addKey(entry);
        } catch (Exception e) {
            e.printStackTrace();
            // TODO: feedback
            return;
        }

        _keyProfiles.add(profile);
        _keyProfileAdapter.notifyDataSetChanged();
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
            // TODO: feedback
            throw new UndeclaredThrowableException(e);
        }

        loadKeyProfiles();
    }

    private void onDecryptResult(int resultCode, Intent data) {
        MasterKey key = (MasterKey) data.getSerializableExtra("key");
        try {
            _db.setMasterKey(key);
        } catch (Exception e) {
            // TODO: feedback
            throw new UndeclaredThrowableException(e);
        }

        loadKeyProfiles();
    }

    @Override
    protected void onResume() {
        super.onResume();
        _keyProfileAdapter.notifyDataSetChanged();
        setPreferredTheme();
    }

    @Override
    protected void onPause() {
        saveDatabase();
        super.onPause();
    }

    private BottomSheetDialog InitializeBottomSheet()
    {
        View bottomSheetView = getLayoutInflater ().inflate (R.layout.bottom_sheet_edit_profile, null);
        LinearLayout copyLayout = (LinearLayout)bottomSheetView.findViewById(R.id.copy_button);
        LinearLayout deleteLayout = (LinearLayout)bottomSheetView.findViewById(R.id.delete_button);
        LinearLayout editLayout = (LinearLayout)bottomSheetView.findViewById(R.id.edit_button);
        bottomSheetView.findViewById(R.id.edit_button);
        BottomSheetDialog bottomDialog =  new BottomSheetDialog(this);
        bottomDialog.setContentView(bottomSheetView);
        bottomDialog.setCancelable (true);
        bottomDialog.getWindow ().setLayout (LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        bottomDialog.show();

        copyLayout.setOnClickListener(view -> {
            bottomDialog.dismiss();
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("text/plain", _keyProfiles.get(_clickedItemPosition).getCode());
            clipboard.setPrimaryClip(clip);

            Toast.makeText(this.getApplicationContext(), "Code successfully copied to the clipboard", Toast.LENGTH_SHORT).show();
        });

        deleteLayout.setOnClickListener(view -> {
            bottomDialog.dismiss();

            KeyProfile keyProfile = _keyProfiles.get(_clickedItemPosition);
            deleteProfile(keyProfile);
        });

        editLayout.setOnClickListener(view -> {
            bottomDialog.dismiss();
            Toast.makeText(this.getApplicationContext(), "Coming soon", Toast.LENGTH_SHORT).show();
        });

        return bottomDialog;
    }

    private void deleteProfile(KeyProfile profile)
    {
        new AlertDialog.Builder(MainActivity.this)
            .setTitle("Delete entry")
            .setMessage("Are you sure you want to delete this profile?")
            .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                try {
                    _db.removeKey(profile.getEntry());
                } catch (Exception e) {
                    e.printStackTrace();
                    //TODO: feedback
                    return;
                }
                _keyProfiles.remove(_clickedItemPosition);
                _keyProfileAdapter.notifyItemRemoved(_clickedItemPosition);
            })
            .setNegativeButton(android.R.string.no, (dialog, which) -> {
            })
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
                startActivity(preferencesActivity);
                return true;
            case R.id.action_import:
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                startActivityForResult(intent, CODE_IMPORT);
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
        String mode = getIntent().getStringExtra("Action");
        if (mode != null) {
            Log.println(Log.DEBUG, "MODE: ", mode);
            if (Objects.equals(mode, "Scan")) {
                Intent scannerActivity = new Intent(getApplicationContext(), ScannerActivity.class);
                startActivityForResult(scannerActivity, CODE_GET_KEYINFO);
            }
        }

        ShortcutManager shortcutManager = null;
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            shortcutManager = getSystemService(ShortcutManager.class);
            if (shortcutManager != null) {
                //TODO: Remove this line
                shortcutManager.removeAllDynamicShortcuts();
                if (shortcutManager.getDynamicShortcuts().size() == 0) {
                    // Application restored. Need to re-publish dynamic shortcuts.

                    Intent intent1 = new Intent(this.getBaseContext(), MainActivity.class);
                    intent1.putExtra("Action", "Scan");
                    intent1.setAction(Intent.ACTION_MAIN);

                    ShortcutInfo shortcut = new ShortcutInfo.Builder(this, "id1")
                            .setShortLabel("New profile")
                            .setLongLabel("Add new profile")
                            .setIcon(Icon.createWithResource(this.getApplicationContext(), R.drawable.intro_scanner))
                            .setIntent(intent1)
                            .build();

                    shortcutManager.setDynamicShortcuts(Arrays.asList(shortcut));
                }
            }
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
        } else {
            if (_nightMode) {
                setTheme(R.style.AppTheme_Default_NoActionBar);
                restart = true;
            }
        }

        if (restart) {
            finish();
            startActivity(new Intent(this, this.getClass()));
        }
    }

    private void saveDatabase() {
        if (!_db.isDecrypted()) {
            return;
        }

        try {
            _db.save();
        } catch (Exception e) {
            //TODO: feedback
            throw new UndeclaredThrowableException(e);
        }
    }

    private void loadKeyProfiles() {
        updateLockIcon();

        try {
            for (DatabaseEntry entry : _db.getKeys()) {
                _keyProfiles.add(new KeyProfile(entry));
            }
            _keyProfileAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateLockIcon() {
        // hide the lock icon if the database is not encrypted
        if (_menu != null && _db.isDecrypted()) {
            MenuItem item = _menu.findItem(R.id.action_lock);
            item.setVisible(_db.getFile().isEncrypted());
        }
    }
}
