package com.beemdevelopment.aegis.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.ArrayRes;
import androidx.appcompat.app.ActionBar;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TableRow;

import com.amulyakhare.textdrawable.TextDrawable;
import com.avito.android.krop.KropView;
import com.beemdevelopment.aegis.encoding.Base32;
import com.beemdevelopment.aegis.encoding.Base32Exception;
import com.beemdevelopment.aegis.helpers.EditTextHelper;
import com.beemdevelopment.aegis.helpers.SpinnerHelper;
import com.beemdevelopment.aegis.helpers.TextDrawableHelper;
import com.beemdevelopment.aegis.otp.HotpInfo;
import com.beemdevelopment.aegis.otp.OtpInfo;
import com.beemdevelopment.aegis.otp.OtpInfoException;
import com.beemdevelopment.aegis.otp.SteamInfo;
import com.beemdevelopment.aegis.otp.TotpInfo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

import androidx.appcompat.app.AlertDialog;
import de.hdodenhof.circleimageview.CircleImageView;
import com.beemdevelopment.aegis.R;
import com.beemdevelopment.aegis.db.DatabaseEntry;

public class EditEntryActivity extends AegisActivity {
    private static final int PICK_IMAGE_REQUEST = 0;

    private boolean _isNew = false;
    private DatabaseEntry _origEntry;
    private TreeSet<String> _groups;
    private boolean _hasCustomIcon = false;
    // keep track of icon changes separately as the generated jpeg's are not deterministic
    private boolean _hasChangedIcon = false;
    private CircleImageView _iconView;
    private ImageView _saveImageButton;

    private EditText _textName;
    private EditText _textIssuer;
    private EditText _textPeriod;
    private EditText _textCounter;
    private EditText _textDigits;
    private EditText _textSecret;

    private TableRow _rowPeriod;
    private TableRow _rowCounter;

    private Spinner _spinnerType;
    private Spinner _spinnerAlgo;
    private Spinner _spinnerGroup;
    private List<String> _spinnerGroupList = new ArrayList<>();

    private KropView _kropView;

    private RelativeLayout _advancedSettingsHeader;
    private RelativeLayout _advancedSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_entry);

        ActionBar bar = getSupportActionBar();
        bar.setHomeAsUpIndicator(R.drawable.ic_close);
        bar.setDisplayHomeAsUpEnabled(true);

        // retrieve info from the calling activity
        Intent intent = getIntent();
        _origEntry = (DatabaseEntry) intent.getSerializableExtra("entry");
        _isNew = intent.getBooleanExtra("isNew", false);
        _groups = new TreeSet<>(Collator.getInstance());
        _groups.addAll(intent.getStringArrayListExtra("groups"));
        if (_isNew) {
            setTitle(R.string.add_new_profile);
        }

        // set up fields
        _iconView = findViewById(R.id.profile_drawable);
        _kropView = findViewById(R.id.krop_view);
        _saveImageButton = findViewById(R.id.iv_saveImage);
        _textName = findViewById(R.id.text_name);
        _textIssuer = findViewById(R.id.text_issuer);
        _textPeriod = findViewById(R.id.text_period);
        _textDigits = findViewById(R.id.text_digits);
        _rowPeriod = findViewById(R.id.row_period);
        _textCounter = findViewById(R.id.text_counter);
        _rowCounter = findViewById(R.id.row_counter);
        _textSecret = findViewById(R.id.text_secret);
        _spinnerType = findViewById(R.id.spinner_type);
        SpinnerHelper.fillSpinner(this, _spinnerType, R.array.otp_types_array);
        _spinnerAlgo = findViewById(R.id.spinner_algo);
        SpinnerHelper.fillSpinner(this, _spinnerAlgo, R.array.otp_algo_array);
        _spinnerGroup = findViewById(R.id.spinner_group);
        updateGroupSpinnerList();
        SpinnerHelper.fillSpinner(this, _spinnerGroup, _spinnerGroupList);

        _advancedSettingsHeader = findViewById(R.id.accordian_header);
        _advancedSettings = findViewById(R.id.expandableLayout);

        // fill the fields with values if possible
        if (_origEntry != null) {
            if (_origEntry.hasIcon()) {
                byte[] imageBytes = _origEntry.getIcon();
                Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                _iconView.setImageBitmap(bitmap);
                _hasCustomIcon = true;
            } else {
                TextDrawable drawable = TextDrawableHelper.generate(_origEntry.getIssuer(), _origEntry.getName(), _iconView);
                _iconView.setImageDrawable(drawable);
            }

            _textName.setText(_origEntry.getName());
            _textIssuer.setText(_origEntry.getIssuer());

            OtpInfo info = _origEntry.getInfo();
            if (info instanceof TotpInfo) {
                _textPeriod.setText(Integer.toString(((TotpInfo) info).getPeriod()));
                _rowPeriod.setVisibility(View.VISIBLE);
            } else if (info instanceof HotpInfo) {
                _textCounter.setText(Long.toString(((HotpInfo) info).getCounter()));
                _rowCounter.setVisibility(View.VISIBLE);
            } else {
                throw new RuntimeException();
            }
            _textDigits.setText(Integer.toString(info.getDigits()));

            byte[] secretBytes = _origEntry.getInfo().getSecret();
            if (secretBytes != null) {
                char[] secretChars = Base32.encode(secretBytes);
                _textSecret.setText(secretChars, 0, secretChars.length);
            }

            String type = _origEntry.getInfo().getType();
            _spinnerType.setSelection(getStringResourceIndex(R.array.otp_types_array, type), false);

            String algo = _origEntry.getInfo().getAlgorithm(false);
            _spinnerAlgo.setSelection(getStringResourceIndex(R.array.otp_algo_array, algo), false);

            String group = _origEntry.getGroup();
            if (group != null) {
                int pos = _groups.contains(group) ? _groups.headSet(group).size() : -1;
                _spinnerGroup.setSelection(pos + 1, false);
            }
        }

        // update the icon if the text changed
        _textIssuer.addTextChangedListener(_iconChangeListener);
        _textName.addTextChangedListener(_iconChangeListener);

        // show/hide period and counter fields on type change
        _spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String type = _spinnerType.getSelectedItem().toString();

                switch (type.toLowerCase()) {
                    case TotpInfo.ID:
                    case SteamInfo.ID:
                        _rowCounter.setVisibility(View.GONE);
                        _rowPeriod.setVisibility(View.VISIBLE);
                        break;
                    case HotpInfo.ID:
                        _rowPeriod.setVisibility(View.GONE);
                        _rowCounter.setVisibility(View.VISIBLE);
                        break;
                    default:
                        throw new RuntimeException();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        final Activity activity = this;
        _spinnerGroup.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private int prevPosition;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == _spinnerGroupList.size() - 1) {
                    Dialogs.showCreateNewGroupDialog(activity, text -> {
                        if (text.isEmpty()) {
                            return;
                        }
                        _groups.add(text);
                        // reset the selection to "No group" to work around a quirk
                        _spinnerGroup.setSelection(0, false);
                        updateGroupSpinnerList();
                        _spinnerGroup.setSelection(_spinnerGroupList.indexOf(text), false);
                    });
                    _spinnerGroup.setSelection(prevPosition, false);
                } else {
                    prevPosition = position;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        _iconView.setOnClickListener(v -> {
            Intent galleryIntent = new Intent(Intent.ACTION_PICK);
            galleryIntent.setDataAndType(android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI, "image/*");

            Intent chooserIntent = Intent.createChooser(galleryIntent, "Select photo");
            startActivityForResult(Intent.createChooser(chooserIntent, "Select photo"), PICK_IMAGE_REQUEST);
        });

        _advancedSettingsHeader.setOnClickListener(v -> openAdvancedSettings());

        // automatically open advanced settings since 'Secret' is required.
        if (_isNew) {
            openAdvancedSettings();
        }
    }

    private void openAdvancedSettings() {
        Animation fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setInterpolator(new AccelerateInterpolator());
        fadeOut.setDuration(220);
        _advancedSettingsHeader.startAnimation(fadeOut);

        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setInterpolator(new AccelerateInterpolator());
        fadeIn.setDuration(250);

        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                _advancedSettingsHeader.setVisibility(View.GONE);
                _advancedSettings.startAnimation(fadeIn);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        fadeIn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                _advancedSettings.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    private void updateGroupSpinnerList() {
        Resources res = getResources();
        _spinnerGroupList.clear();
        _spinnerGroupList.add(res.getString(R.string.no_group));
        _spinnerGroupList.addAll(_groups);
        _spinnerGroupList.add(res.getString(R.string.new_group));
    }

    @Override
    public void onBackPressed() {
        AtomicReference<String> msg = new AtomicReference<>();
        AtomicReference<DatabaseEntry> entry = new AtomicReference<>();

        try {
            entry.set(parseEntry());
        } catch (ParseException e) {
            msg.set(e.getMessage());
        }

        // close the activity if the entry has not been changed
        if (_origEntry != null && !_hasChangedIcon && _origEntry.equals(entry.get())) {
            super.onBackPressed();
            return;
        }

        // ask for confirmation if the entry has been changed
        Dialogs.showDiscardDialog(this,
                (dialog, which) -> {
                    // if the entry couldn't be parsed, we show an error dialog
                    if (msg.get() != null) {
                        onSaveError(msg.get());
                        return;
                    }

                    finish(entry.get(), false);
                },
                (dialog, which) -> super.onBackPressed()
        );
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.action_save:
                onSave();
                break;
            case R.id.action_delete:
                Dialogs.showDeleteEntryDialog(this, (dialog, which) -> {
                    finish(_origEntry, true);
                });
                break;
            case R.id.action_default_icon:
                TextDrawable drawable = TextDrawableHelper.generate(_origEntry.getIssuer(), _origEntry.getName(), _iconView);
                _iconView.setImageDrawable(drawable);
                _hasCustomIcon = false;
                _hasChangedIcon = true;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
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

    private void finish(DatabaseEntry entry, boolean delete) {
        Intent intent = new Intent();
        intent.putExtra("entry", entry);
        intent.putExtra("delete", delete);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, final int resultCode, Intent data) {
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri inputFile = (data.getData());
            InputStream inputStream;
            Bitmap bitmap;
            try {
                inputStream = getContentResolver().openInputStream(inputFile);
                BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                bitmap = BitmapFactory.decodeStream(inputStream, null, bmOptions);
                _kropView.setBitmap(bitmap);
                _kropView.setVisibility(View.VISIBLE);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }


            _saveImageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    _iconView.setImageBitmap(_kropView.getCroppedBitmap());
                    _kropView.setVisibility(View.GONE);
                    _hasCustomIcon = true;
                    _hasChangedIcon = true;
                }
            });
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private int parsePeriod() throws ParseException {
        try {
            return Integer.parseInt(_textPeriod.getText().toString());
        } catch (NumberFormatException e) {
            throw new ParseException("Period is not an integer.");
        }
    }

    private DatabaseEntry parseEntry() throws ParseException {
        if (_textSecret.length() == 0) {
            throw new ParseException("Secret is a required field.");
        }

        String type = _spinnerType.getSelectedItem().toString();
        String algo = _spinnerAlgo.getSelectedItem().toString();

        int digits;
        try {
            digits = Integer.parseInt(_textDigits.getText().toString());
        } catch (NumberFormatException e) {
            throw new ParseException("Digits is not an integer.");
        }

        byte[] secret;
        try {
            secret = Base32.decode(EditTextHelper.getEditTextChars(_textSecret));
        } catch (Base32Exception e) {
            throw new ParseException("Secret is not valid base32.");
        }

        // set otp info
        OtpInfo info;
        try {
            switch (type.toLowerCase()) {
                case TotpInfo.ID:
                    info = new TotpInfo(secret, algo, digits, parsePeriod());
                    break;
                case SteamInfo.ID:
                    info = new SteamInfo(secret, algo, digits, parsePeriod());
                    break;
                case HotpInfo.ID:
                    long counter;
                    try {
                        counter = Long.parseLong(_textCounter.getText().toString());
                    } catch (NumberFormatException e) {
                        throw new ParseException("Counter is not an integer.");
                    }
                    info = new HotpInfo(secret, algo, digits, counter);
                    break;
                default:
                    throw new RuntimeException();
            }

            info.setDigits(digits);
            info.setAlgorithm(algo);
        } catch (OtpInfoException e) {
            throw new ParseException("The entered info is incorrect: " + e.getMessage());
        }

        // set database entry info
        DatabaseEntry entry;
        if (_origEntry == null) {
            entry = new DatabaseEntry(info);
        } else {
            entry = cloneEntry(_origEntry);
            entry.setInfo(info);
        }
        entry.setIssuer(_textIssuer.getText().toString());
        entry.setName(_textName.getText().toString());

        int groupPos = _spinnerGroup.getSelectedItemPosition();
        if (groupPos != 0) {
            String group = _spinnerGroupList.get(_spinnerGroup.getSelectedItemPosition());
            entry.setGroup(group);
        } else {
            entry.setGroup(null);
        }

        if (_hasChangedIcon) {
            if (_hasCustomIcon) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                drawableToBitmap(_iconView.getDrawable()).compress(Bitmap.CompressFormat.JPEG, 100, stream);
                byte[] data = stream.toByteArray();
                entry.setIcon(data);
            } else {
                entry.setIcon(null);
            }
        }

        return entry;
    }

    private void onSaveError(String msg) {
        Dialogs.showSecureDialog(new AlertDialog.Builder(this)
                .setTitle(getString(R.string.saving_profile_error))
                .setMessage(msg)
                .setPositiveButton(android.R.string.ok, null)
                .create());
    }

    private boolean onSave() {
        DatabaseEntry entry;
        try {
            entry = parseEntry();
        } catch (ParseException e) {
            onSaveError(e.getMessage());
            return false;
        }

        finish(entry, false);
        return true;
    }

    private TextWatcher _iconChangeListener = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (!_hasCustomIcon) {
                TextDrawable drawable = TextDrawableHelper.generate(_textIssuer.getText().toString(), _textName.getText().toString(), _iconView);
                _iconView.setImageDrawable(drawable);
            }
        }
    };

    private int getStringResourceIndex(@ArrayRes int id, String string) {
        String[] res = getResources().getStringArray(id);
        for (int i = 0; i < res.length; i++) {
            if (res[i].equalsIgnoreCase(string)) {
                return i;
            }
        }
        return -1;
    }

    private static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        final int width = !drawable.getBounds().isEmpty() ? drawable
                .getBounds().width() : drawable.getIntrinsicWidth();

        final int height = !drawable.getBounds().isEmpty() ? drawable
                .getBounds().height() : drawable.getIntrinsicHeight();

        final Bitmap bitmap = Bitmap.createBitmap(width <= 0 ? 1 : width,
                height <= 0 ? 1 : height, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    private static DatabaseEntry cloneEntry(DatabaseEntry entry) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(entry);

            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            return (DatabaseEntry) ois.readObject();
        } catch (ClassNotFoundException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class ParseException extends Exception {
        public ParseException(String message) {
            super(message);
        }
    }
}
