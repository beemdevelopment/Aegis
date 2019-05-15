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
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.beemdevelopment.aegis.AegisApplication;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.SortCategory;
import com.beemdevelopment.aegis.ViewMode;
import com.beemdevelopment.aegis.db.DatabaseEntry;
import com.beemdevelopment.aegis.db.DatabaseFileCredentials;
import com.beemdevelopment.aegis.db.DatabaseManager;
import com.beemdevelopment.aegis.db.DatabaseManagerException;
import com.beemdevelopment.aegis.helpers.FabScrollHelper;
import com.beemdevelopment.aegis.helpers.PermissionHelper;
import com.beemdevelopment.aegis.otp.GoogleAuthInfo;
import com.beemdevelopment.aegis.otp.GoogleAuthInfoException;
import com.beemdevelopment.aegis.ui.views.EntryListView;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.google.android.material.bottomsheet.BottomSheetDialog;
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

public class MainActivity extends AegisActivity implements EntryListView.Listener {
    // activity request codes
    private static final int CODE_SCAN = 0;
    private static final int CODE_ADD_ENTRY = 1;
    private static final int CODE_EDIT_ENTRY = 2;
    private static final int CODE_ENTER_ENTRY = 3;
    private static final int CODE_DO_INTRO = 4;
    private static final int CODE_DECRYPT = 5;
    private static final int CODE_PREFERENCES = 6;
    private static final int CODE_SCAN_IMAGE = 7;

    // permission request codes
    private static final int CODE_PERM_CAMERA = 0;

    private AegisApplication _app;
    private DatabaseManager _db;
    private boolean _loaded;
    private String _checkedGroup;

    private Menu _menu;
    private FloatingActionsMenu _fabMenu;
    private EntryListView _entryListView;

    private FabScrollHelper _fabScrollHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _app = (AegisApplication) getApplication();
        _db = _app.getDatabaseManager();
        _loaded = false;

        // set up the main view
        setContentView(R.layout.activity_main);

        // set up the entry view
        _entryListView = (EntryListView) getSupportFragmentManager().findFragmentById(R.id.key_profiles);
        _entryListView.setListener(this);
        _entryListView.setShowAccountName(getPreferences().isAccountNameVisible());
        _entryListView.setTapToReveal(getPreferences().isTapToRevealEnabled());
        _entryListView.setTapToRevealTime(getPreferences().getTapToRevealTime());
        _entryListView.setSortCategory(getPreferences().getCurrentSortCategory(), false);
        _entryListView.setViewMode(getPreferences().getCurrentViewMode());

        // set up the floating action button
        _fabMenu = findViewById(R.id.fab);
        findViewById(R.id.fab_enter).setOnClickListener(view -> {
            _fabMenu.collapse();
            startEditProfileActivity(CODE_ENTER_ENTRY, null, true);
        });
        findViewById(R.id.fab_scan_image).setOnClickListener(view -> {
            _fabMenu.collapse();

            Intent galleryIntent = new Intent(Intent.ACTION_PICK);
            galleryIntent.setDataAndType(android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI, "image/*");

            Intent chooserIntent = Intent.createChooser(galleryIntent, getString(R.string.select_picture));
            startActivityForResult(Intent.createChooser(chooserIntent, getString(R.string.select_picture)), CODE_SCAN_IMAGE);
        });
        findViewById(R.id.fab_scan).setOnClickListener(view -> {
            _fabMenu.collapse();
            startScanActivity();
        });

        _fabScrollHelper = new FabScrollHelper(_fabMenu);
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
        if (data == null) {
            return;
        }

        switch (requestCode) {
            case CODE_SCAN:
                onScanResult(resultCode, data);
                break;
            case CODE_ADD_ENTRY:
                onAddEntryResult(resultCode, data);
                break;
            case CODE_EDIT_ENTRY:
                onEditEntryResult(resultCode, data);
                break;
            case CODE_ENTER_ENTRY:
                onEnterEntryResult(resultCode, data);
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
            case CODE_SCAN_IMAGE:
                onScanImageResult(resultCode, data);
        }
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
        }
    }

    private void onPreferencesResult(int resultCode, Intent data) {
        // refresh the entire entry list if needed
        if (data.getBooleanExtra("needsRecreate", false)) {
            recreate();
        } else if (data.getBooleanExtra("needsRefresh", false)) {
            boolean showAccountName = getPreferences().isAccountNameVisible();
            boolean tapToReveal = getPreferences().isTapToRevealEnabled();
            int tapToRevealTime = getPreferences().getTapToRevealTime();
            ViewMode viewMode = getPreferences().getCurrentViewMode();
            _entryListView.setShowAccountName(showAccountName);
            _entryListView.setTapToReveal(tapToReveal);
            _entryListView.setTapToRevealTime(tapToRevealTime);
            _entryListView.setViewMode(viewMode);
            _entryListView.refresh(true);
        }
    }

    private void startEditProfileActivity(int requestCode, DatabaseEntry entry, boolean isNew) {
        Intent intent = new Intent(this, EditEntryActivity.class);
        if (entry != null) {
            intent.putExtra("entry", entry);
        }
        intent.putExtra("isNew", isNew);
        intent.putExtra("groups", new ArrayList<>(_db.getGroups()));
        startActivityForResult(intent, requestCode);
    }

    private void onScanResult(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            DatabaseEntry entry = (DatabaseEntry) data.getSerializableExtra("entry");
            startEditProfileActivity(CODE_ADD_ENTRY, entry, true);
        }
    }

    private void onAddEntryResult(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            DatabaseEntry entry = (DatabaseEntry) data.getSerializableExtra("entry");
            addEntry(entry);
            saveDatabase();
        }
    }

    private void onEditEntryResult(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            DatabaseEntry entry = (DatabaseEntry) data.getSerializableExtra("entry");
            if (data.getBooleanExtra("delete", false)) {
                deleteEntry(entry);
            } else {
                // this profile has been serialized/deserialized and is no longer the same instance it once was
                // to deal with this, the replaceEntry functions are used
                _db.replaceEntry(entry);
                _entryListView.replaceEntry(entry);
                saveDatabase();
            }
        }
    }

    private void onEnterEntryResult(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            DatabaseEntry entry = (DatabaseEntry) data.getSerializableExtra("entry");
            addEntry(entry);
            saveDatabase();
        }
    }

    private void onScanImageResult(int resultCode, Intent intent) {
        if (resultCode == RESULT_OK) {
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
                DatabaseEntry entry = new DatabaseEntry(info);

                startEditProfileActivity(CODE_ADD_ENTRY, entry, true);
            } catch (NotFoundException | IOException | ChecksumException | FormatException | GoogleAuthInfoException e) {
                Toast.makeText(this, getString(R.string.unable_to_read_qrcode), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
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
        TreeSet<String> groups = _db.getGroups();
        if (_checkedGroup != null && !groups.contains(_checkedGroup)) {
            menu.findItem(R.id.menu_filter_all).setChecked(true);
            setGroupFilter(null);
        }

        for (String group : groups) {
            MenuItem item = menu.add(R.id.action_filter_group, Menu.NONE, Menu.NONE, group);
            if (group.equals(_checkedGroup)) {
                item.setChecked(true);
            }
        }

        menu.setGroupCheckable(R.id.action_filter_group, true, true);
    }

    private void updateSortCategoryMenu() {
        SortCategory category = getPreferences().getCurrentSortCategory();
        _menu.findItem(category.getMenuItem()).setChecked(true);
    }

    private void setGroupFilter(String group) {
        getSupportActionBar().setSubtitle(group);
        _checkedGroup = group;
        _entryListView.setGroupFilter(group, true);
    }

    private void addEntry(DatabaseEntry entry) {
        _db.addEntry(entry);
        _entryListView.addEntry(entry);
    }

    private void onDoIntroResult(int resultCode, Intent data) {
        if (resultCode == IntroActivity.RESULT_EXCEPTION) {
            // TODO: user feedback
            Exception e = (Exception) data.getSerializableExtra("exception");
            throw new RuntimeException(e);
        }

        DatabaseFileCredentials creds = (DatabaseFileCredentials) data.getSerializableExtra("creds");
        unlockDatabase(creds);
    }

    private void onDecryptResult(int resultCode, Intent intent) {
        DatabaseFileCredentials creds = (DatabaseFileCredentials) intent.getSerializableExtra("creds");
        unlockDatabase(creds);

        doShortcutActions();
    }

    private void startScanActivity() {
        if (!PermissionHelper.request(this, CODE_PERM_CAMERA, Manifest.permission.CAMERA)) {
            return;
        }

        Intent scannerActivity = new Intent(getApplicationContext(), ScannerActivity.class);
        startActivityForResult(scannerActivity, CODE_SCAN);
    }

    private void doShortcutActions() {
        Intent intent = getIntent();
        String action = intent.getStringExtra("action");
        if (action == null || _db.isLocked()) {
            return;
        }

        switch (action) {
            case "scan":
                startScanActivity();
                break;
        }

        intent.removeExtra("action");
    }

    public void startActivityForResult(Intent intent, int requestCode) {
        super.startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (_db.isLocked()) {
            // start the intro if the database file doesn't exist
            if (!_db.isLoaded() && !_db.fileExists()) {
                // the db doesn't exist, start the intro
                if (getPreferences().isIntroDone()) {
                    Toast.makeText(this, getString(R.string.vault_not_found), Toast.LENGTH_SHORT).show();
                }
                Intent intro = new Intent(this, IntroActivity.class);
                startActivityForResult(intro, CODE_DO_INTRO);
                return;
            } else {
                unlockDatabase(null);
            }
        } else if (_loaded) {
            // update the list of groups in the filter menu
            if (_menu != null) {
                updateGroupFilterMenu();
            }

            // refresh all codes to prevent showing old ones
            _entryListView.refresh(false);
        } else {
            loadEntries();
        }

        updateLockIcon();
        doShortcutActions();
    }

    @Override
    public void onBackPressed() {
        if (_app.isAutoLockEnabled()) {
            lockDatabase();
        }

        super.onBackPressed();
    }

    private BottomSheetDialog createBottomSheet(final DatabaseEntry entry) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(R.layout.bottom_sheet_edit_entry);
        dialog.setCancelable(true);
        dialog.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        dialog.show();

        dialog.findViewById(R.id.copy_button).setOnClickListener(view -> {
            dialog.dismiss();
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("text/plain", entry.getInfo().getOtp());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, getString(R.string.code_copied), Toast.LENGTH_SHORT).show();
        });

        dialog.findViewById(R.id.delete_button).setOnClickListener(view -> {
            dialog.dismiss();
            Dialogs.showDeleteEntryDialog(this, (d, which) -> {
                deleteEntry(entry);
                // update the filter list if the group no longer exists
                if (entry.getGroup() != null) {
                    if (!_db.getGroups().contains(entry.getGroup())) {
                        updateGroupFilterMenu();
                    }
                }
            });
        });

        dialog.findViewById(R.id.edit_button).setOnClickListener(view -> {
            dialog.dismiss();
            startEditProfileActivity(CODE_EDIT_ENTRY, entry, false);
        });

        return dialog;
    }

    private void deleteEntry(DatabaseEntry entry) {
        _db.removeEntry(entry);
        saveDatabase();

        _entryListView.removeEntry(entry);
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
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent(this, PreferencesActivity.class);
                startActivityForResult(intent, CODE_PREFERENCES);
                return true;
            case R.id.action_lock:
                lockDatabase();
                startAuthActivity();
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

    private void lockDatabase() {
        if (_loaded) {
            _entryListView.clearEntries();
            _db.lock();
            _loaded = false;
        }
    }

    private void unlockDatabase(DatabaseFileCredentials creds) {
        if (_loaded) {
            return;
        }

        try {
            if (!_db.isLoaded()) {
                _db.load();
            }
            if (_db.isLocked()) {
                if (creds == null) {
                    startAuthActivity();
                    return;
                } else {
                    _db.unlock(creds);
                }
            }
        } catch (DatabaseManagerException e) {
            Toast.makeText(this, getString(R.string.decryption_error), Toast.LENGTH_LONG).show();
            startAuthActivity();
            return;
        }

        loadEntries();
    }

    private void loadEntries() {
        // load all entries
        List<DatabaseEntry> entries = new ArrayList<DatabaseEntry>(_db.getEntries());
        _entryListView.addEntries(entries);
        _entryListView.runEntriesAnimation();
        _loaded = true;
    }

    private void startAuthActivity() {
        Intent intent = new Intent(this, AuthActivity.class);
        intent.putExtra("slots", _db.getFileHeader().getSlots());
        startActivityForResult(intent, CODE_DECRYPT);
    }

    private void saveDatabase() {
        try {
            _db.save();
        } catch (DatabaseManagerException e) {
            Toast.makeText(this, getString(R.string.saving_error), Toast.LENGTH_LONG).show();
        }
    }

    private void updateLockIcon() {
        // hide the lock icon if the database is not unlocked
        if (_menu != null && !_db.isLocked()) {
            MenuItem item = _menu.findItem(R.id.action_lock);
            item.setVisible(_db.isEncryptionEnabled());
        }
    }

    @Override
    public void onEntryClick(DatabaseEntry entry) {
        createBottomSheet(entry).show();
    }

    @Override
    public void onEntryMove(DatabaseEntry entry1, DatabaseEntry entry2) {
        _db.swapEntries(entry1, entry2);
    }

    @Override
    public void onEntryDrop(DatabaseEntry entry) {
        saveDatabase();
    }

    @Override
    public void onEntryChange(DatabaseEntry entry) {
        saveDatabase();
    }

    @Override
    public void onScroll(int dx, int dy) {
        _fabScrollHelper.onScroll(dx, dy);
    }
}
