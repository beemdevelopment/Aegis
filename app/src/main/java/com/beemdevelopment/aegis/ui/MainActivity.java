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
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.beemdevelopment.aegis.GroupPlaceholderType;
import com.beemdevelopment.aegis.Preferences;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.SortCategory;
import com.beemdevelopment.aegis.helpers.BitmapHelper;
import com.beemdevelopment.aegis.helpers.DropdownHelper;
import com.beemdevelopment.aegis.helpers.FabScrollHelper;
import com.beemdevelopment.aegis.helpers.PermissionHelper;
import com.beemdevelopment.aegis.helpers.ViewHelper;
import com.beemdevelopment.aegis.icons.IconType;
import com.beemdevelopment.aegis.otp.GoogleAuthInfo;
import com.beemdevelopment.aegis.otp.GoogleAuthInfoException;
import com.beemdevelopment.aegis.otp.OtpInfoException;
import com.beemdevelopment.aegis.ui.dialogs.Dialogs;
import com.beemdevelopment.aegis.ui.fragments.preferences.BackupsPreferencesFragment;
import com.beemdevelopment.aegis.ui.fragments.preferences.PreferencesFragment;
import com.beemdevelopment.aegis.ui.models.ErrorCardInfo;
import com.beemdevelopment.aegis.ui.models.VaultGroupModel;
import com.beemdevelopment.aegis.ui.tasks.IconOptimizationTask;
import com.beemdevelopment.aegis.ui.tasks.QrDecodeTask;
import com.beemdevelopment.aegis.ui.views.EntryListView;
import com.beemdevelopment.aegis.util.TimeUtils;
import com.beemdevelopment.aegis.util.UUIDMap;
import com.beemdevelopment.aegis.vault.VaultEntry;
import com.beemdevelopment.aegis.vault.VaultEntryIcon;
import com.beemdevelopment.aegis.vault.VaultFile;
import com.beemdevelopment.aegis.vault.VaultGroup;
import com.beemdevelopment.aegis.vault.VaultRepository;
import com.beemdevelopment.aegis.vault.VaultRepositoryException;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class MainActivity extends AegisActivity implements EntryListView.Listener {
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

    private Collection<VaultGroup> _groups;
    private ChipGroup _groupChip;
    private Set<UUID> _groupFilter;
    private Set<UUID> _prefGroupFilter;

    private FabScrollHelper _fabScrollHelper;

    private ActionMode _actionMode;
    private ActionMode.Callback _actionModeCallbacks = new ActionModeCallbacks();

    private LockBackPressHandler _lockBackPressHandler;
    private SearchViewBackPressHandler _searchViewBackPressHandler;
    private ActionModeBackPressHandler _actionModeBackPressHandler;

    private final ActivityResultLauncher<Intent> authResultLauncher =
            registerForActivityResult(new StartActivityForResult(), activityResult -> {
                _isAuthenticating = false;
                if (activityResult.getResultCode() == RESULT_OK) {
                    onDecryptResult();
                }
            });

    private final ActivityResultLauncher<Intent> introResultLauncher =
            registerForActivityResult(new StartActivityForResult(), activityResult -> {
                _isDoingIntro = false;
                if (activityResult.getResultCode() == RESULT_OK) {
                    onIntroResult();
                }
            });

    private final ActivityResultLauncher<Intent> scanResultLauncher =
            registerForActivityResult(new StartActivityForResult(), activityResult -> {
                if (activityResult.getResultCode() != RESULT_OK || activityResult.getData() == null) {
                    return;
                }
                onScanResult(activityResult.getData());
            });

    private final ActivityResultLauncher<Intent> assignIconsResultLauncher =
            registerForActivityResult(new StartActivityForResult(), activityResult -> {
                if (activityResult.getResultCode() != RESULT_OK || activityResult.getData() == null) {
                    return;
                }
                onAssignIconsResult();
            });

    private final ActivityResultLauncher<Intent> preferenceResultLauncher =
            registerForActivityResult(new StartActivityForResult(), activityResult -> onPreferencesResult());

    private final ActivityResultLauncher<Intent> editEntryResultLauncher =
            registerForActivityResult(new StartActivityForResult(), activityResult -> {
                if (activityResult.getResultCode() != RESULT_OK || activityResult.getData() == null) {
                    return;
                }
                onEditEntryResult();
            });

    private final ActivityResultLauncher<Intent> addEntryResultLauncher =
            registerForActivityResult(new StartActivityForResult(), activityResult -> {
                if (activityResult.getResultCode() != RESULT_OK || activityResult.getData() == null) {
                    return;
                }
                onAddEntryResult(activityResult.getData());
            });

    private final ActivityResultLauncher<Intent> codeScanResultLauncher =
            registerForActivityResult(new StartActivityForResult(), activityResult -> {
                if (activityResult.getResultCode() == RESULT_OK && activityResult.getData() != null) {
                    onScanImageResult(activityResult.getData());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.toolbar));
        ViewHelper.setupAppBarInsets(findViewById(R.id.app_bar_layout));
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
        _entryListView.setAccountNamePosition(_prefs.getAccountNamePosition());
        _entryListView.setShowIcon(_prefs.isIconVisible());
        _entryListView.setShowExpirationState(_prefs.getShowExpirationState());
        _entryListView.setShowNextCode(_prefs.getShowNextCode());
        _entryListView.setOnlyShowNecessaryAccountNames(_prefs.onlyShowNecessaryAccountNames());
        _entryListView.setHighlightEntry(_prefs.isEntryHighlightEnabled());
        _entryListView.setPauseFocused(_prefs.isPauseFocusedEnabled());
        _entryListView.setTapToReveal(_prefs.isTapToRevealEnabled());
        _entryListView.setTapToRevealTime(_prefs.getTapToRevealTime());
        _entryListView.setSortCategory(_prefs.getCurrentSortCategory(), false);
        _entryListView.setViewMode(_prefs.getCurrentViewMode());
        _entryListView.setCopyBehavior(_prefs.getCopyBehavior());
        _entryListView.setSearchBehaviorMask(_prefs.getSearchBehaviorMask());
        _prefGroupFilter = _prefs.getGroupFilter();

         FloatingActionButton fab = findViewById(R.id.fab);
         fab.setOnClickListener(v -> {
             View view = getLayoutInflater().inflate(R.layout.dialog_add_entry, null);
             BottomSheetDialog dialog = new BottomSheetDialog(this);
             dialog.setContentView(view);

             view.findViewById(R.id.fab_enter).setOnClickListener(v1 -> {
                 dialog.dismiss();
                 startEditEntryActivityForManual();
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

        _groupChip = findViewById(R.id.groupChipGroup);
        _fabScrollHelper = new FabScrollHelper(fab);
        _selectedEntries = new ArrayList<>();
    }

    public void setGroups(Collection<VaultGroup> groups) {
        _groups = groups;
        _groupChip.setVisibility(_groups.isEmpty() ? View.GONE : View.VISIBLE);

        if (_prefGroupFilter != null) {
            Set<UUID> groupFilter = cleanGroupFilter(_prefGroupFilter);
            _prefGroupFilter = null;
            if (!groupFilter.isEmpty()) {
                _groupFilter = groupFilter;
                _entryListView.setGroupFilter(groupFilter);
            }
        } else if (_groupFilter != null) {
            Set<UUID> groupFilter = cleanGroupFilter(_groupFilter);
            if (!_groupFilter.equals(groupFilter)) {
                _groupFilter = groupFilter;
                _entryListView.setGroupFilter(groupFilter);
            }
        }

        _entryListView.setGroups(groups);
        initializeGroups();
    }

    private void initializeGroups() {
        _groupChip.removeAllViews();

        for (VaultGroup group : _groups) {
            addChipTo(_groupChip, new VaultGroupModel(group));
        }

        GroupPlaceholderType placeholderType = GroupPlaceholderType.NO_GROUP;
        addChipTo(_groupChip, new VaultGroupModel(this, placeholderType));
        addSaveChip(_groupChip);
    }

    private Set<UUID> cleanGroupFilter(Set<UUID> groupFilter) {
        Set<UUID> groupUuids = _groups.stream().map(UUIDMap.Value::getUUID).collect(Collectors.toSet());

        return groupFilter.stream()
                .filter(g -> g == null || groupUuids.contains(g))
                .collect(Collectors.toSet());
    }

    private void addChipTo(ChipGroup chipGroup, VaultGroupModel group) {
        Chip chip = (Chip) getLayoutInflater().inflate(R.layout.chip_group_filter, null, false);
        chip.setText(group.getName());
        chip.setCheckable(true);
        chip.setCheckedIconVisible(false);
        chip.setChecked(_groupFilter != null && _groupFilter.contains(group.getUUID()));

        if (group.isPlaceholder()) {
            GroupPlaceholderType groupPlaceholderType = group.getPlaceholderType();
            chip.setTag(groupPlaceholderType);

            if (groupPlaceholderType == GroupPlaceholderType.ALL) {
                chip.setChecked(_groupFilter == null);
            } else if (groupPlaceholderType == GroupPlaceholderType.NO_GROUP) {
                chip.setChecked(_groupFilter != null && _groupFilter.contains(null));
            }
        } else {
            chip.setTag(group);
        }

        chip.setOnCheckedChangeListener((group1, isChecked) -> {
            Set<UUID> groupFilter = new HashSet<>();
            if (_actionMode != null) {
                _actionMode.finish();
            }

            setSaveChipVisibility(true);

            if (!isChecked) {
                group1.setChecked(false);
                _groupFilter = groupFilter;
                _entryListView.setGroupFilter(groupFilter);
                return;
            }

            Object chipTag = group1.getTag();
            if (chipTag == GroupPlaceholderType.NO_GROUP) {
                groupFilter.add(null);
            } else {
                groupFilter = getGroupFilter(chipGroup);
            }

            _groupFilter = groupFilter;
            _entryListView.setGroupFilter(groupFilter);
        });

        chipGroup.addView(chip);
    }

    private void addSaveChip(ChipGroup chipGroup) {
        Chip chip = (Chip) getLayoutInflater().inflate(R.layout.chip_group_filter, null, false);

        chip.setText(getString(R.string.save));
        chip.setVisibility(View.GONE);
        chip.setChipStrokeWidth(0);
        chip.setCheckable(false);
        chip.setChipBackgroundColorResource(android.R.color.transparent);
        chip.setTextColor(MaterialColors.getColor(chip.getRootView(), com.google.android.material.R.attr.colorSecondary));
        chip.setClickable(true);
        chip.setCheckedIconVisible(false);
        chip.setOnClickListener(v -> {
            onSaveGroupFilter(_groupFilter);
            setSaveChipVisibility(false);
        });

        chipGroup.addView(chip);
    }

    private void setSaveChipVisibility(boolean visible) {
        Chip saveChip = (Chip) _groupChip.getChildAt(_groupChip.getChildCount() - 1);
        saveChip.setChecked(false);
        saveChip.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private static Set<UUID> getGroupFilter(ChipGroup chipGroup) {
        return chipGroup.getCheckedChipIds().stream()
                .map(i -> {
                    Chip chip = chipGroup.findViewById(i);
                    if (chip.getTag() instanceof VaultGroupModel) {
                        VaultGroupModel group = (VaultGroupModel) chip.getTag();
                        return group.getUUID();
                    }

                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
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

        Map<UUID, Long> lastUsedMap = _entryListView.getLastUsedTimestamps();
        if (lastUsedMap != null) {
            _prefs.setLastUsedTimestamps(lastUsedMap);
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

        if (_groupFilter != null) {
            instance.putSerializable("prefGroupFilter", new HashSet<>(_groupFilter));
        }
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

        if (_searchView != null && !_searchView.isIconified()) {
            if (ViewCompat.getRootWindowInsets(findViewById(android.R.id.content).getRootView()).isVisible(WindowInsetsCompat.Type.ime())) {
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (inputMethodManager != null && getCurrentFocus() != null) {
                    inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                }
            }
        }
    }

    private void onPreferencesResult() {
        // refresh the entire entry list if needed
        if (_loaded) {
            recreate();
        }
    }

    private void startEditEntryActivityForNew(VaultEntry entry) {
        Intent intent = new Intent(this, EditEntryActivity.class);
        intent.putExtra("newEntry", entry);
        intent.putExtra("isManual", false);
        addEntryResultLauncher.launch(intent);
    }

    private void startEditEntryActivityForManual() {
        Intent intent = new Intent(this, EditEntryActivity.class);
        intent.putExtra("newEntry", VaultEntry.getDefault());
        intent.putExtra("isManual", true);
        addEntryResultLauncher.launch(intent);
    }

    private void startEditEntryActivity(VaultEntry entry) {
        Intent intent = new Intent(this, EditEntryActivity.class);
        intent.putExtra("entryUUID", entry.getUUID());
        editEntryResultLauncher.launch(intent);
    }

    private void startAssignIconsActivity(List<VaultEntry> entries) {
        ArrayList<UUID> assignIconEntriesIds = new ArrayList<>();
        Intent assignIconIntent = new Intent(getBaseContext(), AssignIconsActivity.class);
        for (VaultEntry entry : entries) {
            assignIconEntriesIds.add(entry.getUUID());
        }

        assignIconIntent.putExtra("entries", assignIconEntriesIds);
        assignIconsResultLauncher.launch(assignIconIntent);
    }

    private void startAssignGroupsDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_select_group, null);
        TextInputLayout groupSelectionLayout = view.findViewById(R.id.group_selection_layout);
        AutoCompleteTextView groupsSelection = view.findViewById(R.id.group_selection_dropdown);
        TextInputLayout newGroupLayout = view.findViewById(R.id.text_group_name_layout);
        TextInputEditText newGroupText = view.findViewById(R.id.text_group_name);

        Collection<VaultGroup> groups = _vaultManager.getVault().getUsedGroups();
        List<VaultGroupModel> groupModels = new ArrayList<>();
        groupModels.add(new VaultGroupModel(this, GroupPlaceholderType.NEW_GROUP));
        groupModels.addAll(groups.stream().map(VaultGroupModel::new).collect(Collectors.toList()));
        DropdownHelper.fillDropdown(this, groupsSelection, groupModels);

        AtomicReference<VaultGroupModel> groupModelRef = new AtomicReference<>();
        groupsSelection.setOnItemClickListener((parent, view1, position, id) -> {
            VaultGroupModel groupModel = (VaultGroupModel) parent.getItemAtPosition(position);
            groupModelRef.set(groupModel);

            if (groupModel.isPlaceholder()) {
                newGroupLayout.setVisibility(View.VISIBLE);
                newGroupText.requestFocus();
            } else {
                newGroupLayout.setVisibility(View.GONE);
            }

            groupSelectionLayout.setError(null);
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.assign_groups)
                .setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        dialog.setOnShowListener(d -> {
            Button btnPos = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            btnPos.setOnClickListener(v -> {
                VaultGroupModel groupModel = groupModelRef.get();
                if (groupModel == null) {
                    groupSelectionLayout.setError(getString(R.string.error_required_field));
                    return;
                }

                if (groupModel.isPlaceholder()) {
                    String newGroupName = newGroupText.getText().toString().trim();
                    if (newGroupName.isEmpty()) {
                        newGroupLayout.setError(getString(R.string.error_required_field));
                        return;
                    }

                    VaultGroup group = new VaultGroup(newGroupName);
                    _vaultManager.getVault().addGroup(group);
                    groupModel = new VaultGroupModel(group);
                }

                for (VaultEntry selectedEntry : _selectedEntries) {
                    selectedEntry.addGroup(groupModel.getUUID());
                }

                dialog.dismiss();
                saveAndBackupVault();
                _actionMode.finish();
                setGroups(_vaultManager.getVault().getUsedGroups());
            });
        });

        Dialogs.showSecureDialog(dialog);
    }

    private void startIntroActivity() {
        if (!_isDoingIntro) {
            Intent intro = new Intent(this, IntroActivity.class);
            introResultLauncher.launch(intro);
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
            _entryListView.setEntries(_vaultManager.getVault().getEntries());
            _entryListView.onEntryAdded(entry);
        }
    }

    private void onEditEntryResult() {
        if (_loaded) {
            _entryListView.setEntries(_vaultManager.getVault().getEntries());
        }
    }

    private void onAssignIconsResult() {
        if (_loaded) {
            _entryListView.setEntries(_vaultManager.getVault().getEntries());
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
                    Dialogs.showMultiErrorDialog(this, R.string.import_error_title, getString(R.string.no_tokens_can_be_imported), errors, null);
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
                Dialogs.showMultiErrorDialog(this, R.string.import_error_title, getString(R.string.unable_to_read_qrcode_files, uris.size() - errors.size(), uris.size()), errors, dialogDismissHandler);
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
            startEditEntryActivityForNew(entries.get(0));
        } else if (entries.size() > 1) {
            for (VaultEntry entry: entries) {
                _vaultManager.getVault().addEntry(entry);
            }

            if (saveAndBackupVault()) {
                Toast.makeText(this, getResources().getQuantityString(R.plurals.added_new_entries, entries.size(), entries.size()), Toast.LENGTH_LONG).show();
            }

            _entryListView.setEntries(_vaultManager.getVault().getEntries());
        }
    }

    private void updateSortCategoryMenu() {
        if (_menu != null) {
            SortCategory category = _prefs.getCurrentSortCategory();
            _menu.findItem(category.getMenuItem()).setChecked(true);
        }
    }

    private void onIntroResult() {
        loadEntries();
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

    private void checkIconOptimization() {
        if (!_vaultManager.getVault().areIconsOptimized()) {
            Map<UUID, VaultEntryIcon> oldIcons = _vaultManager.getVault().getEntries().stream()
                    .filter(e -> e.getIcon() != null
                            && !e.getIcon().getType().equals(IconType.SVG)
                            && !BitmapHelper.isVaultEntryIconOptimized(e.getIcon()))
                    .collect(Collectors.toMap(VaultEntry::getUUID, VaultEntry::getIcon));

            if (!oldIcons.isEmpty()) {
                IconOptimizationTask task = new IconOptimizationTask(this, this::onIconsOptimized);
                task.execute(getLifecycle(), oldIcons);
            } else {
                onIconsOptimized(Collections.emptyMap());
            }
        }
    }

    private void onIconsOptimized(Map<UUID, VaultEntryIcon> newIcons) {
        for (Map.Entry<UUID, VaultEntryIcon> mapEntry : newIcons.entrySet()) {
            VaultEntry entry = _vaultManager.getVault().getEntryByUUID(mapEntry.getKey());
            entry.setIcon(mapEntry.getValue());
        }

        _vaultManager.getVault().setIconsOptimized(true);
        saveAndBackupVault();

        if (!newIcons.isEmpty()) {
            _entryListView.setEntries(_vaultManager.getVault().getEntries());
        }
    }

    private void onDecryptResult() {
        _auditLogRepository.addVaultUnlockedEvent();

        loadEntries();
    }

    private void startScanActivity() {
        if (!PermissionHelper.request(this, CODE_PERM_CAMERA, Manifest.permission.CAMERA)) {
            return;
        }

        Intent scannerActivity = new Intent(getApplicationContext(), ScannerActivity.class);
        scanResultLauncher.launch(scannerActivity);
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
        _vaultManager.fireIntentLauncher(this, chooserIntent, codeScanResultLauncher);
    }

    private void startPreferencesActivity() {
        startPreferencesActivity(null, null);
    }

    private void startPreferencesActivity(Class<? extends PreferencesFragment> fragmentType, String preference) {
        Intent intent = new Intent(this, PreferencesActivity.class);
        intent.putExtra("fragment", fragmentType);
        intent.putExtra("pref", preference);
        preferenceResultLauncher.launch(intent);
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
                    startEditEntryActivityForNew(entry);
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
                        startEditEntryActivityForNew(entry);
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
    protected void onStart() {
        super.onStart();

        if (_vaultManager.isVaultInitNeeded()) {
            if (_prefs.isIntroDone()) {
                Toast.makeText(this, getString(R.string.vault_not_found), Toast.LENGTH_SHORT).show();
            }
            startIntroActivity();
            return;
        }

        // If the vault is not loaded yet, try to load it now in case it's plain text
        if (!_vaultManager.isVaultLoaded()) {
            VaultFile vaultFile;
            try {
                vaultFile = VaultRepository.readVaultFile(this);
            } catch (VaultRepositoryException e) {
                e.printStackTrace();
                Dialogs.showErrorDialog(this, R.string.vault_load_error, e, (dialog, which) -> {
                    finish();
                });
                return;
            }

            if (!vaultFile.isEncrypted()) {
                try {
                    _vaultManager.loadFrom(vaultFile);
                } catch (VaultRepositoryException e) {
                    e.printStackTrace();
                    Dialogs.showErrorDialog(this, R.string.vault_load_error, e, (dialog, which) -> {
                        finish();
                    });
                    return;
                }
            }
        }

        if (!_vaultManager.isVaultLoaded()) {
            startAuthActivity(false);
        } else if (_loaded) {
            // update the list of groups in the entry list view so that the chip gets updated
            setGroups(_vaultManager.getVault().getUsedGroups());

            // update the usage counts in case they are edited outside of the EntryListView
            _entryListView.setUsageCounts(_prefs.getUsageCounts());

            _entryListView.setLastUsedTimestamps(_prefs.getLastUsedTimestamps());

            // refresh all codes to prevent showing old ones
            _entryListView.refresh(false);
        } else {
            loadEntries();
            checkTimeSyncSetting();
            checkIconOptimization();
        }

        _lockBackPressHandler.setEnabled(
                _vaultManager.isAutoLockEnabled(Preferences.AUTO_LOCK_ON_BACK_BUTTON)
        );

        handleIncomingIntent();
        updateLockIcon();
        updateSortCategoryMenu();
        doShortcutActions();
        updateErrorCard();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        _menu = menu;
        getMenuInflater().inflate(R.menu.menu_main, menu);

        updateLockIcon();
        updateSortCategoryMenu();

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
            _groupChip.setVisibility(_groups.isEmpty() ? View.GONE : View.VISIBLE);
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
            _groupChip.setVisibility(View.GONE);
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
        int itemId = item.getItemId();
        if (itemId == R.id.action_settings) {
            startPreferencesActivity();
        } else if (itemId == R.id.action_about) {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
        } else if (itemId == R.id.action_lock) {
            _vaultManager.lock(true);
        } else {
            if (item.getGroupId() == R.id.action_sort_category) {
                item.setChecked(true);

                SortCategory sortCategory;
                int subItemId = item.getItemId();
                if (subItemId == R.id.menu_sort_alphabetically) {
                    sortCategory = SortCategory.ISSUER;
                } else if (subItemId == R.id.menu_sort_alphabetically_reverse) {
                    sortCategory = SortCategory.ISSUER_REVERSED;
                } else if (subItemId == R.id.menu_sort_alphabetically_name) {
                    sortCategory = SortCategory.ACCOUNT;
                } else if (subItemId == R.id.menu_sort_alphabetically_name_reverse) {
                    sortCategory = SortCategory.ACCOUNT_REVERSED;
                } else if (subItemId == R.id.menu_sort_usage_count) {
                    sortCategory = SortCategory.USAGE_COUNT;
                } else if (subItemId == R.id.menu_sort_last_used) {
                    sortCategory = SortCategory.LAST_USED;
                } else {
                    sortCategory = SortCategory.CUSTOM;
                }

                _entryListView.setSortCategory(sortCategory, true);
                _prefs.setCurrentSortCategory(sortCategory);
            }

            return super.onOptionsItemSelected(item);
        }

        return true;
    }

    private void collapseSearchView() {
        _groupChip.setVisibility(_groups.isEmpty() ? View.GONE : View.VISIBLE);
        _searchView.setQuery(null, false);
        _searchView.setIconified(true);
    }

    private void loadEntries() {
        if (!_loaded) {
            setGroups(_vaultManager.getVault().getUsedGroups());
            _entryListView.setUsageCounts(_prefs.getUsageCounts());
            _entryListView.setLastUsedTimestamps(_prefs.getLastUsedTimestamps());
            _entryListView.setEntries(_vaultManager.getVault().getEntries());
            if (!_isRecreated) {
                _entryListView.runEntriesAnimation();
            }
            _loaded = true;
        }
    }

    private void startAuthActivity(boolean inhibitBioPrompt) {
        if (!_isAuthenticating) {
            Intent intent = new Intent(this, AuthActivity.class);
            intent.putExtra("inhibitBioPrompt", inhibitBioPrompt);
            authResultLauncher.launch(intent);
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

   private void updateErrorCard() {
       ErrorCardInfo info = null;

       Preferences.BackupResult backupRes = _prefs.getErroredBackupResult();
       if (backupRes != null) {
           info = new ErrorCardInfo(getString(R.string.backup_error_bar_message), view -> {
               Dialogs.showBackupErrorDialog(this, backupRes, (dialog, which) -> {
                   startPreferencesActivity(BackupsPreferencesFragment.class, "pref_backups");
               });
           });
       } else if (_prefs.isBackupsReminderNeeded() && _prefs.isBackupReminderEnabled()) {
           String text;
           Date date = _prefs.getLatestBackupOrExportTime();
           if (date != null) {
               text = getString(R.string.backup_reminder_bar_message_with_latest, TimeUtils.getElapsedSince(this, date));
           } else {
               text = getString(R.string.backup_reminder_bar_message);
           }
           info = new ErrorCardInfo(text, view -> {
               Dialogs.showSecureDialog(new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Aegis_AlertDialog_Error)
                       .setTitle(R.string.backup_reminder_bar_dialog_title)
                       .setMessage(R.string.backup_reminder_bar_dialog_summary)
                       .setIconAttribute(android.R.attr.alertDialogIcon)
                       .setPositiveButton(R.string.backup_reminder_bar_dialog_accept, (dialog, whichButton) -> {
                           startPreferencesActivity(BackupsPreferencesFragment.class, "pref_backups");
                       })
                       .setNegativeButton(android.R.string.cancel, null)
                       .create());
           });
       } else if (_prefs.isPlaintextBackupWarningNeeded()) {
           info = new ErrorCardInfo(getString(R.string.backup_plaintext_export_warning), view -> showPlaintextExportWarningOptions());
       }

       _entryListView.setErrorCardInfo(info);
   }

    private void showPlaintextExportWarningOptions() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_plaintext_warning, null);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Aegis_AlertDialog_Warning)
                .setTitle(R.string.backup_plaintext_export_warning)
                .setView(view)
                .setIconAttribute(android.R.attr.alertDialogIcon)
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

                updateErrorCard();
            });
        });

        Dialogs.showSecureDialog(dialog);
    }

    @Override
    public void onRestoreInstanceState(@Nullable Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState == null) {
            return;
        }

        HashSet<UUID> filter = (HashSet<UUID>) savedInstanceState.getSerializable("prefGroupFilter");
        if (filter != null) {
            _prefGroupFilter = filter;
        }
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

    private void setAssignIconsMenuItemVisibility() {
        MenuItem assignIconsMenuItem = _actionMode.getMenu().findItem(R.id.action_assign_icons);
        assignIconsMenuItem.setVisible(_iconPackManager.hasIconPack());
    }

    private void setFavoriteMenuItemVisiblity() {
        MenuItem toggleFavoriteMenuItem = _actionMode.getMenu().findItem(R.id.action_toggle_favorite);

        if (_selectedEntries.size() == 1){
            if (_selectedEntries.get(0).isFavorite()) {
                toggleFavoriteMenuItem.setIcon(R.drawable.ic_filled_star_24);
                toggleFavoriteMenuItem.setTitle(R.string.unfavorite);
            } else {
                toggleFavoriteMenuItem.setIcon(R.drawable.ic_outline_star_24);
                toggleFavoriteMenuItem.setTitle(R.string.favorite);
            }
        } else {
            toggleFavoriteMenuItem.setIcon(R.drawable.ic_outline_star_24);
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
        setAssignIconsMenuItemVisibility();
    }

    @Override
    public void onEntryMove(VaultEntry entry1, VaultEntry entry2) {
        _vaultManager.getVault().moveEntry(entry1, entry2);
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
    public void onSaveGroupFilter(Set<UUID> groupFilter) {
        if (_vaultManager.getVault().isGroupsMigrationFresh()) {
            saveAndBackupVault();
        }
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

    @Override
    protected boolean saveAndBackupVault() {
        boolean res = super.saveAndBackupVault();
        updateErrorCard();
        return res;
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

            int itemId = item.getItemId();
            if (itemId == R.id.action_copy) {
                copyEntryCode(_selectedEntries.get(0));
                mode.finish();
            } else if (itemId == R.id.action_edit) {
                startEditEntryActivity(_selectedEntries.get(0));
                mode.finish();
            } else if (itemId == R.id.action_toggle_favorite) {
                for (VaultEntry entry : _selectedEntries) {
                    _vaultManager.getVault().editEntry(entry, newEntry -> {
                        newEntry.setIsFavorite(!newEntry.isFavorite());
                    });
                }

                saveAndBackupVault();
                _entryListView.setEntries(_vaultManager.getVault().getEntries());
                mode.finish();
            } else if (itemId == R.id.action_share_qr) {
                Intent intent = new Intent(getBaseContext(), TransferEntriesActivity.class);
                ArrayList<GoogleAuthInfo> authInfos = new ArrayList<>();
                for (VaultEntry entry : _selectedEntries) {
                    GoogleAuthInfo authInfo = new GoogleAuthInfo(entry.getInfo(), entry.getName(), entry.getIssuer());
                    authInfos.add(authInfo);

                    _auditLogRepository.addEntrySharedEvent(entry.getUUID().toString());
                }

                intent.putExtra("authInfos", authInfos);
                startActivity(intent);

                mode.finish();
            } else if (itemId == R.id.action_delete) {
                Dialogs.showDeleteEntriesDialog(MainActivity.this, _selectedEntries, (d, which) -> {
                    for (VaultEntry entry : _selectedEntries) {
                        _vaultManager.getVault().removeEntry(entry);
                    }
                    saveAndBackupVault();
                    _entryListView.setGroups(_vaultManager.getVault().getUsedGroups());
                    _entryListView.setEntries(_vaultManager.getVault().getEntries());
                    mode.finish();
                });
            } else if (itemId == R.id.action_select_all) {
                _selectedEntries = _entryListView.selectAllEntries();
                setFavoriteMenuItemVisiblity();
                setIsMultipleSelected(_selectedEntries.size() > 1);
            } else if (itemId == R.id.action_assign_icons) {
                startAssignIconsActivity(_selectedEntries);
                mode.finish();
            } else if (itemId == R.id.action_assign_groups) {
                startAssignGroupsDialog();
            } else {
                return false;
            }

            return true;
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
