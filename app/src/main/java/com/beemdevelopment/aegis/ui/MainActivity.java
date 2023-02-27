package com.beemdevelopment.aegis.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.SearchView;

import com.beemdevelopment.aegis.Preferences;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.SortCategory;
import com.beemdevelopment.aegis.ViewMode;
import com.beemdevelopment.aegis.helpers.FabScrollHelper;
import com.beemdevelopment.aegis.helpers.PermissionHelper;
import com.beemdevelopment.aegis.otp.GoogleAuthInfo;
import com.beemdevelopment.aegis.otp.GoogleAuthInfoException;
import com.beemdevelopment.aegis.otp.OtpInfoException;
import com.beemdevelopment.aegis.ui.dialogs.Dialogs;
import com.beemdevelopment.aegis.ui.fragments.preferences.BackupsPreferencesFragment;
import com.beemdevelopment.aegis.ui.fragments.preferences.PreferencesFragment;
import com.beemdevelopment.aegis.ui.tasks.QrDecodeTask;
import com.beemdevelopment.aegis.ui.views.EntryListView;
import com.beemdevelopment.aegis.util.TimeUtils;
import com.beemdevelopment.aegis.vault.VaultEntry;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

public class MainActivity extends AegisActivity implements EntryListView.Listener {
    // activity request codes
    private static final int CODE_SCAN = 0;
    private static final int CODE_ADD_ENTRY = 1;
    private static final int CODE_EDIT_ENTRY = 2;
    private static final int CODE_DO_INTRO = 3;
    private static final int CODE_DECRYPT = 4;
    private static final int CODE_PREFERENCES = 5;
    private static final int CODE_SCAN_IMAGE = 6;

    // Permission request codes
    private static final int CODE_PERM_CAMERA = 0;

    private boolean _loaded;
    private boolean _isRecreated;
    private boolean _isDPadPressed;
    private boolean _isDoingIntro;
    private boolean _isAuthenticating;

    private String _submittedSearchQuery;
    private String _pendingSearchQuery;

    private List<VaultEntry> _selectedEntries;

    private Menu _menu;
    private SearchView _searchView;
    private EntryListView _entryListView;
    private LinearLayout _btnErrorBar;
    private TextView _textErrorBar;

    private FabScrollHelper _fabScrollHelper;

    private ActionMode _actionMode;
    private ActionMode.Callback _actionModeCallbacks = new ActionModeCallbacks();

    private LockBackPressHandler _lockBackPressHandler;
    private SearchViewBackPressHandler _searchViewBackPressHandler;
    private ActionModeBackPressHandler _actionModeBackPressHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.toolbar));
        _loaded = false;
        _isDPadPressed = false;
        _isDoingIntro = false;
        _isAuthenticating = false;
        if (savedInstanceState != null) {
            _isRecreated = true;
            _pendingSearchQuery = savedInstanceState.getString("pendingSearchQuery");
            _submittedSearchQuery = savedInstanceState.getString("submittedSearchQuery");
            _isDoingIntro = savedInstanceState.getBoolean("isDoingIntro");
            _isAuthenticating = savedInstanceState.getBoolean("isAuthenticating");
        }

        _lockBackPressHandler = new LockBackPressHandler();
        getOnBackPressedDispatcher().addCallback(this, _lockBackPressHandler);
        _searchViewBackPressHandler = new SearchViewBackPressHandler();
        getOnBackPressedDispatcher().addCallback(this, _searchViewBackPressHandler);
        _actionModeBackPressHandler = new ActionModeBackPressHandler();
        getOnBackPressedDispatcher().addCallback(this, _actionModeBackPressHandler);

        _entryListView = (EntryListView) getSupportFragmentManager().findFragmentById(R.id.key_profiles);
        _entryListView.setListener(this);
        _entryListView.setCodeGroupSize(_prefs.getCodeGroupSize());
        _entryListView.setShowAccountName(_prefs.isAccountNameVisible());
        _entryListView.setShowIcon(_prefs.isIconVisible());
        _entryListView.setHighlightEntry(_prefs.isEntryHighlightEnabled());
        _entryListView.setPauseFocused(_prefs.isPauseFocusedEnabled());
        _entryListView.setTapToReveal(_prefs.isTapToRevealEnabled());
        _entryListView.setTapToRevealTime(_prefs.getTapToRevealTime());
        _entryListView.setSortCategory(_prefs.getCurrentSortCategory(), false);
        _entryListView.setViewMode(_prefs.getCurrentViewMode());
        _entryListView.setIsCopyOnTapEnabled(_prefs.isCopyOnTapEnabled());
        _entryListView.setPrefGroupFilter(_prefs.getGroupFilter());

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

        _btnErrorBar = findViewById(R.id.btn_error_bar);
        _textErrorBar = findViewById(R.id.text_error_bar);

        _fabScrollHelper = new FabScrollHelper(fab);
        _selectedEntries = new ArrayList<>();
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
            _prefs.setUsageCount(usageMap);
        }

        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle instance) {
        super.onSaveInstanceState(instance);
        instance.putString("pendingSearchQuery", _pendingSearchQuery);
        instance.putString("submittedSearchQuery", _submittedSearchQuery);
        instance.putBoolean("isDoingIntro", _isDoingIntro);
        instance.putBoolean("isAuthenticating", _isAuthenticating);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CODE_DECRYPT) {
            _isAuthenticating = false;
        }
        if (requestCode == CODE_DO_INTRO) {
            _isDoingIntro = false;
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
                onIntroResult();
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

        if (requestCode == CODE_PERM_CAMERA) {
            startScanActivity();
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        _isDPadPressed = isDPadKey(keyCode);
        return super.onKeyDown(keyCode, event);
    }

    private static boolean isDPadKey(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_DPAD_LEFT;
    }

    @Override
    public void onEntryListTouch() {
        _isDPadPressed = false;
    }

    private void onPreferencesResult(Intent data) {
        // refresh the entire entry list if needed
        if (_loaded) {
            if (data.getBooleanExtra("needsRecreate", false)) {
                recreate();
            } else if (data.getBooleanExtra("needsRefresh", false)) {
                boolean showAccountName = _prefs.isAccountNameVisible();
                boolean showIcons = _prefs.isIconVisible();
                Preferences.CodeGrouping codeGroupSize = _prefs.getCodeGroupSize();
                boolean highlightEntry = _prefs.isEntryHighlightEnabled();
                boolean pauseFocused = _prefs.isPauseFocusedEnabled();
                boolean tapToReveal = _prefs.isTapToRevealEnabled();
                int tapToRevealTime = _prefs.getTapToRevealTime();
                ViewMode viewMode = _prefs.getCurrentViewMode();
                boolean copyOnTap = _prefs.isCopyOnTapEnabled();
                _entryListView.setShowAccountName(showAccountName);
                _entryListView.setShowIcon(showIcons);
                _entryListView.setCodeGroupSize(codeGroupSize);
                _entryListView.setHighlightEntry(highlightEntry);
                _entryListView.setPauseFocused(pauseFocused);
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

    private void startIntroActivity() {
        if (!_isDoingIntro) {
            Intent intro = new Intent(this, IntroActivity.class);
            startActivityForResult(intro, CODE_DO_INTRO);
            _isDoingIntro = true;
        }
    }

    private void onScanResult(Intent data) {
        List<VaultEntry> entries = (ArrayList<VaultEntry>) data.getSerializableExtra("entries");
        if (entries != null) {
            importScannedEntries(entries);
        }
    }

    private void onAddEntryResult(Intent data) {
        if (_loaded) {
            UUID entryUUID = (UUID) data.getSerializableExtra("entryUUID");
            VaultEntry entry = _vaultManager.getVault().getEntryByUUID(entryUUID);
            _entryListView.addEntry(entry, true);
        }
    }

    private void onEditEntryResult(Intent data) {
        if (_loaded) {
            UUID entryUUID = (UUID) data.getSerializableExtra("entryUUID");

            if (data.getBooleanExtra("delete", false)) {
                _entryListView.removeEntry(entryUUID);
            } else {
                VaultEntry entry = _vaultManager.getVault().getEntryByUUID(entryUUID);
                _entryListView.replaceEntry(entryUUID, entry);
            }
        }
    }

    private void onScanImageResult(Intent intent) {
        if (intent.getData() != null) {
            startDecodeQrCodeImages(Collections.singletonList(intent.getData()));
            return;
        }

        if (intent.getClipData() != null) {
            ClipData data = intent.getClipData();

            List<Uri> uris = new ArrayList<>();
            for (int i = 0; i < data.getItemCount(); i++) {
                ClipData.Item item = data.getItemAt(i);
                if (item.getUri() != null) {
                    uris.add(item.getUri());
                }
            }

            if (uris.size() > 0) {
                startDecodeQrCodeImages(uris);
            }
        }
    }

    private static CharSequence buildImportError(String fileName, Throwable e) {
        SpannableStringBuilder builder = new SpannableStringBuilder(String.format("%s:\n%s", fileName, e));
        builder.setSpan(new StyleSpan(Typeface.BOLD), 0, fileName.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return builder;
    }

    private void startDecodeQrCodeImages(List<Uri> uris) {
        QrDecodeTask task = new QrDecodeTask(this, (results) -> {
            List<CharSequence> errors = new ArrayList<>();
            List<VaultEntry> entries = new ArrayList<>();
            List<GoogleAuthInfo.Export> googleAuthExports = new ArrayList<>();

            for (QrDecodeTask.Result res : results) {
                if (res.getException() != null) {
                    errors.add(buildImportError(res.getFileName(), res.getException()));
                    continue;
                }

                try {
                    Uri scanned = Uri.parse(res.getResult().getText());
                    if (Objects.equals(scanned.getScheme(), GoogleAuthInfo.SCHEME_EXPORT)) {
                        GoogleAuthInfo.Export export = GoogleAuthInfo.parseExportUri(scanned);
                        for (GoogleAuthInfo info: export.getEntries()) {
                            VaultEntry entry = new VaultEntry(info);
                            entries.add(entry);
                        }
                        googleAuthExports.add(export);
                    } else {
                        GoogleAuthInfo info = GoogleAuthInfo.parseUri(res.getResult().getText());
                        VaultEntry entry = new VaultEntry(info);
                        entries.add(entry);
                    }
                } catch (GoogleAuthInfoException e) {
                    errors.add(buildImportError(res.getFileName(), e));
                }
            }

            final DialogInterface.OnClickListener dialogDismissHandler = (dialog, which) -> importScannedEntries(entries);
            if (!googleAuthExports.isEmpty()) {
                boolean isSingleBatch = GoogleAuthInfo.Export.isSingleBatch(googleAuthExports);
                if (!isSingleBatch && errors.size() > 0) {
                    errors.add(getString(R.string.unrelated_google_auth_batches_error));
                    Dialogs.showMultiMessageDialog(this, R.string.import_error_title, getString(R.string.no_tokens_can_be_imported), errors, null);
                    return;
                } else if (!isSingleBatch) {
                    Dialogs.showErrorDialog(this, R.string.import_google_auth_failure, getString(R.string.unrelated_google_auth_batches_error));
                    return;
                } else {
                    List<Integer> missingIndices = GoogleAuthInfo.Export.getMissingIndices(googleAuthExports);
                    if (missingIndices.size() != 0) {
                        Dialogs.showPartialGoogleAuthImportWarningDialog(this, missingIndices, entries.size(), errors, dialogDismissHandler);
                        return;
                    }
                }
            }

            if ((errors.size() > 0 && results.size() > 1) || errors.size() > 1) {
                Dialogs.showMultiMessageDialog(this, R.string.import_error_title, getString(R.string.unable_to_read_qrcode_files, uris.size() - errors.size(), uris.size()), errors, dialogDismissHandler);
            } else if (errors.size() > 0) {
                Dialogs.showErrorDialog(this, getString(R.string.unable_to_read_qrcode_file, results.get(0).getFileName()), errors.get(0), dialogDismissHandler);
            } else {
                importScannedEntries(entries);
            }
        });
        task.execute(getLifecycle(), uris);
    }

    private void importScannedEntries(List<VaultEntry> entries) {
        if (entries.size() == 1) {
            startEditEntryActivityForNew(CODE_ADD_ENTRY, entries.get(0));
        } else if (entries.size() > 1) {
            for (VaultEntry entry: entries) {
                _vaultManager.getVault().addEntry(entry);
                _entryListView.addEntry(entry);
            }

            if (saveAndBackupVault()) {
                Toast.makeText(this, getResources().getQuantityString(R.plurals.added_new_entries, entries.size(), entries.size()), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void updateSortCategoryMenu() {
        SortCategory category = _prefs.getCurrentSortCategory();
        _menu.findItem(category.getMenuItem()).setChecked(true);
    }

    private void onIntroResult() {
        loadEntries();
        checkTimeSyncSetting();
    }

    private void checkTimeSyncSetting() {
        boolean autoTime = Settings.Global.getInt(getContentResolver(), Settings.Global.AUTO_TIME, 1) == 1;
        if (!autoTime && _prefs.isTimeSyncWarningEnabled()) {
            Dialogs.showTimeSyncWarningDialog(this, (dialog, which) -> {
                Intent intent = new Intent(Settings.ACTION_DATE_SETTINGS);
                startActivity(intent);
            });
        }
    }

    private void onDecryptResult() {
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
        galleryIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        galleryIntent.setDataAndType(android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI, "image/*");

        Intent fileIntent = new Intent(Intent.ACTION_GET_CONTENT);
        fileIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        fileIntent.setType("image/*");

        Intent chooserIntent = Intent.createChooser(galleryIntent, getString(R.string.select_picture));
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] { fileIntent });
        _vaultManager.startActivityForResult(this, chooserIntent, CODE_SCAN_IMAGE);
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
        if (action == null || !_vaultManager.isVaultLoaded()) {
            return;
        }

        switch (action) {
            case "scan":
                startScanActivity();
                break;
        }

        intent.removeExtra("action");
    }

    private void handleIncomingIntent() {
        if (!_vaultManager.isVaultLoaded()) {
            return;
        }

        Intent intent = getIntent();
        if (intent.getAction() == null) {
            return;
        }

        Uri uri;
        switch (intent.getAction()) {
            case Intent.ACTION_VIEW:
                uri = intent.getData();
                if (uri != null) {
                    intent.setData(null);
                    intent.setAction(null);

                    GoogleAuthInfo info;
                    try {
                        info = GoogleAuthInfo.parseUri(uri);
                    } catch (GoogleAuthInfoException e) {
                        e.printStackTrace();
                        Dialogs.showErrorDialog(this, R.string.unable_to_process_deeplink, e);
                        break;
                    }

                    VaultEntry entry = new VaultEntry(info);
                    startEditEntryActivityForNew(CODE_ADD_ENTRY, entry);
                }
                break;
            case Intent.ACTION_SEND:
                if (intent.hasExtra(Intent.EXTRA_STREAM)) {
                    uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                    intent.setAction(null);
                    intent.removeExtra(Intent.EXTRA_STREAM);

                    if (uri != null) {
                        startDecodeQrCodeImages(Collections.singletonList(uri));
                    }
                }
                if (intent.hasExtra(Intent.EXTRA_TEXT)) {
                    String stringExtra = intent.getStringExtra(Intent.EXTRA_TEXT);
                    intent.setAction(null);
                    intent.removeExtra(Intent.EXTRA_TEXT);

                    if (stringExtra != null) {
                        GoogleAuthInfo info;
                        try {
                            info = GoogleAuthInfo.parseUri(stringExtra);
                        } catch (GoogleAuthInfoException e) {
                            Dialogs.showErrorDialog(this, R.string.unable_to_process_shared_text, e);
                            break;
                        }

                        VaultEntry entry = new VaultEntry(info);
                        startEditEntryActivityForNew(CODE_ADD_ENTRY, entry);
                    }
                }
                break;
            case Intent.ACTION_SEND_MULTIPLE:
                if (intent.hasExtra(Intent.EXTRA_STREAM)) {
                    List<Uri> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                    intent.setAction(null);
                    intent.removeExtra(Intent.EXTRA_STREAM);

                    if (uris != null) {
                        uris = uris.stream()
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());

                        startDecodeQrCodeImages(uris);
                    }
                }
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (_vaultManager.isVaultInitNeeded()) {
            if (_prefs.isIntroDone()) {
                Toast.makeText(this, getString(R.string.vault_not_found), Toast.LENGTH_SHORT).show();
            }
            startIntroActivity();
            return;
        }

        if (!_vaultManager.isVaultLoaded() && !_vaultManager.isVaultFileLoaded()) {
            Dialogs.showErrorDialog(this, R.string.vault_load_error, _vaultManager.getVaultFileError(), (dialog1, which) -> finish());
            return;
        }

        if (!_vaultManager.isVaultLoaded()) {
            startAuthActivity(false);
        } else if (_loaded) {
            // update the list of groups in the entry list view so that the chip gets updated
            _entryListView.setGroups(_vaultManager.getVault().getGroups());

            // update the usage counts in case they are edited outside of the EntryListView
            _entryListView.setUsageCounts(_prefs.getUsageCounts());

            // refresh all codes to prevent showing old ones
            _entryListView.refresh(false);
        } else {
            loadEntries();
            checkTimeSyncSetting();
        }

        _lockBackPressHandler.setEnabled(
                _vaultManager.isAutoLockEnabled(Preferences.AUTO_LOCK_ON_BACK_BUTTON)
        );

        handleIncomingIntent();
        updateLockIcon();
        doShortcutActions();
        updateErrorBar();
    }

    private void deleteEntries(List<VaultEntry> entries) {
        for (VaultEntry entry: entries) {
            VaultEntry oldEntry = _vaultManager.getVault().removeEntry(entry);
            _entryListView.removeEntry(oldEntry);
        }

        saveAndBackupVault();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        _menu = menu;
        getMenuInflater().inflate(R.menu.menu_main, menu);

        updateLockIcon();
        if (_loaded) {
            _entryListView.setGroups(_vaultManager.getVault().getGroups());
            updateSortCategoryMenu();
        }

        MenuItem searchViewMenuItem = menu.findItem(R.id.mi_search);

        _searchView = (SearchView) searchViewMenuItem.getActionView();
        _searchView.setMaxWidth(Integer.MAX_VALUE);
        _searchView.setOnQueryTextFocusChangeListener((v, hasFocus) -> {
            boolean enabled = _submittedSearchQuery != null || hasFocus;
            _searchViewBackPressHandler.setEnabled(enabled);
        });
        _searchView.setOnCloseListener(() -> {
            boolean enabled = _submittedSearchQuery != null;
            _searchViewBackPressHandler.setEnabled(enabled);
            return false;
        });

        _searchView.setQueryHint(getString(R.string.search));
        _searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                setTitle(getString(R.string.search));
                getSupportActionBar().setSubtitle(s);
                _entryListView.setSearchFilter(s);
                _pendingSearchQuery = null;
                _submittedSearchQuery = s;
                collapseSearchView();
                _searchViewBackPressHandler.setEnabled(true);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                if (_submittedSearchQuery == null) {
                    _entryListView.setSearchFilter(s);
                }

                _pendingSearchQuery = Strings.isNullOrEmpty(s) && !_searchView.isIconified() ? null : s;
                if (_pendingSearchQuery != null) {
                    _entryListView.setSearchFilter(_pendingSearchQuery);
                }

                return false;
            }
        });
        _searchView.setOnSearchClickListener(v -> {
            String query = _submittedSearchQuery != null ? _submittedSearchQuery : _pendingSearchQuery;
            _searchView.setQuery(query, false);
        });

        if (_pendingSearchQuery != null) {
            _searchView.setIconified(false);
            _searchView.setQuery(_pendingSearchQuery, false);
            _searchViewBackPressHandler.setEnabled(true);
        } else if (_submittedSearchQuery != null) {
            setTitle(getString(R.string.search));
            getSupportActionBar().setSubtitle(_submittedSearchQuery);
            _entryListView.setSearchFilter(_submittedSearchQuery);
            _searchViewBackPressHandler.setEnabled(true);
        } else if (_prefs.getFocusSearchEnabled() && !_isRecreated) {
            _searchView.setIconified(false);
            _searchView.setFocusable(true);
            _searchView.requestFocus();
            _searchView.requestFocusFromTouch();
        }

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
                _vaultManager.lock(true);
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
                    _prefs.setCurrentSortCategory(sortCategory);
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
            _entryListView.setUsageCounts(_prefs.getUsageCounts());
            _entryListView.addEntries(_vaultManager.getVault().getEntries());
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
        if (_menu != null && _vaultManager.isVaultLoaded()) {
            MenuItem item = _menu.findItem(R.id.action_lock);
            item.setVisible(_vaultManager.getVault().isEncryptionEnabled());
        }
    }

    private void updateErrorBar() {
        Preferences.BackupResult backupRes = _prefs.getErroredBackupResult();
        if (backupRes != null) {
            _textErrorBar.setText(R.string.backup_error_bar_message);
            _btnErrorBar.setOnClickListener(view -> {
                Dialogs.showBackupErrorDialog(this, backupRes, (dialog, which) -> {
                    startPreferencesActivity(BackupsPreferencesFragment.class, "pref_backups");
                });
            });
            _btnErrorBar.setVisibility(View.VISIBLE);
        } else if (_prefs.isBackupsReminderNeeded() && _prefs.isBackupReminderEnabled()) {
            Date date = _prefs.getLatestBackupOrExportTime();
            if (date != null) {
                _textErrorBar.setText(getString(R.string.backup_reminder_bar_message_with_latest, TimeUtils.getElapsedSince(this, date)));
            } else {
                _textErrorBar.setText(R.string.backup_reminder_bar_message);
            }
            _btnErrorBar.setOnClickListener(view -> {
                Dialogs.showSecureDialog(new AlertDialog.Builder(this)
                        .setTitle(R.string.backup_reminder_bar_dialog_title)
                        .setMessage(R.string.backup_reminder_bar_dialog_summary)
                        .setPositiveButton(R.string.backup_reminder_bar_dialog_accept, (dialog, whichButton) -> {
                            startPreferencesActivity(BackupsPreferencesFragment.class, "pref_backups");
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .create());
            });
            _btnErrorBar.setVisibility(View.VISIBLE);
        } else if (_prefs.isPlaintextBackupWarningNeeded()) {
            _textErrorBar.setText(R.string.backup_plaintext_export_warning);
            _btnErrorBar.setOnClickListener(view -> showPlaintextExportWarningOptions());
            _btnErrorBar.setVisibility(View.VISIBLE);
        } else {
            _btnErrorBar.setVisibility(View.GONE);
        }
    }

    private void showPlaintextExportWarningOptions() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_plaintext_warning, null);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.backup_plaintext_export_warning)
                .setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        CheckBox checkBox = view.findViewById(R.id.checkbox_plaintext_warning);
        checkBox.setChecked(false);

        dialog.setOnShowListener(d -> {
            Button btnPos = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

            btnPos.setOnClickListener(l -> {
                dialog.dismiss();

                _prefs.setIsPlaintextBackupWarningDisabled(checkBox.isChecked());
                _prefs.setIsPlaintextBackupWarningNeeded(false);

                updateErrorBar();
            });
        });

        Dialogs.showSecureDialog(dialog);
    }

    @Override
    public void onEntryClick(VaultEntry entry) {
        if (_actionMode != null) {
            if (_selectedEntries.isEmpty()) {
                _actionMode.finish();
            } else {
                setFavoriteMenuItemVisiblity();
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

    private void setFavoriteMenuItemVisiblity() {
        MenuItem toggleFavoriteMenuItem = _actionMode.getMenu().findItem(R.id.action_toggle_favorite);

        if (_selectedEntries.size() == 1){
            if (_selectedEntries.get(0).isFavorite()) {
                toggleFavoriteMenuItem.setIcon(R.drawable.ic_set_favorite);
                toggleFavoriteMenuItem.setTitle(R.string.unfavorite);
            } else {
                toggleFavoriteMenuItem.setIcon(R.drawable.ic_unset_favorite);
                toggleFavoriteMenuItem.setTitle(R.string.favorite);
            }
        } else {
            toggleFavoriteMenuItem.setIcon(R.drawable.ic_unset_favorite);
            toggleFavoriteMenuItem.setTitle(String.format("%s / %s", getString(R.string.favorite), getString(R.string.unfavorite)));
        }
    }

    @Override
    public void onLongEntryClick(VaultEntry entry) {
        if (!_selectedEntries.isEmpty()) {
            return;
        }

        _selectedEntries.add(entry);
        _entryListView.setActionModeState(true, entry);
        startActionMode();
    }

    private void startActionMode() {
        _actionMode = startSupportActionMode(_actionModeCallbacks);
        _actionModeBackPressHandler.setEnabled(true);
        setFavoriteMenuItemVisiblity();
    }

    @Override
    public void onEntryMove(VaultEntry entry1, VaultEntry entry2) {
        _vaultManager.getVault().swapEntries(entry1, entry2);
    }

    @Override
    public void onEntryDrop(VaultEntry entry) {
        saveVault();
    }

    @Override
    public void onEntryChange(VaultEntry entry) {
        saveAndBackupVault();
    }

    public void onEntryCopy(VaultEntry entry) {
        copyEntryCode(entry);
    }

    @Override
    public void onScroll(int dx, int dy) {
        if (!_isDPadPressed) {
            _fabScrollHelper.onScroll(dx, dy);
        }
    }

    @Override
    public void onListChange() { _fabScrollHelper.setVisible(true); }

    @Override
    public void onSaveGroupFilter(List<String> groupFilter) {
        _prefs.setGroupFilter(groupFilter);
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
            super.onLocked(false);
        }
    }

    @SuppressLint("InlinedApi")
    private void copyEntryCode(VaultEntry entry) {
        String otp;
        try {
            otp = entry.getInfo().getOtp();
        } catch (OtpInfoException e) {
            return;
        }

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("text/plain", otp);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            PersistableBundle extras = new PersistableBundle();
            extras.putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true);
            clip.getDescription().setExtras(extras);
        }
        clipboard.setPrimaryClip(clip);
        if (_prefs.isMinimizeOnCopyEnabled()) {
            moveTaskToBack(true);
        }
    }

    private class SearchViewBackPressHandler extends OnBackPressedCallback {
        public SearchViewBackPressHandler() {
            super(false);
        }

        @Override
        public void handleOnBackPressed() {
            if (!_searchView.isIconified() || _submittedSearchQuery != null) {
                _submittedSearchQuery = null;
                _pendingSearchQuery = null;
                _entryListView.setSearchFilter(null);

                collapseSearchView();
                setTitle(R.string.app_name);
                getSupportActionBar().setSubtitle(null);
            }
        }
    }

    private class LockBackPressHandler extends OnBackPressedCallback {
        public LockBackPressHandler() {
            super(false);
        }

        @Override
        public void handleOnBackPressed() {
            if (_vaultManager.isAutoLockEnabled(Preferences.AUTO_LOCK_ON_BACK_BUTTON)) {
                _vaultManager.lock(false);
            }
        }
    }

    private class ActionModeBackPressHandler extends OnBackPressedCallback {
        public ActionModeBackPressHandler() {
            super(false);
        }

        @Override
        public void handleOnBackPressed() {
            if (_actionMode != null) {
                _actionMode.finish();
            }
        }
    }

    private class ActionModeCallbacks implements ActionMode.Callback {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                MenuInflater inflater = getMenuInflater();
                inflater.inflate(R.menu.menu_action_mode, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                if (_selectedEntries.size() == 0) {
                    mode.finish();
                    return true;
                }
                switch (item.getItemId()) {
                    case R.id.action_copy:
                        copyEntryCode(_selectedEntries.get(0));
                        mode.finish();
                        return true;

                    case R.id.action_edit:
                        startEditEntryActivity(CODE_EDIT_ENTRY, _selectedEntries.get(0));
                        mode.finish();
                        return true;

                    case R.id.action_toggle_favorite:
                        for (VaultEntry entry : _selectedEntries) {
                            entry.setIsFavorite(!entry.isFavorite());
                            _entryListView.replaceEntry(entry.getUUID(), entry);
                        }
                        _entryListView.refresh(true);

                        saveAndBackupVault();
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
                                    TreeSet<String> groups = _vaultManager.getVault().getGroups();
                                    if (!groups.contains(entry.getGroup())) {
                                        _entryListView.setGroups(groups);
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
                _actionModeBackPressHandler.setEnabled(false);
                _selectedEntries.clear();
                _actionMode = null;
            }
    }
}
