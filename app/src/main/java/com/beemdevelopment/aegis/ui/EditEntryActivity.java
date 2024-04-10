package com.beemdevelopment.aegis.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;

import com.amulyakhare.textdrawable.TextDrawable;
import com.avito.android.krop.KropView;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.encoding.Base32;
import com.beemdevelopment.aegis.encoding.EncodingException;
import com.beemdevelopment.aegis.encoding.Hex;
import com.beemdevelopment.aegis.helpers.AnimationsHelper;
import com.beemdevelopment.aegis.helpers.DropdownHelper;
import com.beemdevelopment.aegis.helpers.EditTextHelper;
import com.beemdevelopment.aegis.helpers.SafHelper;
import com.beemdevelopment.aegis.helpers.SimpleAnimationEndListener;
import com.beemdevelopment.aegis.helpers.SimpleTextWatcher;
import com.beemdevelopment.aegis.helpers.TextDrawableHelper;
import com.beemdevelopment.aegis.icons.IconPack;
import com.beemdevelopment.aegis.icons.IconType;
import com.beemdevelopment.aegis.otp.GoogleAuthInfo;
import com.beemdevelopment.aegis.otp.HotpInfo;
import com.beemdevelopment.aegis.otp.MotpInfo;
import com.beemdevelopment.aegis.otp.OtpInfo;
import com.beemdevelopment.aegis.otp.OtpInfoException;
import com.beemdevelopment.aegis.otp.SteamInfo;
import com.beemdevelopment.aegis.otp.TotpInfo;
import com.beemdevelopment.aegis.otp.YandexInfo;
import com.beemdevelopment.aegis.ui.dialogs.Dialogs;
import com.beemdevelopment.aegis.ui.dialogs.IconPickerDialog;
import com.beemdevelopment.aegis.ui.glide.GlideHelper;
import com.beemdevelopment.aegis.ui.models.VaultGroupModel;
import com.beemdevelopment.aegis.ui.tasks.ImportFileTask;
import com.beemdevelopment.aegis.ui.views.IconAdapter;
import com.beemdevelopment.aegis.util.Cloner;
import com.beemdevelopment.aegis.util.IOUtils;
import com.beemdevelopment.aegis.vault.VaultEntry;
import com.beemdevelopment.aegis.vault.VaultEntryIcon;
import com.beemdevelopment.aegis.vault.VaultGroup;
import com.beemdevelopment.aegis.vault.VaultRepository;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import de.hdodenhof.circleimageview.CircleImageView;

public class EditEntryActivity extends AegisActivity {
    private boolean _isNew = false;
    private boolean _isManual = false;
    private VaultEntry _origEntry;
    private Collection<VaultGroup> _groups;
    private boolean _hasCustomIcon = false;
    // keep track of icon changes separately as the generated jpeg's are not deterministic
    private boolean _hasChangedIcon = false;
    private IconPack.Icon _selectedIcon;
    private CircleImageView _iconView;
    private ImageView _saveImageButton;

    private TextInputEditText _textName;
    private TextInputEditText _textIssuer;
    private TextInputLayout _textGroupLayout;
    private TextInputEditText _textGroup;
    private TextInputEditText _textPeriodCounter;
    private TextInputLayout _textPeriodCounterLayout;
    private TextInputEditText _textDigits;
    private TextInputLayout _textDigitsLayout;
    private TextInputEditText _textSecret;
    private TextInputEditText _textPin;
    private LinearLayout _textPinLayout;
    private TextInputEditText _textUsageCount;
    private TextInputEditText _textNote;
    private TextView _textLastUsed;

    private AutoCompleteTextView _dropdownType;
    private AutoCompleteTextView _dropdownAlgo;
    private TextInputLayout _dropdownAlgoLayout;
    private List<UUID> _selectedGroups = new ArrayList<>();

    private KropView _kropView;

    private RelativeLayout _advancedSettingsHeader;
    private RelativeLayout _advancedSettings;

    private BackPressHandler _backPressHandler;
    private IconBackPressHandler _iconBackPressHandler;

    private final ActivityResultLauncher<Intent> pickImageResultLauncher =
            registerForActivityResult(new StartActivityForResult(), activityResult -> {
                Intent data = activityResult.getData();
                if (activityResult.getResultCode() != RESULT_OK || data == null || data.getData() == null) {
                    return;
                }
                String fileType = SafHelper.getMimeType(this, data.getData());
                if (fileType != null && fileType.equals(IconType.SVG.toMimeType())) {
                    ImportFileTask.Params params = new ImportFileTask.Params(data.getData(), "icon", null);
                    ImportFileTask task = new ImportFileTask(this, result -> {
                        if (result.getError() == null) {
                            CustomSvgIcon icon = new CustomSvgIcon(result.getFile());
                            selectIcon(icon);
                        } else {
                            Dialogs.showErrorDialog(this, R.string.reading_file_error, result.getError());
                        }
                    });
                    task.execute(getLifecycle(), params);
                } else {
                    startEditingIcon(data.getData());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (abortIfOrphan(savedInstanceState)) {
            return;
        }
        setContentView(R.layout.activity_edit_entry);
        setSupportActionBar(findViewById(R.id.toolbar));

        _groups = _vaultManager.getVault().getGroups();

        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setHomeAsUpIndicator(R.drawable.ic_outline_close_24);
            bar.setDisplayHomeAsUpEnabled(true);
        }

        _backPressHandler = new BackPressHandler();
        getOnBackPressedDispatcher().addCallback(this, _backPressHandler);
        _iconBackPressHandler = new IconBackPressHandler();
        getOnBackPressedDispatcher().addCallback(this, _iconBackPressHandler);

        // retrieve info from the calling activity
        Intent intent = getIntent();
        UUID entryUUID = (UUID) intent.getSerializableExtra("entryUUID");
        if (entryUUID != null) {
            _origEntry = _vaultManager.getVault().getEntryByUUID(entryUUID);
        } else {
            _origEntry = (VaultEntry) intent.getSerializableExtra("newEntry");
            _isManual = intent.getBooleanExtra("isManual", false);
            _isNew = true;
            setTitle(R.string.add_new_entry);
        }

        // set up fields
        _iconView = findViewById(R.id.profile_drawable);
        _kropView = findViewById(R.id.krop_view);
        _saveImageButton = findViewById(R.id.iv_saveImage);
        _textName = findViewById(R.id.text_name);
        _textIssuer = findViewById(R.id.text_issuer);
        _textGroup = findViewById(R.id.text_group);
        _textGroupLayout = findViewById(R.id.text_group_layout);
        _textPeriodCounter = findViewById(R.id.text_period_counter);
        _textPeriodCounterLayout = findViewById(R.id.text_period_counter_layout);
        _textDigits = findViewById(R.id.text_digits);
        _textDigitsLayout = findViewById(R.id.text_digits_layout);
        _textSecret = findViewById(R.id.text_secret);
        _textPin = findViewById(R.id.text_pin);
        _textPinLayout = findViewById(R.id.layout_pin);
        _textUsageCount = findViewById(R.id.text_usage_count);
        _textNote = findViewById(R.id.text_note);
        _textLastUsed = findViewById(R.id.text_last_used);
        _dropdownType = findViewById(R.id.dropdown_type);
        DropdownHelper.fillDropdown(this, _dropdownType, R.array.otp_types_array);
        _dropdownAlgoLayout = findViewById(R.id.dropdown_algo_layout);
        _dropdownAlgo = findViewById(R.id.dropdown_algo);
        DropdownHelper.fillDropdown(this, _dropdownAlgo, R.array.otp_algo_array);

        // if this is NOT a manually entered entry, move the "Secret" field from basic to advanced settings
        if (!_isNew || !_isManual) {
            int secretIndex = 0;
            LinearLayout layoutSecret = findViewById(R.id.layout_secret);
            LinearLayout layoutBasic = findViewById(R.id.layout_basic);
            LinearLayout layoutAdvanced = findViewById(R.id.layout_advanced);
            layoutBasic.removeView(layoutSecret);
            if (!_isNew) {
                secretIndex = 1;
                layoutBasic.removeView(_textPinLayout);
                layoutAdvanced.addView(_textPinLayout, 0);
                ((LinearLayout.LayoutParams) _textPinLayout.getLayoutParams()).topMargin = 0;
            } else {
                ((LinearLayout.LayoutParams) layoutSecret.getLayoutParams()).topMargin = 0;
            }
            layoutAdvanced.addView(layoutSecret, secretIndex);

            if (_isNew && !_isManual) {
                setViewEnabled(layoutAdvanced, false);
            }
        } else {
            LinearLayout layoutTypeAlgo = findViewById(R.id.layout_type_algo);
            ((LinearLayout.LayoutParams) layoutTypeAlgo.getLayoutParams()).topMargin = 0;
        }

        _advancedSettingsHeader = findViewById(R.id.accordian_header);
        _advancedSettingsHeader.setOnClickListener(v -> openAdvancedSettings());
        _advancedSettings = findViewById(R.id.expandableLayout);

        // fill the fields with values if possible
        GlideHelper.loadEntryIcon(Glide.with(this), _origEntry, _iconView);
        if (_origEntry.hasIcon()) {
            _hasCustomIcon = true;
        }

        _textName.setText(_origEntry.getName());
        _textIssuer.setText(_origEntry.getIssuer());
        _textNote.setText(_origEntry.getNote());

        OtpInfo info = _origEntry.getInfo();

        if (info instanceof TotpInfo) {
            _textPeriodCounterLayout.setHint(R.string.period_hint);
            _textPeriodCounter.setText(Integer.toString(((TotpInfo) info).getPeriod()));
        } else if (info instanceof HotpInfo) {
            _textPeriodCounterLayout.setHint(R.string.counter);
            _textPeriodCounter.setText(Long.toString(((HotpInfo) info).getCounter()));
        } else {
            throw new RuntimeException(String.format("Unsupported OtpInfo type: %s", info.getClass()));
        }
        _textDigits.setText(Integer.toString(info.getDigits()));

        byte[] secretBytes = _origEntry.getInfo().getSecret();
        if (secretBytes != null) {
            String secretString = (info instanceof MotpInfo) ? Hex.encode(secretBytes) : Base32.encode(secretBytes);
            _textSecret.setText(secretString);
        }

        _dropdownType.setText(_origEntry.getInfo().getType(), false);
        _dropdownAlgo.setText(_origEntry.getInfo().getAlgorithm(false), false);

        if (info instanceof YandexInfo) {
            _textPin.setText(((YandexInfo) info).getPin());
        } else if (info instanceof MotpInfo) {
            _textPin.setText(((MotpInfo) info).getPin());
        }

        updateAdvancedFieldStatus(_origEntry.getInfo().getTypeId());
        updatePinFieldVisibility(_origEntry.getInfo().getTypeId());

        Set<UUID> groups = _origEntry.getGroups();
        if (groups.isEmpty()) {
            _textGroup.setText(getString(R.string.no_group));
        } else {
            String text = groups.stream().map(uuid -> {
                VaultGroup group = _vaultManager.getVault().getGroupByUUID(uuid);
                return group.getName();
            })
            .collect(Collectors.joining(", "));
            _selectedGroups.addAll(groups);
            _textGroup.setText(text);
        }

        // Update the icon if the issuer or name has changed
        _textIssuer.addTextChangedListener(_nameChangeListener);
        _textName.addTextChangedListener(_nameChangeListener);

        // Register listeners to trigger validation
        _textIssuer.addTextChangedListener(_validationListener);
        _textGroup.addTextChangedListener(_validationListener);
        _textName.addTextChangedListener(_validationListener);
        _textNote.addTextChangedListener(_validationListener);
        _textSecret.addTextChangedListener(_validationListener);
        _dropdownType.addTextChangedListener(_validationListener);
        _dropdownAlgo.addTextChangedListener(_validationListener);
        _textPeriodCounter.addTextChangedListener(_validationListener);
        _textDigits.addTextChangedListener(_validationListener);
        _textPin.addTextChangedListener(_validationListener);

        // show/hide period and counter fields on type change
        _dropdownType.setOnItemClickListener((parent, view, position, id) -> {
            String type = _dropdownType.getText().toString().toLowerCase(Locale.ROOT);
            switch (type) {
                case SteamInfo.ID:
                    _dropdownAlgo.setText(OtpInfo.DEFAULT_ALGORITHM, false);
                    _textPeriodCounterLayout.setHint(R.string.period_hint);
                    _textPeriodCounter.setText(String.valueOf(TotpInfo.DEFAULT_PERIOD));
                    _textDigits.setText(String.valueOf(SteamInfo.DIGITS));
                    break;
                case TotpInfo.ID:
                    _dropdownAlgo.setText(OtpInfo.DEFAULT_ALGORITHM, false);
                    _textPeriodCounterLayout.setHint(R.string.period_hint);
                    _textPeriodCounter.setText(String.valueOf(TotpInfo.DEFAULT_PERIOD));
                    _textDigits.setText(String.valueOf(OtpInfo.DEFAULT_DIGITS));
                    break;
                case HotpInfo.ID:
                    _dropdownAlgo.setText(OtpInfo.DEFAULT_ALGORITHM, false);
                    _textPeriodCounterLayout.setHint(R.string.counter);
                    _textPeriodCounter.setText(String.valueOf(HotpInfo.DEFAULT_COUNTER));
                    _textDigits.setText(String.valueOf(OtpInfo.DEFAULT_DIGITS));
                    break;
                case YandexInfo.ID:
                    _dropdownAlgo.setText(YandexInfo.DEFAULT_ALGORITHM, false);
                    _textPeriodCounterLayout.setHint(R.string.period_hint);
                    _textPeriodCounter.setText(String.valueOf(TotpInfo.DEFAULT_PERIOD));
                    _textDigits.setText(String.valueOf(YandexInfo.DIGITS));
                    break;
                case MotpInfo.ID:
                    _dropdownAlgo.setText(MotpInfo.ALGORITHM, false);
                    _textPeriodCounterLayout.setHint(R.string.period_hint);
                    _textPeriodCounter.setText(String.valueOf(MotpInfo.PERIOD));
                    _textDigits.setText(String.valueOf(MotpInfo.DIGITS));
                    break;
                default:
                    throw new RuntimeException(String.format("Unsupported OTP type: %s", type));
            }

            updateAdvancedFieldStatus(type);
            updatePinFieldVisibility(type);
        });

        _iconView.setOnClickListener(v -> {
            startIconSelection();
        });

        _textGroup.setShowSoftInputOnFocus(false);
        _textGroup.setOnClickListener(v -> showGroupSelectionDialog());
        _textGroup.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                showGroupSelectionDialog();
            }
        });

        _textGroupLayout.setOnClickListener(v -> {
            showGroupSelectionDialog();
        });

        _textUsageCount.setText(_prefs.getUsageCount(entryUUID).toString());
        setLastUsedTimestamp(_prefs.getLastUsedTimestamp(entryUUID));
    }

    private void showGroupSelectionDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_select_groups, null);
        dialog.setContentView(view);

        ChipGroup chipGroup = view.findViewById(R.id.groupChipGroup);
        TextView addGroupInfo = view.findViewById(R.id.addGroupInfo);
        LinearLayout addGroup = view.findViewById(R.id.addGroup);
        Button clearButton = view.findViewById(R.id.btnClear);
        Button saveButton = view.findViewById(R.id.btnSave);

        chipGroup.removeAllViews();
        addGroupInfo.setVisibility(View.VISIBLE);
        addGroup.setVisibility(View.VISIBLE);

        for (VaultGroup group : _groups) {
            addChipTo(chipGroup, new VaultGroupModel(group), false);
        }

        addGroup.setOnClickListener(v1 -> {
            Dialogs.TextInputListener onAddGroup = text -> {
                String groupName = new String(text).trim();
                if (!groupName.isEmpty()) {
                    VaultGroup group = _vaultManager.getVault().findGroupByName(groupName);
                    if (group == null) {
                        group = new VaultGroup(groupName);
                        _vaultManager.getVault().addGroup(group);
                    }

                    _selectedGroups.add(group.getUUID());
                    addChipTo(chipGroup, new VaultGroupModel(group), true);
                }
            };

            Dialogs.showTextInputDialog(EditEntryActivity.this, R.string.set_group, R.string.group_name_hint, onAddGroup);
        });

        saveButton.setOnClickListener(v1 -> {
            if(getCheckedUUID(chipGroup).isEmpty()) {
                _selectedGroups.clear();
                _textGroup.setText(getString(R.string.no_group));
            } else {
                _selectedGroups.clear();
                _selectedGroups.addAll(getCheckedUUID(chipGroup));
                _textGroup.setText(getCheckedNames(chipGroup));
            }
            dialog.dismiss();
        });

        clearButton.setOnClickListener(v1 -> {
            chipGroup.clearCheck();
        });

        Dialogs.showSecureDialog(dialog);
    }

    private void addChipTo(ChipGroup chipGroup, VaultGroupModel group, Boolean isNew) {
        Chip chip = (Chip) getLayoutInflater().inflate(R.layout.chip_group_filter, null, false);
        chip.setText(group.getName());
        chip.setCheckable(true);

        chip.setChecked((!_selectedGroups.isEmpty() && _selectedGroups.contains(group.getUUID())) || isNew);
        chip.setCheckedIconVisible(true);
        chip.setTag(group);
        chipGroup.addView(chip);
    }

    private static Set<UUID> getCheckedUUID(ChipGroup chipGroup) {
        return chipGroup.getCheckedChipIds().stream()
                .map(i -> {
                    Chip chip = chipGroup.findViewById(i);
                    VaultGroupModel group = (VaultGroupModel) chip.getTag();
                    return group.getUUID();
                })
                .collect(Collectors.toSet());
    }

    private static String getCheckedNames(ChipGroup chipGroup) {
        return chipGroup.getCheckedChipIds().stream()
                .map(i -> {
                    Chip chip = chipGroup.findViewById(i);
                    VaultGroupModel group = (VaultGroupModel) chip.getTag();
                    return group.getName();
                })
                .collect(Collectors.joining(", "));
    }

    private void updateAdvancedFieldStatus(String otpType) {
        boolean enabled = !otpType.equals(SteamInfo.ID) && !otpType.equals(YandexInfo.ID)
                && !otpType.equals(MotpInfo.ID) && (!_isNew || _isManual);
        _textDigitsLayout.setEnabled(enabled);
        _textPeriodCounterLayout.setEnabled(enabled);
        _dropdownAlgoLayout.setEnabled(enabled);
    }

    private void updatePinFieldVisibility(String otpType) {
        boolean visible = otpType.equals(YandexInfo.ID) || otpType.equals(MotpInfo.ID);
        _textPinLayout.setVisibility(visible ? View.VISIBLE : View.GONE);
        _textPin.setHint(otpType.equals(MotpInfo.ID) ? R.string.motp_pin : R.string.yandex_pin);
    }

    private void openAdvancedSettings() {
        Animation fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setInterpolator(new AccelerateInterpolator());
        fadeOut.setDuration((long) (220 * AnimationsHelper.Scale.ANIMATOR.getValue(this)));
        _advancedSettingsHeader.startAnimation(fadeOut);

        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setInterpolator(new AccelerateInterpolator());
        fadeIn.setDuration((long) (250 * AnimationsHelper.Scale.ANIMATOR.getValue(this)));

        fadeOut.setAnimationListener(new SimpleAnimationEndListener((a) -> {
            _advancedSettingsHeader.setVisibility(View.GONE);
            _advancedSettings.startAnimation(fadeIn);
        }));

        fadeIn.setAnimationListener(new SimpleAnimationEndListener((a) -> {
            _advancedSettings.setVisibility(View.VISIBLE);
        }));
    }

    private boolean hasUnsavedChanges(VaultEntry newEntry) {
        return _hasChangedIcon || !_origEntry.equals(newEntry);
    }

    private void discardAndFinish() {
        AtomicReference<String> msg = new AtomicReference<>();
        AtomicReference<VaultEntry> entry = new AtomicReference<>();
        try {
            entry.set(parseEntry());
        } catch (ParseException e) {
            msg.set(e.getMessage());
        }

        if (!hasUnsavedChanges(entry.get())) {
            finish();
            return;
        }

        // ask for confirmation if the entry has been changed
        Dialogs.showDiscardDialog(EditEntryActivity.this,
                (dialog, which) -> {
                    // if the entry couldn't be parsed, we show an error dialog
                    if (msg.get() != null) {
                        onSaveError(msg.get());
                        return;
                    }

                    addAndFinish(entry.get());
                },
                (dialog, which) -> finish()
        );
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            discardAndFinish();
        } else if (itemId == R.id.action_save) {
            onSave();
        } else if (itemId == R.id.action_delete) {
            Dialogs.showDeleteEntriesDialog(this, Collections.singletonList(_origEntry), (dialog, which) -> {
                deleteAndFinish(_origEntry);
            });
        } else if (itemId == R.id.action_edit_icon) {
            startIconSelection();
        } else if (itemId == R.id.action_reset_usage_count) {
            Dialogs.showSecureDialog(new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.action_reset_usage_count)
                    .setMessage(R.string.action_reset_usage_count_dialog)
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> resetUsageCount())
                    .setNegativeButton(android.R.string.no, null)
                    .create());
        } else if (itemId == R.id.action_default_icon) {
            TextDrawable drawable = TextDrawableHelper.generate(_origEntry.getIssuer(), _origEntry.getName(), _iconView);
            _iconView.setImageDrawable(drawable);

            _selectedIcon = null;
            _hasCustomIcon = false;
            _hasChangedIcon = true;
        } else {
            return super.onOptionsItemSelected(item);
        }

        return true;
    }

    private void startImageSelectionActivity() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setDataAndType(android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI, "image/*");

        Intent fileIntent = new Intent(Intent.ACTION_GET_CONTENT);
        fileIntent.setType("image/*");

        Intent chooserIntent = Intent.createChooser(galleryIntent, getString(R.string.select_icon));
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] { fileIntent });
        _vaultManager.fireIntentLauncher(this, chooserIntent, pickImageResultLauncher);
    }

    private void resetUsageCount() {
        _prefs.resetUsageCount(_origEntry.getUUID());
        _textUsageCount.setText("0");
    }

    private void startIconSelection() {
        List<IconPack> iconPacks = _iconPackManager.getIconPacks().stream()
                .sorted(Comparator.comparing(IconPack::getName))
                .collect(Collectors.toList());
        if (iconPacks.size() == 0) {
            startImageSelectionActivity();
            return;
        }

        BottomSheetDialog dialog = IconPickerDialog.create(this, iconPacks, _textIssuer.getText().toString(), true, new IconAdapter.Listener() {
            @Override
            public void onIconSelected(IconPack.Icon icon) {
                selectIcon(icon);
            }

            @Override
            public void onCustomSelected() {
                startImageSelectionActivity();
            }
        });
        Dialogs.showSecureDialog(dialog);
    }

    private void selectIcon(IconPack.Icon icon) {
        _selectedIcon = icon;
        _hasCustomIcon = true;
        _hasChangedIcon = true;

        GlideHelper.loadIcon(Glide.with(EditEntryActivity.this), icon, _iconView);
    }

    private void startEditingIcon(Uri data) {
        Glide.with(this)
                .asBitmap()
                .load(data)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(false)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        _kropView.setBitmap(resource);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                });
        _iconView.setVisibility(View.GONE);
        _kropView.setVisibility(View.VISIBLE);

        _saveImageButton.setOnClickListener(v -> {
            stopEditingIcon(true);
        });

        _iconBackPressHandler.setEnabled(true);
    }

    private void stopEditingIcon(boolean save) {
        if (save && _selectedIcon == null) {
            _iconView.setImageBitmap(_kropView.getCroppedBitmap());
        }
        _iconView.setVisibility(View.VISIBLE);
        _kropView.setVisibility(View.GONE);

        _hasCustomIcon = _hasCustomIcon || save;
        _hasChangedIcon = save;
        _iconBackPressHandler.setEnabled(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edit, menu);
        if (_isNew) {
            menu.findItem(R.id.action_delete).setVisible(false);
        }
        if (!_hasCustomIcon) {
            menu.findItem(R.id.action_default_icon).setVisible(false);
        }

        return true;
    }

    private void addAndFinish(VaultEntry entry) {
        // It's possible that the new entry was already added to the vault, but writing the
        // vault to disk failed, causing the user to tap 'Save' again. Calling addEntry
        // again would cause a crash in that case, so the isEntryDuplicate check prevents
        // that.
        VaultRepository vault = _vaultManager.getVault();
        if (_isNew && !vault.isEntryDuplicate(entry)) {
            vault.addEntry(entry);
        } else {
            vault.replaceEntry(entry);
        }

        saveAndFinish(entry, false);
    }

    private void setLastUsedTimestamp(long timestamp) {
        String readableDate = getString(R.string.last_used_never);
        if (timestamp != 0) {
            DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.getDefault());
            readableDate = dateFormat.format(new Date(timestamp));
        }

        _textLastUsed.setText(String.format("%s: %s", getString(R.string.last_used), readableDate));
    }

    private void deleteAndFinish(VaultEntry entry) {
        _vaultManager.getVault().removeEntry(entry);
        saveAndFinish(entry, true);
    }

    private void saveAndFinish(VaultEntry entry, boolean delete) {
        Intent intent = new Intent();
        intent.putExtra("entryUUID", entry.getUUID());
        intent.putExtra("delete", delete);

        if (saveAndBackupVault()) {
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    private int parsePeriod() throws ParseException {
        try {
            return Integer.parseInt(_textPeriodCounter.getText().toString());
        } catch (NumberFormatException e) {
            throw new ParseException("Period is not an integer.");
        }
    }

    private VaultEntry parseEntry() throws ParseException {
        if (_textSecret.length() == 0) {
            throw new ParseException("Secret is a required field.");
        }

        String type = _dropdownType.getText().toString();
        String algo = _dropdownAlgo.getText().toString();
        String lowerCasedType = type.toLowerCase(Locale.ROOT);

        if (lowerCasedType.equals(YandexInfo.ID) || lowerCasedType.equals(MotpInfo.ID)) {
            int pinLength = _textPin.length();
            if (pinLength < 4) {
                throw new ParseException("PIN is a required field. Must have a minimum length of 4 digits.");
            }
            if (pinLength != 4 && lowerCasedType.equals(MotpInfo.ID)) {
                throw new ParseException("PIN must have a length of 4 digits.");
            }
        }

        int digits;
        try {
            digits = Integer.parseInt(_textDigits.getText().toString());
        } catch (NumberFormatException e) {
            throw new ParseException("Digits is not an integer.");
        }

        byte[] secret;
        try {
            String secretString = new String(EditTextHelper.getEditTextChars(_textSecret));

            secret = (lowerCasedType.equals(MotpInfo.ID)) ?
                    Hex.decode(secretString) : GoogleAuthInfo.parseSecret(secretString);

            if (secret.length == 0) {
                throw new ParseException("Secret cannot be empty");
            }
        } catch (EncodingException e) {
            String exceptionMessage = (lowerCasedType.equals(MotpInfo.ID)) ?
                    "Secret is not valid hexadecimal" : "Secret is not valid base32.";

            throw new ParseException(exceptionMessage);
        }

        OtpInfo info;
        try {
            switch (type.toLowerCase(Locale.ROOT)) {
                case TotpInfo.ID:
                    info = new TotpInfo(secret, algo, digits, parsePeriod());
                    break;
                case SteamInfo.ID:
                    info = new SteamInfo(secret, algo, digits, parsePeriod());
                    break;
                case HotpInfo.ID:
                    long counter;
                    try {
                        counter = Long.parseLong(_textPeriodCounter.getText().toString());
                    } catch (NumberFormatException e) {
                        throw new ParseException("Counter is not an integer.");
                    }
                    info = new HotpInfo(secret, algo, digits, counter);
                    break;
                case YandexInfo.ID:
                    info = new YandexInfo(secret, _textPin.getText().toString());
                    break;
                case MotpInfo.ID:
                    info = new MotpInfo(secret, _textPin.getText().toString());
                    break;
                default:
                    throw new RuntimeException(String.format("Unsupported OTP type: %s", type));
            }

            info.setDigits(digits);
            info.setAlgorithm(algo);
        } catch (OtpInfoException e) {
            throw new ParseException("The entered info is incorrect: " + e.getMessage());
        }

        VaultEntry entry = Cloner.clone(_origEntry);
        entry.setInfo(info);
        entry.setIssuer(_textIssuer.getText().toString());
        entry.setName(_textName.getText().toString());
        entry.setNote(_textNote.getText().toString());

        if (_selectedGroups.isEmpty()) {
            entry.setGroups(new HashSet<>());
        } else {
            entry.setGroups(new HashSet<>(_selectedGroups));
        }

        if (_hasChangedIcon) {
            if (_hasCustomIcon) {
                VaultEntryIcon icon;
                if (_selectedIcon == null) {
                    Bitmap bitmap = ((BitmapDrawable) _iconView.getDrawable()).getBitmap();
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    // the quality parameter is ignored for PNG
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    byte[] data = stream.toByteArray();
                    icon = new VaultEntryIcon(data, IconType.PNG);
                } else {
                    byte[] iconBytes;
                    try (FileInputStream inStream = new FileInputStream(_selectedIcon.getFile())){
                        iconBytes = IOUtils.readFile(inStream);
                    } catch (IOException e) {
                        throw new ParseException(e.getMessage());
                    }
                    icon = new VaultEntryIcon(iconBytes, _selectedIcon.getIconType());
                }

                entry.setIcon(icon);
            } else {
                entry.setIcon(null);
            }
        }

        return entry;
    }

    private void onSaveError(String msg) {
        Dialogs.showSecureDialog(new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Aegis_AlertDialog_Error)
                .setTitle(getString(R.string.saving_profile_error))
                .setMessage(msg)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setPositiveButton(android.R.string.ok, null)
                .create());
    }

    private boolean onSave() {
        if (_iconBackPressHandler.isEnabled()) {
            stopEditingIcon(true);
        }

        VaultEntry entry;
        try {
            entry = parseEntry();
        } catch (ParseException e) {
            onSaveError(e.getMessage());
            return false;
        }

        addAndFinish(entry);
        return true;
    }

    private static void setViewEnabled(View view, boolean enabled) {
        view.setEnabled(enabled);

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                setViewEnabled(group.getChildAt(i), enabled);
            }
        }
    }

    private final TextWatcher _validationListener = new SimpleTextWatcher((s) -> {
        updateBackPressHandlerState();
    });

    private final TextWatcher _nameChangeListener = new SimpleTextWatcher((s) -> {
        if (!_hasCustomIcon) {
            TextDrawable drawable = TextDrawableHelper.generate(_textIssuer.getText().toString(), _textName.getText().toString(), _iconView);
            _iconView.setImageDrawable(drawable);
        }
    });

    private void updateBackPressHandlerState() {
        VaultEntry entry = null;
        try {
            entry = parseEntry();
        } catch (ParseException ignored) {

        }

        boolean backEnabled = hasUnsavedChanges(entry);
        _backPressHandler.setEnabled(backEnabled);
    }

    private class BackPressHandler extends OnBackPressedCallback {
        public BackPressHandler() {
            super(false);
        }

        @Override
        public void handleOnBackPressed() {
            discardAndFinish();
        }
    }

    private class IconBackPressHandler extends OnBackPressedCallback {
        public IconBackPressHandler() {
            super(false);
        }

        @Override
        public void handleOnBackPressed() {
            stopEditingIcon(false);
        }
    }

    private static class ParseException extends Exception {
        public ParseException(String message) {
            super(message);
        }
    }

    private static class CustomSvgIcon extends IconPack.Icon {
        private final File _file;

        protected CustomSvgIcon(File file) {
            super(file.getAbsolutePath(), null, null, null);
            _file = file;
        }

        @Nullable
        @Override
        public File getFile() {
            return _file;
        }

        @Override
        public IconType getIconType() {
            return IconType.SVG;
        }
    }
}
