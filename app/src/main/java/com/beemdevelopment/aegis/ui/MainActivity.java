package com.beemdevelopment.aegis.ui;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.SearchView;

import com.beemdevelopment.aegis.AegisApplication;
import com.beemdevelopment.aegis.Preferences;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.SortCategory;
import com.beemdevelopment.aegis.ViewMode;
import com.beemdevelopment.aegis.helpers.BitmapHelper;
import com.beemdevelopment.aegis.helpers.FabScrollHelper;
import com.beemdevelopment.aegis.helpers.PermissionHelper;
import com.beemdevelopment.aegis.helpers.QrCodeAnalyzer;
import com.beemdevelopment.aegis.otp.GoogleAuthInfo;
import com.beemdevelopment.aegis.otp.GoogleAuthInfoException;
import com.beemdevelopment.aegis.ui.dialogs.Dialogs;
import com.beemdevelopment.aegis.ui.fragments.BackupsPreferencesFragment;
import com.beemdevelopment.aegis.ui.fragments.PreferencesFragment;
import com.beemdevelopment.aegis.ui.views.EntryListView;
import com.beemdevelopment.aegis.vault.VaultEntry;
import com.beemdevelopment.aegis.vault.VaultFile;
import com.beemdevelopment.aegis.vault.VaultManager;
import com.beemdevelopment.aegis.vault.VaultManagerException;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AegisActivity implements EntryListView.Listener {
    // activity request codes
    private static final int CODE_SCAN = 0;
    private static final int CODE_ADD_ENTRY = 1;
    private static final int CODE_EDIT_ENTRY = 2;
    private static final int CODE_DO_INTRO = 3;
    private static final int CODE_DECRYPT = 4;
    private static final int CODE_PREFERENCES = 5;
    private static final int CODE_SCAN_IMAGE = 6;

    // permission request codes
    private static final int CODE_PERM_CAMERA = 0;
    private static final int CODE_PERM_READ_STORAGE = 1;

    private AegisApplication _app;
    private VaultManager _vault;
    private boolean _loaded;
    private boolean _searchSubmitted;

    private boolean _isAuthenticating;
    private boolean _isDoingIntro;

    private List<VaultEntry> _selectedEntries;
    private ActionMode _actionMode;

    private Menu _menu;
    private SearchView _searchView;
    private EntryListView _entryListView;
    private LinearLayout _btnBackupError;

    private FabScrollHelper _fabScrollHelper;

    private ActionMode.Callback _actionModeCallbacks = new ActionModeCallbacks();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.toolbar));

        _app = (AegisApplication) getApplication();
        _vault = _app.getVaultManager();
        _loaded = false;

        if (savedInstanceState != null) {
            _isAuthenticating = savedInstanceState.getBoolean("isAuthenticating");
            _isDoingIntro = savedInstanceState.getBoolean("isDoingIntro");
        }

        _entryListView = (EntryListView) getSupportFragmentManager().findFragmentById(R.id.key_profiles);
        _entryListView.setListener(this);
        _entryListView.setCodeGroupSize(getPreferences().getCodeGroupSize());
        _entryListView.setShowAccountName(getPreferences().isAccountNameVisible());
        _entryListView.setHighlightEntry(getPreferences().isEntryHighlightEnabled());
        _entryListView.setTapToReveal(getPreferences().isTapToRevealEnabled());
        _entryListView.setTapToRevealTime(getPreferences().getTapToRevealTime());
        _entryListView.setSortCategory(getPreferences().getCurrentSortCategory(), false);
        _entryListView.setViewMode(getPreferences().getCurrentViewMode());
        _entryListView.setIsCopyOnTapEnabled(getPreferences().isCopyOnTapEnabled());
        _entryListView.setPrefGroupFilter(getPreferences().getGroupFilter());

         FloatingActionButton fab = findViewById(R.id.fab);
         fab.setOnClickListener(v -> {
             View view = getLayoutInflater().inflate(R.layout.dialog_add_entry, null);
             BottomSheetDialog dialog = new BottomSheetDialog(this);
             dialog.setContentView(view);

             view.findViewById(R.id.fab_enter).setOnClickListener(v1 -> {
                 dialog.dismiss();
                 startEditEntryActivityForManual(CODE_ADD_ENTRY);
             });
             view.findViewById(R.id.fab_scan_image).setOnClickListener(v2 -> {
                 dialog.dismiss();
                 startScanImageActivity();
             });
             view.findViewById(R.id.fab_scan).setOnClickListener(v3 -> {
                 dialog.dismiss();
                 startScanActivity();
             });

             Dialogs.showSecureDialog(dialog);
         });

        _btnBackupError = findViewById(R.id.btn_backup_error);
        _btnBackupError.setOnClickListener(view -> {
            startPreferencesActivity(BackupsPreferencesFragment.class, "pref_backups");
        });

        _fabScrollHelper = new FabScrollHelper(fab);
        _selectedEntries = new ArrayList<>();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("isAuthenticating", _isAuthenticating);
        outState.putBoolean("isDoingIntro", _isDoingIntro);
    }

    @Override
    protected void onDestroy() {
        _entryListView.setListener(null);
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        Map<UUID, Integer> usageMap = _entryListView.getUsageCounts();
        if (usageMap != null) {
            getPreferences().setUsageCount(usageMap);
        }

        super.onPause();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        _isAuthenticating = false;
        _isDoingIntro = false;

        if (resultCode != RESULT_OK) {
            return;
        }

        switch (requestCode) {
            case CODE_SCAN:
                onScanResult(data);
                break;
            case CODE_ADD_ENTRY:
                onAddEntryResult(data);
                break;
            case CODE_EDIT_ENTRY:
                onEditEntryResult(data);
                break;
            case CODE_DO_INTRO:
                onDoIntroResult();
                break;
            case CODE_DECRYPT:
                onDecryptResult();
                break;
            case CODE_PREFERENCES:
                onPreferencesResult(data);
                break;
            case CODE_SCAN_IMAGE:
                onScanImageResult(data);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (!PermissionHelper.checkResults(grantResults)) {
            Toast.makeText(this, getString(R.string.permission_denied), Toast.LENGTH_SHORT).show();
            return;
        }

        switch (requestCode) {
            case CODE_PERM_CAMERA:
                startScanActivity();
                break;
            case CODE_PERM_READ_STORAGE:
                startScanImageActivity();
                break;
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void onPreferencesResult(Intent data) {
        // refresh the entire entry list if needed
        if (_loaded) {
            if (data.getBooleanExtra("needsRecreate", false)) {
                recreate();
            } else if (data.getBooleanExtra("needsRefresh", false)) {
                boolean showAccountName = getPreferences().isAccountNameVisible();
                int codeGroupSize = getPreferences().getCodeGroupSize();
                boolean highlightEntry = getPreferences().isEntryHighlightEnabled();
                boolean tapToReveal = getPreferences().isTapToRevealEnabled();
                int tapToRevealTime = getPreferences().getTapToRevealTime();
                ViewMode viewMode = getPreferences().getCurrentViewMode();
                boolean copyOnTap = getPreferences().isCopyOnTapEnabled();
                _entryListView.setShowAccountName(showAccountName);
                _entryListView.setCodeGroupSize(codeGroupSize);
                _entryListView.setHighlightEntry(highlightEntry);
                _entryListView.setTapToReveal(tapToReveal);
                _entryListView.setTapToRevealTime(tapToRevealTime);
                _entryListView.setViewMode(viewMode);
                _entryListView.setIsCopyOnTapEnabled(copyOnTap);
                _entryListView.refresh(true);
            }
        }
    }

    private void startEditEntryActivityForNew(int requestCode, VaultEntry entry) {
        Intent intent = new Intent(this, EditEntryActivity.class);
        intent.putExtra("newEntry", entry);
        intent.putExtra("isManual", false);
        startActivityForResult(intent, requestCode);
    }

    private void startEditEntryActivityForManual(int requestCode) {
        Intent intent = new Intent(this, EditEntryActivity.class);
        intent.putExtra("newEntry", VaultEntry.getDefault());
        intent.putExtra("isManual", true);
        startActivityForResult(intent, requestCode);
    }

    private void startEditEntryActivity(int requestCode, VaultEntry entry) {
        Intent intent = new Intent(this, EditEntryActivity.class);
        intent.putExtra("entryUUID", entry.getUUID());
        startActivityForResult(intent, requestCode);
    }

    private void onScanResult(Intent data) {
        List<VaultEntry> entries = (ArrayList<VaultEntry>) data.getSerializableExtra("entries");
        if (entries.size() == 1) {
            startEditEntryActivityForNew(CODE_ADD_ENTRY, entries.get(0));
        } else {
            for (VaultEntry entry : entries) {
                _vault.addEntry(entry);
                if (_loaded) {
                    _entryListView.addEntry(entry);
                }
            }

            saveVault(true);
        }
    }

    private void onAddEntryResult(Intent data) {
        if (_loaded) {
            UUID entryUUID = (UUID) data.getSerializableExtra("entryUUID");
            VaultEntry entry = _vault.getEntryByUUID(entryUUID);
            _entryListView.addEntry(entry, true);
        }
    }

    private void onEditEntryResult(Intent data) {
        if (_loaded) {
            UUID entryUUID = (UUID) data.getSerializableExtra("entryUUID");

            if (data.getBooleanExtra("delete", false)) {
                _entryListView.removeEntry(entryUUID);
            } else {
                VaultEntry entry = _vault.getEntryByUUID(entryUUID);
                _entryListView.replaceEntry(entryUUID, entry);
            }
        }
    }

    private void onScanImageResult(Intent intent) {
        decodeQrCodeImage(intent.getData());
    }

    private void decodeQrCodeImage(Uri inputFile) {
        Bitmap bitmap;

        try {
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();

            try (InputStream inputStream = getContentResolver().openInputStream(inputFile)) {
                bitmap = BitmapFactory.decodeStream(inputStream, null, bmOptions);
                bitmap = BitmapHelper.resize(bitmap, QrCodeAnalyzer.RESOLUTION.getWidth(), QrCodeAnalyzer.RESOLUTION.getHeight());
            }

            int[] intArray = new int[bitmap.getWidth() * bitmap.getHeight()];
            bitmap.getPixels(intArray, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

            LuminanceSource source = new RGBLuminanceSource(bitmap.getWidth(), bitmap.getHeight(), intArray);
            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));

            Reader reader = new QRCodeReader();
            Result result = reader.decode(binaryBitmap);

            GoogleAuthInfo info = GoogleAuthInfo.parseUri(result.getText());
            VaultEntry entry = new VaultEntry(info);

            startEditEntryActivityForNew(CODE_ADD_ENTRY, entry);
        } catch (NotFoundException | IOException | ChecksumException | FormatException | GoogleAuthInfoException e) {
            e.printStackTrace();
            Dialogs.showErrorDialog(this, R.string.unable_to_read_qrcode, e);
        }
    }

    private void updateSortCategoryMenu() {
        SortCategory category = getPreferences().getCurrentSortCategory();
        _menu.findItem(category.getMenuItem()).setChecked(true);
    }

    private void onDoIntroResult() {
        _vault = _app.getVaultManager();
        loadEntries();
        checkTimeSyncSetting();
    }

    private void checkTimeSyncSetting() {
        boolean autoTime = Settings.Global.getInt(getContentResolver(), Settings.Global.AUTO_TIME, 1) == 1;
        if (!autoTime && getPreferences().isTimeSyncWarningEnabled()) {
            Dialogs.showTimeSyncWarningDialog(this, (dialog, which) -> {
                Intent intent = new Intent(Settings.ACTION_DATE_SETTINGS);
                startActivity(intent);
            });
        }
    }

    private void onDecryptResult() {
        _vault = _app.getVaultManager();
        loadEntries();
        checkTimeSyncSetting();
    }

    private void startScanActivity() {
        if (!PermissionHelper.request(this, CODE_PERM_CAMERA, Manifest.permission.CAMERA)) {
            return;
        }

        Intent scannerActivity = new Intent(getApplicationContext(), ScannerActivity.class);
        startActivityForResult(scannerActivity, CODE_SCAN);
    }

    private void startScanImageActivity() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setDataAndType(android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI, "image/*");

        Intent fileIntent = new Intent(Intent.ACTION_GET_CONTENT);
        fileIntent.setType("image/*");

        Intent chooserIntent = Intent.createChooser(galleryIntent, getString(R.string.select_picture));
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] { fileIntent });
        AegisActivity.Helper.startExtActivityForResult(this, chooserIntent, CODE_SCAN_IMAGE);
    }

    private void startPreferencesActivity() {
        startPreferencesActivity(null, null);
    }

    private void startPreferencesActivity(Class<? extends PreferencesFragment> fragmentType, String preference) {
        Intent intent = new Intent(this, PreferencesActivity.class);
        intent.putExtra("fragment", fragmentType);
        intent.putExtra("pref", preference);
        startActivityForResult(intent, CODE_PREFERENCES);
    }

    private void doShortcutActions() {
        Intent intent = getIntent();
        String action = intent.getStringExtra("action");
        if (action == null || _app.isVaultLocked()) {
            return;
        }

        switch (action) {
            case "scan":
                startScanActivity();
                break;
        }

        intent.removeExtra("action");
    }

    private void handleDeeplink() {
        if (_app.isVaultLocked()) {
            return;
        }

        Intent intent = getIntent();
        Uri uri = intent.getData();
        if (Intent.ACTION_VIEW.equals(intent.getAction()) && uri != null) {
            intent.setData(null);
            intent.setAction(null);

            GoogleAuthInfo info = null;
            try {
                info = GoogleAuthInfo.parseUri(uri);
            } catch (GoogleAuthInfoException e) {
                e.printStackTrace();
                Dialogs.showErrorDialog(this, R.string.unable_to_read_qrcode, e);
            }

            if (info != null) {
                VaultEntry entry = new VaultEntry(info);
                startEditEntryActivityForNew(CODE_ADD_ENTRY, entry);
            }
        }
    }

    private void handleSharedImage() {
        if (_app.isVaultLocked()) {
            return;
        }

        Intent intent = getIntent();
        String action = intent.getAction();
        Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);

        if (Intent.ACTION_SEND.equals(action) && uri != null) {
            intent.setAction(null);
            intent.removeExtra(Intent.EXTRA_STREAM);

            decodeQrCodeImage(uri);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (_vault == null) {
            // start the intro if the vault file doesn't exist
            if (!_isDoingIntro && !VaultManager.fileExists(this)) {
                if (getPreferences().isIntroDone()) {
                    Toast.makeText(this, getString(R.string.vault_not_found), Toast.LENGTH_SHORT).show();
                }
                Intent intro = new Intent(this, IntroActivity.class);
                startActivityForResult(intro, CODE_DO_INTRO);
                _isDoingIntro = true;
                return;
            }

            // read the vault from disk
            // if this fails, show the error to the user and close the app
            try {
                VaultFile vaultFile = _app.loadVaultFile();
                if (!vaultFile.isEncrypted()) {
                    _vault = _app.initVaultManager(vaultFile, null);
                }
            } catch (VaultManagerException e) {
                e.printStackTrace();
                Dialogs.showErrorDialog(this, R.string.vault_load_error, e, (dialog1, which) -> finish());
                return;
            }
        }

        if (_app.isVaultLocked()) {
            startAuthActivity(false);
        } else if (_loaded) {
            // update the list of groups in the entry list view so that the chip gets updated
            _entryListView.setGroups(_vault.getGroups());

            // update the usage counts in case they are edited outside of the entrylistview
            _entryListView.setUsageCounts(getPreferences().getUsageCounts());

            // refresh all codes to prevent showing old ones
            _entryListView.refresh(false);
        } else {
            loadEntries();
            checkTimeSyncSetting();
        }

        handleDeeplink();
        handleSharedImage();
        updateLockIcon();
        doShortcutActions();
        updateBackupErrorBar();
    }

    @Override
    public void onBackPressed() {
        if (!_searchView.isIconified() || _searchSubmitted) {
            _searchSubmitted = false;
            _entryListView.setSearchFilter(null);

            collapseSearchView();
            setTitle(R.string.app_name);
            return;
        }

        if (_app.isAutoLockEnabled(Preferences.AUTO_LOCK_ON_BACK_BUTTON)) {
            _app.lock(false);
            return;
        }

        super.onBackPressed();
    }

    private void deleteEntries(List<VaultEntry> entries) {
        for (VaultEntry entry: entries) {
            VaultEntry oldEntry = _vault.removeEntry(entry);
            _entryListView.removeEntry(oldEntry);
        }

        saveVault(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        _menu = menu;
        getMenuInflater().inflate(R.menu.menu_main, menu);
        updateLockIcon();
        if (_loaded) {
            _entryListView.setGroups(_vault.getGroups());
            updateSortCategoryMenu();
        }

        MenuItem searchViewMenuItem = menu.findItem(R.id.mi_search);

        _searchView = (SearchView) searchViewMenuItem.getActionView();
        _searchView.setFocusable(false);
        _searchView.setQueryHint(getString(R.string.search));
        _searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                setTitle(getString(R.string.search));
                getSupportActionBar().setSubtitle(s);
                _searchSubmitted = true;
                collapseSearchView();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                if (!_searchSubmitted) {
                    _entryListView.setSearchFilter(s);
                }
                return false;
            }
        });
        _searchView.setOnSearchClickListener(v -> {
            if (_searchSubmitted) {
                _searchSubmitted = false;
                _entryListView.setSearchFilter(null);
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings: {
                startPreferencesActivity();
                return true;
            }
            case R.id.action_about: {
                Intent intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
                return true;
            }
            case R.id.action_lock:
                _app.lock(true);
                return true;
            default:
                if (item.getGroupId() == R.id.action_sort_category) {
                    item.setChecked(true);

                    SortCategory sortCategory;
                    switch (item.getItemId()) {
                        case R.id.menu_sort_alphabetically:
                            sortCategory = SortCategory.ISSUER;
                            break;
                        case R.id.menu_sort_alphabetically_reverse:
                            sortCategory = SortCategory.ISSUER_REVERSED;
                            break;
                        case R.id.menu_sort_alphabetically_name:
                            sortCategory = SortCategory.ACCOUNT;
                            break;
                        case R.id.menu_sort_alphabetically_name_reverse:
                            sortCategory = SortCategory.ACCOUNT_REVERSED;
                            break;
                        case R.id.menu_sort_usage_count:
                            sortCategory = SortCategory.USAGE_COUNT;
                            break;
                        case R.id.menu_sort_custom:
                        default:
                            sortCategory = SortCategory.CUSTOM;
                            break;
                    }

                    _entryListView.setSortCategory(sortCategory, true);
                    getPreferences().setCurrentSortCategory(sortCategory);
                }
                return super.onOptionsItemSelected(item);
        }
    }

    private void collapseSearchView() {
        _searchView.setQuery(null, false);
        _searchView.setIconified(true);
    }

    private void loadEntries() {
        if (!_loaded) {
            _entryListView.setUsageCounts(getPreferences().getUsageCounts());
            _entryListView.addEntries(_vault.getEntries());
            _entryListView.runEntriesAnimation();
            _loaded = true;
        }
    }

    private void startAuthActivity(boolean inhibitBioPrompt) {
        if (!_isAuthenticating) {
            Intent intent = new Intent(this, AuthActivity.class);
            intent.putExtra("inhibitBioPrompt", inhibitBioPrompt);
            startActivityForResult(intent, CODE_DECRYPT);
            _isAuthenticating = true;
        }
    }

    private void updateLockIcon() {
        // hide the lock icon if the vault is not unlocked
        if (_menu != null && !_app.isVaultLocked()) {
            MenuItem item = _menu.findItem(R.id.action_lock);
            item.setVisible(_vault.isEncryptionEnabled());
        }
    }

    private void updateBackupErrorBar() {
        String error = null;
        if (_app.getPreferences().isBackupsEnabled()) {
            error = _app.getPreferences().getBackupsError();
        }

        _btnBackupError.setVisibility(error == null ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onEntryClick(VaultEntry entry) {
        if (_actionMode != null) {
            if (_selectedEntries.isEmpty()) {
                _actionMode.finish();
            } else {
                setIsMultipleSelected(_selectedEntries.size() > 1);
            }

            return;
        }
    }

    @Override
    public void onSelect(VaultEntry entry) {
        _selectedEntries.add(entry);
    }

    @Override
    public void onDeselect(VaultEntry entry) {
        _selectedEntries.remove(entry);
    }

    private void setIsMultipleSelected(boolean multipleSelected) {
        _entryListView.setIsLongPressDragEnabled(!multipleSelected);
        _actionMode.getMenu().findItem(R.id.action_edit).setVisible(!multipleSelected);
        _actionMode.getMenu().findItem(R.id.action_copy).setVisible(!multipleSelected);
    }

    @Override
    public void onLongEntryClick(VaultEntry entry) {
        if (!_selectedEntries.isEmpty()) {
            return;
        }

        _selectedEntries.add(entry);
        _entryListView.setActionModeState(true, entry);
        _actionMode = this.startSupportActionMode(_actionModeCallbacks);
    }

    @Override
    public void onEntryMove(VaultEntry entry1, VaultEntry entry2) {
        _vault.swapEntries(entry1, entry2);
    }

    @Override
    public void onEntryDrop(VaultEntry entry) {
        saveVault(false);
    }

    @Override
    public void onEntryChange(VaultEntry entry) {
        saveVault(true);
    }

    public void onEntryCopy(VaultEntry entry) {
        copyEntryCode(entry);
    }

    @Override
    public void onScroll(int dx, int dy) {
        _fabScrollHelper.onScroll(dx, dy);
    }

    @Override
    public void onListChange() { _fabScrollHelper.setVisible(true); }

    @Override
    public void onSaveGroupFilter(List<String> groupFilter) {
        getPreferences().setGroupFilter(groupFilter);
    }

    @Override
    public void onLocked(boolean userInitiated) {
        if (_actionMode != null) {
            _actionMode.finish();
        }
        if (_searchView != null && !_searchView.isIconified()) {
            collapseSearchView();
        }

        _entryListView.clearEntries();
        _loaded = false;


        if (userInitiated) {
            startAuthActivity(true);
        } else {
            super.onLocked(userInitiated);
        }
    }

    private void copyEntryCode(VaultEntry entry) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("text/plain", entry.getInfo().getOtp());
        clipboard.setPrimaryClip(clip);
    }

    private class ActionModeCallbacks implements ActionMode.Callback {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.menu_action_mode, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_copy:
                        copyEntryCode(_selectedEntries.get(0));
                        mode.finish();
                        return true;

                    case R.id.action_edit:
                        startEditEntryActivity(CODE_EDIT_ENTRY, _selectedEntries.get(0));
                        mode.finish();
                        return true;

                    case R.id.action_share_qr:
                        Intent intent = new Intent(getBaseContext(), TransferEntriesActivity.class);
                        ArrayList<GoogleAuthInfo> authInfos = new ArrayList<>();
                        for (VaultEntry entry : _selectedEntries) {
                            GoogleAuthInfo authInfo = new GoogleAuthInfo(entry.getInfo(), entry.getName(), entry.getIssuer());
                            authInfos.add(authInfo);
                        }

                        intent.putExtra("authInfos", authInfos);
                        startActivity(intent);

                        mode.finish();
                        return true;

                    case R.id.action_delete:
                        Dialogs.showDeleteEntriesDialog(MainActivity.this, _selectedEntries, (d, which) -> {
                            deleteEntries(_selectedEntries);

                            for (VaultEntry entry : _selectedEntries) {
                                if (entry.getGroup() != null) {
                                    if (!_vault.getGroups().contains(entry.getGroup())) {
                                        _entryListView.setGroups(_vault.getGroups());
                                        break;
                                    }
                                }
                            }

                            mode.finish();
                        });
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                _entryListView.setActionModeState(false, null);
                _selectedEntries.clear();
                _actionMode = null;
            }
    }
}
