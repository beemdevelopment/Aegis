package com.beemdevelopment.aegis.ui;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.widget.Toast;

import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.SearchView;

import com.beemdevelopment.aegis.AegisApplication;
import com.beemdevelopment.aegis.CancelAction;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.SortCategory;
import com.beemdevelopment.aegis.ViewMode;
import com.beemdevelopment.aegis.helpers.FabScrollHelper;
import com.beemdevelopment.aegis.helpers.PermissionHelper;
import com.beemdevelopment.aegis.otp.GoogleAuthInfo;
import com.beemdevelopment.aegis.otp.GoogleAuthInfoException;
import com.beemdevelopment.aegis.ui.views.EntryListView;
import com.beemdevelopment.aegis.vault.VaultEntry;
import com.beemdevelopment.aegis.vault.VaultFile;
import com.beemdevelopment.aegis.vault.VaultManager;
import com.beemdevelopment.aegis.vault.VaultManagerException;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
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
    private String _selectedGroup;
    private boolean _searchSubmitted;

    private List<VaultEntry> _selectedEntries;
    private ActionMode _actionMode;

    private Menu _menu;
    private SearchView _searchView;
    private FloatingActionsMenu _fabMenu;
    private EntryListView _entryListView;

    private FabScrollHelper _fabScrollHelper;

    private ActionMode.Callback _actionModeCallbacks = new ActionModeCallbacks();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        _app = (AegisApplication) getApplication();
        _vault = _app.getVaultManager();
        _loaded = false;

        // set up the main view
        setContentView(R.layout.activity_main);

        // set up the entry view
        _entryListView = (EntryListView) getSupportFragmentManager().findFragmentById(R.id.key_profiles);
        _entryListView.setListener(this);
        _entryListView.setCodeGroupSize(getPreferences().getCodeGroupSize());
        _entryListView.setShowAccountName(getPreferences().isAccountNameVisible());
        _entryListView.setSearchAccountName(getPreferences().isSearchAccountNameEnabled());
        _entryListView.setHighlightEntry(getPreferences().isEntryHighlightEnabled());
        _entryListView.setTapToReveal(getPreferences().isTapToRevealEnabled());
        _entryListView.setTapToRevealTime(getPreferences().getTapToRevealTime());
        _entryListView.setSortCategory(getPreferences().getCurrentSortCategory(), false);
        _entryListView.setViewMode(getPreferences().getCurrentViewMode());
        _entryListView.setIsCopyOnTapEnabled(getPreferences().isCopyOnTapEnabled());

        // set up the floating action button
        _fabMenu = findViewById(R.id.fab);
        findViewById(R.id.fab_enter).setOnClickListener(view -> {
            _fabMenu.collapse();
            startEditEntryActivity(CODE_ADD_ENTRY, null, true);
        });
        findViewById(R.id.fab_scan_image).setOnClickListener(view -> {
            _fabMenu.collapse();
            startScanImageActivity();
        });
        findViewById(R.id.fab_scan).setOnClickListener(view -> {
            _fabMenu.collapse();
            startScanActivity();
        });

        _fabScrollHelper = new FabScrollHelper(_fabMenu);
        _selectedEntries = new ArrayList<>();
    }

    @Override
    protected void onDestroy() {
        _entryListView.setListener(null);
        super.onDestroy();
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

        doShortcutActions();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // don't process any activity results if the vault is locked
        if (requestCode != CODE_DECRYPT && requestCode != CODE_DO_INTRO && _app.isVaultLocked()) {
            return;
        }

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
    }

    private void onPreferencesResult(Intent data) {
        // refresh the entire entry list if needed
        if (_loaded) {
            if (data.getBooleanExtra("needsRecreate", false)) {
                recreate();
            } else if (data.getBooleanExtra("needsRefresh", false)) {
                boolean showAccountName = getPreferences().isAccountNameVisible();
                int codeGroupSize = getPreferences().getCodeGroupSize();
                boolean searchAccountName = getPreferences().isSearchAccountNameEnabled();
                boolean highlightEntry = getPreferences().isEntryHighlightEnabled();
                boolean tapToReveal = getPreferences().isTapToRevealEnabled();
                int tapToRevealTime = getPreferences().getTapToRevealTime();
                ViewMode viewMode = getPreferences().getCurrentViewMode();
                boolean copyOnTap = getPreferences().isCopyOnTapEnabled();
                _entryListView.setShowAccountName(showAccountName);
                _entryListView.setCodeGroupSize(codeGroupSize);
                _entryListView.setSearchAccountName(searchAccountName);
                _entryListView.setHighlightEntry(highlightEntry);
                _entryListView.setTapToReveal(tapToReveal);
                _entryListView.setTapToRevealTime(tapToRevealTime);
                _entryListView.setViewMode(viewMode);
                _entryListView.setIsCopyOnTapEnabled(copyOnTap);
                _entryListView.refresh(true);
            }
        }
    }

    private void startEditEntryActivity(int requestCode, VaultEntry entry, boolean isNew) {
        Intent intent = new Intent(this, EditEntryActivity.class);
        if (isNew) {
            intent.putExtra("newEntry", entry != null ? entry : VaultEntry.getDefault());
        } else {
            intent.putExtra("entryUUID", entry.getUUID());
        }
        intent.putExtra("selectedGroup", _selectedGroup);
        startActivityForResult(intent, requestCode);
    }

    private void onScanResult(Intent data) {
        List<VaultEntry> entries = (ArrayList<VaultEntry>) data.getSerializableExtra("entries");
        if (entries.size() == 1) {
            startEditEntryActivity(CODE_ADD_ENTRY, entries.get(0), true);
        } else {
            for (VaultEntry entry : entries) {
                _vault.addEntry(entry);
                if (_loaded) {
                    _entryListView.addEntry(entry);
                }
            }

            saveVault();
        }
    }

    private void onAddEntryResult(Intent data) {
        if (_loaded) {
            UUID entryUUID = (UUID) data.getSerializableExtra("entryUUID");
            VaultEntry entry = _vault.getEntryByUUID(entryUUID);
            _entryListView.addEntry(entry);
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
        Uri inputFile = (intent.getData());
        Bitmap bitmap;

        try {
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();

            try (InputStream inputStream = getContentResolver().openInputStream(inputFile)) {
                bitmap = BitmapFactory.decodeStream(inputStream, null, bmOptions);
            }

            int[] intArray = new int[bitmap.getWidth() * bitmap.getHeight()];
            bitmap.getPixels(intArray, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

            LuminanceSource source = new RGBLuminanceSource(bitmap.getWidth(), bitmap.getHeight(), intArray);
            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));

            Reader reader = new MultiFormatReader();
            Result result = reader.decode(binaryBitmap);

            GoogleAuthInfo info = GoogleAuthInfo.parseUri(result.getText());
            VaultEntry entry = new VaultEntry(info);

            startEditEntryActivity(CODE_ADD_ENTRY, entry, true);
        } catch (NotFoundException | IOException | ChecksumException | FormatException | GoogleAuthInfoException e) {
            e.printStackTrace();
            Dialogs.showErrorDialog(this, R.string.unable_to_read_qrcode, e);
        }
    }

    private void updateGroupFilterMenu() {
        SubMenu menu = _menu.findItem(R.id.action_filter).getSubMenu();
        for (int i = menu.size() - 1; i >= 0; i--) {
            MenuItem item = menu.getItem(i);
            if (item.getItemId() == R.id.menu_filter_all) {
                continue;
            }
            menu.removeItem(item.getItemId());
        }

        // if the group no longer exists, switch back to 'All'
        TreeSet<String> groups = _vault.getGroups();
        if (_selectedGroup != null && !groups.contains(_selectedGroup)) {
            menu.findItem(R.id.menu_filter_all).setChecked(true);
            setGroupFilter(null);
        }

        for (String group : groups) {
            MenuItem item = menu.add(R.id.action_filter_group, Menu.NONE, Menu.NONE, group);
            if (group.equals(_selectedGroup)) {
                item.setChecked(true);
            }
        }

        if (groups.size() > 0) {
            menu.add(R.id.action_filter_group, Menu.NONE, 10, R.string.filter_ungrouped);
        }

        menu.setGroupCheckable(R.id.action_filter_group, true, true);
    }

    private void updateSortCategoryMenu() {
        SortCategory category = getPreferences().getCurrentSortCategory();
        _menu.findItem(category.getMenuItem()).setChecked(true);
    }

    private void setGroupFilter(String group) {
        getSupportActionBar().setSubtitle(group);
        _selectedGroup = group;
        _entryListView.setGroupFilter(group, true);
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

        Intent chooserIntent = Intent.createChooser(galleryIntent, getString(R.string.select_picture));
        startActivityForResult(Intent.createChooser(chooserIntent, getString(R.string.select_picture)), CODE_SCAN_IMAGE);
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
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri uri = intent.getData();
            getIntent().setData(null);
            getIntent().setAction(null);

            GoogleAuthInfo info = null;
            try {
                info = GoogleAuthInfo.parseUri(uri);
            } catch (GoogleAuthInfoException e) {
                e.printStackTrace();
                Dialogs.showErrorDialog(this, R.string.unable_to_read_qrcode, e);
            }

            if (info != null) {
                VaultEntry entry = new VaultEntry(info);
                startEditEntryActivity(CODE_ADD_ENTRY, entry, true);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (_vault == null) {
            // start the intro if the vault file doesn't exist
            if (!VaultManager.fileExists(this)) {
                if (getPreferences().isIntroDone()) {
                    Toast.makeText(this, getString(R.string.vault_not_found), Toast.LENGTH_SHORT).show();
                }
                Intent intro = new Intent(this, IntroActivity.class);
                startActivityForResult(intro, CODE_DO_INTRO);
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
            startAuthActivity();
        } else if (_loaded) {
            // update the list of groups in the filter menu
            if (_menu != null) {
                updateGroupFilterMenu();
            }

            // refresh all codes to prevent showing old ones
            _entryListView.refresh(false);
        } else {
            loadEntries();
            checkTimeSyncSetting();
        }

        handleDeeplink();
        updateLockIcon();
        doShortcutActions();
    }

    @Override
    public void onBackPressed() {
        if (!_searchView.isIconified() || _searchSubmitted) {
            _searchSubmitted = false;
            _entryListView.setSearchFilter(null);

            collapseSearchView();
            setTitle("Aegis");
            setGroupFilter(_selectedGroup);
            return;
        }

        if (_app.isAutoLockEnabled()) {
            _app.lock();
            return;
        }

        super.onBackPressed();
    }

    private void deleteEntries(List<VaultEntry> entries) {
        for (VaultEntry entry: entries) {
            VaultEntry oldEntry = _vault.removeEntry(entry);
            _entryListView.removeEntry(oldEntry);
        }

        saveVault();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        _menu = menu;
        getMenuInflater().inflate(R.menu.menu_main, menu);
        updateLockIcon();
        if (_loaded) {
            updateGroupFilterMenu();
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
                Intent intent = new Intent(this, PreferencesActivity.class);
                startActivityForResult(intent, CODE_PREFERENCES);
                return true;
            }
            case R.id.action_about: {
                Intent intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
                return true;
            }
            case R.id.action_lock:
                _app.lock();
                return true;
            default:
                if (item.getGroupId() == R.id.action_filter_group) {
                    item.setChecked(true);

                    String group = null;
                    if (item.getItemId() != R.id.menu_filter_all) {
                        group = item.getTitle().toString();
                    }
                    setGroupFilter(group);
                }

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
            _entryListView.addEntries(_vault.getEntries());
            _entryListView.runEntriesAnimation();
            _loaded = true;
        }
    }

    private void startAuthActivity() {
        Intent intent = new Intent(this, AuthActivity.class);
        intent.putExtra("cancelAction", CancelAction.KILL);
        startActivityForResult(intent, CODE_DECRYPT);
    }

    private void updateLockIcon() {
        // hide the lock icon if the vault is not unlocked
        if (_menu != null && !_app.isVaultLocked()) {
            MenuItem item = _menu.findItem(R.id.action_lock);
            item.setVisible(_vault.isEncryptionEnabled());
        }
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
        saveVault();
    }

    @Override
    public void onEntryChange(VaultEntry entry) {
        saveVault();
    }

    public void onEntryCopy(VaultEntry entry) {
        copyEntryCode(entry);
    }

    @Override
    public void onScroll(int dx, int dy) {
        _fabScrollHelper.onScroll(dx, dy);
    }

    @Override
    public void onLocked() {
        if (_actionMode != null) {
            _actionMode.finish();
        }

        _entryListView.clearEntries();
        _loaded = false;

        if (isOpen()) {
            startAuthActivity();
        }

        super.onLocked();
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
                        startEditEntryActivity(CODE_EDIT_ENTRY, _selectedEntries.get(0), false);
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
                        Dialogs.showDeleteEntriesDialog(MainActivity.this, (d, which) -> {
                            deleteEntries(_selectedEntries);

                            for (VaultEntry entry : _selectedEntries) {
                                if (entry.getGroup() != null) {
                                    if (!_vault.getGroups().contains(entry.getGroup())) {
                                        updateGroupFilterMenu();
                                    }
                                }
                            }

                            mode.finish();
                        }, _selectedEntries.size());
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
