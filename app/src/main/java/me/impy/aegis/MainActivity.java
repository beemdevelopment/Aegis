package me.impy.aegis;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.hardware.fingerprint.FingerprintManager;
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

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import me.impy.aegis.crypto.CryptResult;
import me.impy.aegis.crypto.CryptoUtils;
import me.impy.aegis.crypto.MasterKey;
import me.impy.aegis.crypto.OTP;
import me.impy.aegis.crypto.slots.PasswordSlot;
import me.impy.aegis.crypto.slots.Slot;
import me.impy.aegis.crypto.slots.SlotCollection;
import me.impy.aegis.db.Database;
import me.impy.aegis.db.DatabaseFile;
import me.impy.aegis.finger.FingerprintAuthenticationDialogFragment;
import me.impy.aegis.helpers.SimpleItemTouchHelperCallback;

public class MainActivity extends AppCompatActivity {

    static final int GET_KEYINFO = 1;
    static final int ADD_KEYINFO = 2;

    RecyclerView rvKeyProfiles;
    KeyProfileAdapter mKeyProfileAdapter;
    ArrayList<KeyProfile> mKeyProfiles = new ArrayList<>();
    MasterKey masterKey;
    Database database;
    DatabaseFile databaseFile;

    boolean nightMode = false;
    int clickedItemPosition = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = this.getSharedPreferences("me.impy.aegis", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("passedIntro", false)) {
            Intent intro = new Intent(this, IntroActivity.class);
            startActivity(intro);
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPreferences.getBoolean("pref_night_mode", false)) {
            nightMode = true;
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
            startActivityForResult(scannerActivity, GET_KEYINFO);
        });

        rvKeyProfiles = (RecyclerView) findViewById(R.id.rvKeyProfiles);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        rvKeyProfiles.setLayoutManager(mLayoutManager);

        mKeyProfileAdapter = new KeyProfileAdapter(mKeyProfiles);
        mKeyProfileAdapter.setOnItemClickListener((position, v) -> {
            clickedItemPosition = position;
            InitializeBottomSheet().show();
        });
        mKeyProfileAdapter.setOnLongItemClickListener((position, v) -> {

        });

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mKeyProfileAdapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(rvKeyProfiles);

        rvKeyProfiles.setAdapter(mKeyProfileAdapter);
        Comparator<KeyProfile> comparator = new Comparator<KeyProfile>() {
            @Override
            public int compare(KeyProfile keyProfile, KeyProfile t1) {
                return keyProfile.Order - t1.Order;
            }
        };
        Collections.sort(mKeyProfiles, comparator);

        loadDatabase();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == GET_KEYINFO) {
            if (resultCode == RESULT_OK) {
                final KeyProfile keyProfile = (KeyProfile)data.getSerializableExtra("KeyProfile");

                Intent intent = new Intent(this, AddProfileActivity.class);
                intent.putExtra("KeyProfile", keyProfile);
                startActivityForResult(intent, ADD_KEYINFO);
            }
        }
        else if (requestCode == ADD_KEYINFO) {
            if (resultCode == RESULT_OK) {
                final KeyProfile keyProfile = (KeyProfile) data.getSerializableExtra("KeyProfile");

                String otp;
                try {
                    otp = OTP.generateOTP(keyProfile.Info);
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }

                keyProfile.Order = mKeyProfiles.size() + 1;
                keyProfile.Code = otp;
                try {
                    database.addKey(keyProfile);
                } catch (Exception e) {
                    e.printStackTrace();
                    // TODO: feedback
                    return;
                }

                mKeyProfiles.add(keyProfile);
                mKeyProfileAdapter.notifyDataSetChanged();
                saveDatabase();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mKeyProfileAdapter.notifyDataSetChanged();
        setPreferredTheme();
    }

    @Override
    protected void onPause() {
        // update order of keys
        for (int i = 0; i < mKeyProfiles.size(); i++) {
            try {
                database.updateKey(mKeyProfiles.get(i));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        saveDatabase();

        super.onPause();
    }

    private void promptFingerPrint(FingerprintAuthenticationDialogFragment.Action action, Cipher cipher) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            FingerprintAuthenticationDialogFragment fragment = new FingerprintAuthenticationDialogFragment();
            fragment.setCryptoObject(new FingerprintManager.CryptoObject(cipher));
            fragment.setStage(FingerprintAuthenticationDialogFragment.Stage.FINGERPRINT);
            fragment.setAction(action);
            fragment.show(getFragmentManager(), "");
        }
    }

    public void onAuthenticated(FingerprintAuthenticationDialogFragment.Action action, FingerprintManager.CryptoObject obj) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Cipher cipher = obj.getCipher();
            switch (action) {
                case SAVE:
                    saveDatabase();
                    break;
                case LOAD:
                    loadDatabase();
                    break;
            }
        }
    }

    private void saveDatabase() {
        try {
            byte[] bytes = database.serialize();
            CryptResult result = masterKey.encrypt(bytes);
            databaseFile.setContent(result.Data);
            databaseFile.setCryptParameters(result.Parameters);
            databaseFile.save(getApplicationContext());
        } catch (Exception e) {
            throw new UndeclaredThrowableException(e);
        }
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
            ClipData clip = ClipData.newPlainText("text/plain", mKeyProfiles.get(clickedItemPosition).Code);
            clipboard.setPrimaryClip(clip);

            Toast.makeText(this.getApplicationContext(), "Code successfully copied to the clipboard", Toast.LENGTH_SHORT).show();
        });

        deleteLayout.setOnClickListener(view -> {
            bottomDialog.dismiss();

            KeyProfile keyProfile = mKeyProfiles.get(clickedItemPosition);
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
                    database.removeKey(profile);
                } catch (Exception e) {
                    e.printStackTrace();
                    //TODO: feedback
                    return;
                }
                mKeyProfiles.remove(clickedItemPosition);
                mKeyProfileAdapter.notifyItemRemoved(clickedItemPosition);
            })
            .setNegativeButton(android.R.string.no, (dialog, which) -> {
            })
            .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            Intent preferencesActivity = new Intent(this, PreferencesActivity.class);
            startActivity(preferencesActivity);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initializeAppShortcuts()
    {
        String mode = getIntent().getStringExtra("Action");
        if(mode != null)
        {
            Log.println(Log.DEBUG, "MODE: ", mode);
            if(Objects.equals(mode, "Scan"))
            {
                Log.println(Log.DEBUG, "OKK ", "OKKK");
                Intent scannerActivity = new Intent(getApplicationContext(), ScannerActivity.class);
                startActivityForResult(scannerActivity, GET_KEYINFO);
            }
        }

        ShortcutManager shortcutManager = null;
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            shortcutManager = getSystemService(ShortcutManager.class);
            if(shortcutManager != null) {
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

    private void setPreferredTheme()
    {
        boolean restart = false;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if(sharedPreferences.getBoolean("pref_night_mode", false)) {
            if(!nightMode) {
                setTheme(R.style.AppTheme_Dark_NoActionBar);
                restart = true;
            }
        } else {
            if(nightMode) {
                setTheme(R.style.AppTheme_Default_NoActionBar);
                restart = true;
            }
        }

        if(restart){
            finish();
            startActivity(new Intent(this, this.getClass()));
        }
    }

    private void createDatabase() {
        database = new Database();
        try {
            databaseFile = new DatabaseFile();
        } catch (Exception e) {
            // TODO: tell the user to stop using a weird platform
            throw new UndeclaredThrowableException(e);
        }

        try {
            masterKey = new MasterKey(null);
        } catch (NoSuchAlgorithmException e) {
            // TODO: tell the user to stop using a weird platform
            throw new UndeclaredThrowableException(e);
        }

        SlotCollection slots = databaseFile.getSlots();

        try {
            PasswordSlot slot = new PasswordSlot();
            byte[] salt = CryptoUtils.generateSalt();
            SecretKey derivedKey = slot.deriveKey("testpassword".toCharArray(), salt, 1000);
            Cipher cipher = Slot.createCipher(derivedKey, Cipher.ENCRYPT_MODE);
            masterKey.encryptSlot(slot, cipher);
            slots.add(slot);
        } catch (Exception e) {
            throw new UndeclaredThrowableException(e);
        }
    }

    private void loadDatabase() {
        try {
            databaseFile = DatabaseFile.load(getApplicationContext());
        } catch (IOException e) {
            // the database file doesn't exist yet
            createDatabase();
            saveDatabase();
            return;
        } catch (Exception e) {
            // something else went wrong
            throw new UndeclaredThrowableException(e);
        }

        byte[] content = databaseFile.getContent();
        if (databaseFile.isEncrypted()) {
            SlotCollection slots = databaseFile.getSlots();
            for (Slot slot : slots) {
                if (slot instanceof PasswordSlot) {
                    try {
                        PasswordSlot derSlot = (PasswordSlot)slot;
                        SecretKey derivedKey = derSlot.deriveKey("testpassword".toCharArray());
                        Cipher cipher = Slot.createCipher(derivedKey, Cipher.DECRYPT_MODE);
                        masterKey = MasterKey.decryptSlot(slot, cipher);
                    } catch (Exception e) {
                        throw new UndeclaredThrowableException(e);
                    }
                    break;
                } else {

                }
            }

            CryptResult result;
            try {
                result = masterKey.decrypt(content, databaseFile.getCryptParameters());
            } catch (Exception e) {
                throw new UndeclaredThrowableException(e);
            }

            content = result.Data;
        }

        database = new Database();
        try {
            database.deserialize(content);
        } catch (Exception e) {
            throw new UndeclaredThrowableException(e);
        }

        try {
            mKeyProfiles.addAll(database.getKeys());
            mKeyProfileAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            throw new UndeclaredThrowableException(e);
        }
    }

    private boolean causeIsKeyUserNotAuthenticated(Exception e) {
        // TODO: is there a way to catch "Key user not authenticated" specifically aside from checking the exception message?
        return e.getCause().getMessage().equals("Key user not authenticated");
    }
}
