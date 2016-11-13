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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;

import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;

import me.impy.aegis.crypto.CryptParameters;
import me.impy.aegis.crypto.CryptResult;
import me.impy.aegis.crypto.CryptoUtils;
import me.impy.aegis.crypto.DerivationParameters;
import me.impy.aegis.crypto.KeyStoreHandle;
import me.impy.aegis.crypto.OTP;
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
    Database database;
    DatabaseFile databaseFile;

    boolean blockSave = false;
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
        fab.setEnabled(false);
        fab.setOnClickListener(view -> {
            blockSave = true;
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

        loadDatabase(null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        blockSave = false;
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
                saveDatabase(true, null);
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
        if (!blockSave) {
            // update order of keys
            for (int i = 0; i < mKeyProfiles.size(); i++) {
                try {
                    database.updateKey(mKeyProfiles.get(i));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            saveDatabase(false, null);
        }

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
                    saveDatabase(false, cipher);
                    break;
                case LOAD:
                    loadDatabase(cipher);
                    break;
            }
        }
    }

    private void saveDatabase(boolean allowPrompt, Cipher cipher) {
        try {
            byte[] bytes = database.serialize();
            CryptParameters cryptParams = null;
            DerivationParameters derParams = null;

            switch (databaseFile.getLevel()) {
                case DatabaseFile.SEC_LEVEL_DERIVED:
                    // TODO
                    break;
                case DatabaseFile.SEC_LEVEL_KEYSTORE:
                    if (cipher == null) {
                        KeyStoreHandle keyStore = new KeyStoreHandle();
                        SecretKey key = keyStore.getKey();
                        cipher = CryptoUtils.createCipher(key, Cipher.ENCRYPT_MODE);
                    }

                    CryptResult result = CryptoUtils.encrypt(bytes, cipher);
                    bytes = result.Data;
                    cryptParams = result.Parameters;
                    break;
            }

            databaseFile.setContent(bytes);
            databaseFile.setCryptParameters(cryptParams);
            databaseFile.setDerivationParameters(derParams);
            databaseFile.save();
        } catch (IllegalBlockSizeException e) {
            // TODO: is there a way to catch "Key user not authenticated" specifically aside from checking the exception message?
            if (causeIsKeyUserNotAuthenticated(e) && allowPrompt && cipher != null) {
                promptFingerPrint(FingerprintAuthenticationDialogFragment.Action.SAVE, cipher);
            }
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
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
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {

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

    private void loadDatabase(Cipher cipher) {
        try {
            databaseFile = DatabaseFile.load(getApplicationContext());
        } catch (IOException e) {
            // this file doesn't exist yet
            try {
                // TODO: prompt for security settings (level, auth, etc)
                database = new Database();
                databaseFile = new DatabaseFile(getApplicationContext());
                databaseFile.setLevel(DatabaseFile.SEC_LEVEL_KEYSTORE);

                if (databaseFile.getLevel() == DatabaseFile.SEC_LEVEL_KEYSTORE) {
                    KeyStoreHandle store = new KeyStoreHandle();
                    if (!store.keyExists()) {
                        store.generateKey(true);
                    }
                }
            } catch (Exception ex) {
                e.printStackTrace();
                return;
            }
        } catch (Exception e) {
            // something else went wrong
            e.printStackTrace();
            return;
        }

        if (database == null) {
            byte[] content = databaseFile.getContent();
            switch (databaseFile.getLevel()) {
                case DatabaseFile.SEC_LEVEL_NONE:
                    try {
                        Database temp = new Database();
                        temp.deserialize(content);
                        database = temp;
                    } catch (Exception e) {
                        // TODO: handle corrupt database
                        e.printStackTrace();
                        return;
                    }
                    break;
                case DatabaseFile.SEC_LEVEL_DERIVED:
                    // TODO: prompt for pin/pass
                    /*CryptParameters cryptParams = dbFile.getCryptParameters();
                    DerivationParameters derParams = dbFile.getDerivationParameters();
                    SecretKey key = CryptoUtils.deriveKey("password".toCharArray(), derParams.Salt, (int)derParams.IterationCount);*/

                    break;
                case DatabaseFile.SEC_LEVEL_KEYSTORE:
                    // TODO: prompt for fingerprint if auth is required
                    try {
                        CryptParameters params = databaseFile.getCryptParameters();

                        if (cipher == null) {
                            KeyStoreHandle store = new KeyStoreHandle();
                            SecretKey key = store.getKey();
                            cipher = CryptoUtils.createCipher(key, Cipher.DECRYPT_MODE, params.Nonce);
                        }

                        CryptResult result = null;
                        //try {
                            result = CryptoUtils.decrypt(content, cipher, params);
                        //} catch (Exception e) {
                        //    // we probably need to authenticate ourselves
                        //    promptFingerPrint(1, cipher);
                        //}
                        if (result != null) {
                            database = new Database();
                            database.deserialize(result.Data);
                        }
                    } catch (IllegalBlockSizeException e) {
                        if (causeIsKeyUserNotAuthenticated(e) && cipher != null) {
                            promptFingerPrint(FingerprintAuthenticationDialogFragment.Action.LOAD, cipher);
                        }
                        e.printStackTrace();
                        return;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return;
                    }
                    break;
                default:
                    // TODO: handle unknown security level
                    return;
            }
        }

        try {
            mKeyProfiles.addAll(database.getKeys());
            mKeyProfileAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setEnabled(true);
    }

    private boolean causeIsKeyUserNotAuthenticated(Exception e) {
        // TODO: is there a way to catch "Key user not authenticated" specifically aside from checking the exception message?
        return e.getCause().getMessage().equals("Key user not authenticated");
    }
}
