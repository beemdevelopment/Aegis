package me.impy.aegis.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.ArrayRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
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
import com.esafirm.imagepicker.features.ImagePicker;
import com.esafirm.imagepicker.features.ReturnMode;
import com.esafirm.imagepicker.model.Image;

import java.io.ByteArrayOutputStream;

import de.hdodenhof.circleimageview.CircleImageView;
import me.impy.aegis.R;
import me.impy.aegis.db.DatabaseEntry;
import me.impy.aegis.encoding.Base32;
import me.impy.aegis.encoding.Base32Exception;
import me.impy.aegis.helpers.EditTextHelper;
import me.impy.aegis.helpers.SpinnerHelper;
import me.impy.aegis.helpers.TextDrawableHelper;
import me.impy.aegis.otp.HotpInfo;
import me.impy.aegis.otp.OtpInfo;
import me.impy.aegis.otp.OtpInfoException;
import me.impy.aegis.otp.TotpInfo;
import me.impy.aegis.ui.dialogs.Dialogs;

public class EditEntryActivity extends AegisActivity {
    private boolean _isNew = false;
    private boolean _edited = false;
    private DatabaseEntry _entry;
    private boolean _hasCustomImage = false;
    private CircleImageView _iconView;
    private ImageView _saveImageButton;

    private EditText _textName;
    private EditText _textIssuer;
    private EditText _textPeriod;
    private EditText _textCounter;
    private EditText _textSecret;

    private TableRow _rowPeriod;
    private TableRow _rowCounter;

    private Spinner _spinnerType;
    private Spinner _spinnerAlgo;
    private Spinner _spinnerDigits;
    private SpinnerItemSelectedListener _selectedListener;

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
        _entry = (DatabaseEntry) intent.getSerializableExtra("entry");
        _isNew = intent.getBooleanExtra("isNew", false);
        if (_isNew) {
            setTitle("Add profile");
        }

        // set up fields
        _iconView = findViewById(R.id.profile_drawable);
        _kropView = findViewById(R.id.krop_view);
        _saveImageButton = findViewById(R.id.iv_saveImage);
        _textName = findViewById(R.id.text_name);
        _textIssuer = findViewById(R.id.text_issuer);
        _textPeriod = findViewById(R.id.text_period);
        _rowPeriod = findViewById(R.id.row_period);
        _textCounter = findViewById(R.id.text_counter);
        _rowCounter = findViewById(R.id.row_counter);
        _textSecret = findViewById(R.id.text_secret);
        _spinnerType = findViewById(R.id.spinner_type);
        SpinnerHelper.fillSpinner(this, _spinnerType, R.array.otp_types_array);
        _spinnerAlgo = findViewById(R.id.spinner_algo);
        SpinnerHelper.fillSpinner(this, _spinnerAlgo, R.array.otp_algo_array);
        _spinnerDigits = findViewById(R.id.spinner_digits);
        SpinnerHelper.fillSpinner(this, _spinnerDigits, R.array.otp_digits_array);
        _selectedListener = new SpinnerItemSelectedListener();

        _advancedSettingsHeader = findViewById(R.id.accordian_header);
        _advancedSettings = findViewById(R.id.expandableLayout);

        // fill the fields with values if possible
        if (_entry != null) {
            if (_entry.hasIcon()) {
                byte[] imageBytes = _entry.getIcon();
                Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                _iconView.setImageBitmap(bitmap);
                _hasCustomImage = true;
            } else {
                TextDrawable drawable = TextDrawableHelper.generate(_entry.getIssuer(), _entry.getName(), _iconView);
                _iconView.setImageDrawable(drawable);
            }

            _textName.setText(_entry.getName());
            _textIssuer.setText(_entry.getIssuer());

            OtpInfo info = _entry.getInfo();
            if (info instanceof TotpInfo) {
                _textPeriod.setText(Integer.toString(((TotpInfo) info).getPeriod()));
                _rowPeriod.setVisibility(View.VISIBLE);
            } else if (info instanceof HotpInfo) {
                _textCounter.setText(Long.toString(((HotpInfo) info).getCounter()));
                _rowCounter.setVisibility(View.VISIBLE);
            } else {
                throw new RuntimeException();
            }

            byte[] secretBytes = _entry.getInfo().getSecret();
            if (secretBytes != null) {
                char[] secretChars = Base32.encode(secretBytes);
                _textSecret.setText(secretChars, 0, secretChars.length);
            }

            String type = _entry.getInfo().getType();
            _spinnerType.setSelection(getStringResourceIndex(R.array.otp_types_array, type.toUpperCase()), false);

            String algo = _entry.getInfo().getAlgorithm(false);
            _spinnerAlgo.setSelection(getStringResourceIndex(R.array.otp_algo_array, algo), false);

            String digits = Integer.toString(_entry.getInfo().getDigits());
            _spinnerDigits.setSelection(getStringResourceIndex(R.array.otp_digits_array, digits), false);
        }

        // listen for changes to any of the fields
        _textName.addTextChangedListener(_textListener);
        _textIssuer.addTextChangedListener(_textListener);
        _textPeriod.addTextChangedListener(_textListener);
        _textCounter.addTextChangedListener(_textListener);
        _textSecret.addTextChangedListener(_textListener);
        _spinnerAlgo.setOnTouchListener(_selectedListener);
        _spinnerAlgo.setOnItemSelectedListener(_selectedListener);
        _spinnerDigits.setOnTouchListener(_selectedListener);
        _spinnerDigits.setOnItemSelectedListener(_selectedListener);

        // update the icon if the text changed
        _textIssuer.addTextChangedListener(_iconChangeListener);
        _textName.addTextChangedListener(_iconChangeListener);

        // show/hide period and counter fields on type change
        _spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String type = _spinnerType.getSelectedItem().toString();

                switch (type.toLowerCase()) {
                    case "totp":
                        _rowCounter.setVisibility(View.GONE);
                        _rowPeriod.setVisibility(View.VISIBLE);
                        break;
                    case "hotp":
                        _rowPeriod.setVisibility(View.GONE);
                        _rowCounter.setVisibility(View.VISIBLE);
                        break;
                    default:
                        throw new RuntimeException();
                }

                _selectedListener.onItemSelected(parent, view, position, id);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        ImagePicker imagePicker = ImagePicker.create(this)
                .returnMode(ReturnMode.ALL) // set whether pick and / or camera action should return immediate result or not.
                .folderMode(true) // folder mode (false by default)
                .toolbarFolderTitle("Folder") // folder selection title
                .toolbarImageTitle("Tap to select") // image selection title
                .toolbarArrowColor(Color.BLACK) // Toolbar 'up' arrow color
                .single() // single mode
                .showCamera(false) // show camera or not (true by default)
                .imageDirectory("Camera");

        // Open ImagePicker when clicking on the icon
        _iconView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imagePicker.start(); // start image picker activity with request code
            }
        });

        _advancedSettingsHeader.setOnClickListener(v -> {
            openAdvancedSettings();
        });

        // automatically open advanced settings since 'Secret' is required.
        if(_isNew){
            openAdvancedSettings();
        }
    }

    @Override
    protected void setPreferredTheme(boolean darkMode) {
        if (darkMode) {
            setTheme(R.style.AppTheme_Dark);
        } else {
            setTheme(R.style.AppTheme);
        }
    }

    private void openAdvancedSettings() {
        Animation fadeOut = new AlphaAnimation(1, 0);  // the 1, 0 here notifies that we want the opacity to go from opaque (1) to transparent (0)
        fadeOut.setInterpolator(new AccelerateInterpolator());
        fadeOut.setDuration(220); // Fadeout duration should be 1000 milli seconds
        _advancedSettingsHeader.startAnimation(fadeOut);

        Animation fadeIn = new AlphaAnimation(0, 1);  // the 1, 0 here notifies that we want the opacity to go from opaque (1) to transparent (0)
        fadeIn.setInterpolator(new AccelerateInterpolator());
        fadeIn.setDuration(250); // Fadeout duration should be 1000 milli seconds

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

    @Override
    public void onBackPressed() {
        if (!_edited) {
            super.onBackPressed();
            return;
        }

        Dialogs.showDiscardDialog(this,
                (dialog, which) -> onSave(),
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
                    finish(true);
                });
                break;
            case R.id.action_default_icon:
                TextDrawable drawable = TextDrawableHelper.generate(_entry.getIssuer(), _entry.getName(), _iconView);
                _iconView.setImageDrawable(drawable);
                _hasCustomImage = false;
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
        if (!_hasCustomImage) {
            menu.findItem(R.id.action_default_icon).setVisible(false);
        }

        return true;
    }

    private void finish(boolean delete) {
        Intent intent = new Intent();
        intent.putExtra("entry", _entry);
        intent.putExtra("delete", delete);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, final int resultCode, Intent data) {
        if (ImagePicker.shouldHandle(requestCode, resultCode, data)) {
            // or get a single image only
            Image image = ImagePicker.getFirstImageOrNull(data);
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            Bitmap bitmap = BitmapFactory.decodeFile(image.getPath(),bmOptions);
            _kropView.setBitmap(bitmap);
            _kropView.setVisibility(View.VISIBLE);

            _saveImageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    _iconView.setImageBitmap(_kropView.getCroppedBitmap());
                    _kropView.setVisibility(View.GONE);
                    _hasCustomImage = true;
                }
            });
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private boolean onSave() {
        if (_textSecret.length() == 0) {
            onError("Secret is a required field.");
            return false;
        }

        String type = _spinnerType.getSelectedItem().toString();
        String algo = _spinnerAlgo.getSelectedItem().toString();

        int digits;
        try {
            digits = Integer.parseInt(_spinnerDigits.getSelectedItem().toString());
        } catch (NumberFormatException e) {
            onError("Digits is not an integer.");
            return false;
        }

        byte[] secret;
        try {
            secret = Base32.decode(EditTextHelper.getEditTextChars(_textSecret));
        } catch (Base32Exception e) {
            onError("Secret is not valid base32.");
            return false;
        }

        // set otp info
        OtpInfo info;
        try {
            switch (type.toLowerCase()) {
                case "totp":
                    int period;
                    try {
                        period = Integer.parseInt(_textPeriod.getText().toString());
                    } catch (NumberFormatException e) {
                        onError("Period is not an integer.");
                        return false;
                    }
                    info = new TotpInfo(secret, algo, digits, period);
                    break;
                case "hotp":
                    long counter;
                    try {
                        counter = Long.parseLong(_textCounter.getText().toString());
                    } catch (NumberFormatException e) {
                        onError("Counter is not an integer.");
                        return false;
                    }
                    info = new HotpInfo(secret, algo, digits, counter);
                    break;
                default:
                    throw new RuntimeException();
            }

            info.setDigits(digits);
            info.setAlgorithm(algo);
        } catch (OtpInfoException e) {
            onError("The entered info is incorrect: " + e.getMessage());
            return false;
        }

        // set database entry info
        DatabaseEntry entry = _entry;
        if (entry == null) {
            entry = new DatabaseEntry(info);
        } else {
            entry.setInfo(info);
        }
        entry.setIssuer(_textIssuer.getText().toString());
        entry.setName(_textName.getText().toString());

        if (_hasCustomImage) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            drawableToBitmap(_iconView.getDrawable()).compress(Bitmap.CompressFormat.JPEG, 100, stream);
            byte[] bitmapdata = stream.toByteArray();
            entry.setIcon(bitmapdata);
        } else {
            entry.setIcon(null);
        }

        _entry = entry;
        finish(false);
        return true;
    }

    private void onError(String msg) {
        new AlertDialog.Builder(this)
                .setTitle("Error saving profile")
                .setMessage(msg)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void onFieldEdited() {
        _edited = true;
    }

    private TextWatcher _textListener = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            onFieldEdited();
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            onFieldEdited();
        }

        @Override
        public void afterTextChanged(Editable s) {
            onFieldEdited();
        }
    };

    private TextWatcher _iconChangeListener = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (!_hasCustomImage) {
                TextDrawable drawable = TextDrawableHelper.generate(_textIssuer.getText().toString(), _textName.getText().toString(), _iconView);
                _iconView.setImageDrawable(drawable);
            }
        }
    };

    private class SpinnerItemSelectedListener implements AdapterView.OnItemSelectedListener, View.OnTouchListener {
        private boolean _userSelect = false;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            _userSelect = true;
            return false;
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (_userSelect) {
                onFieldEdited();
                _userSelect = false;
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }

    private int getStringResourceIndex(@ArrayRes int id, String string) {
        String[] res = getResources().getStringArray(id);
        for (int i = 0; i < res.length; i++) {
            if (res[i].equals(string)) {
                return i;
            }
        }
        return -1;
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
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
}
