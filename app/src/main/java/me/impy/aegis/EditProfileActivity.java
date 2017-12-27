package me.impy.aegis;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.ArrayRes;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;

import me.impy.aegis.crypto.KeyInfo;
import me.impy.aegis.db.DatabaseEntry;
import me.impy.aegis.encoding.Base32;
import me.impy.aegis.helpers.SpinnerHelper;

public class EditProfileActivity extends AegisActivity {
    private boolean _edited = false;
    private KeyProfile _profile;

    private EditText _textName;
    private EditText _textIssuer;
    private EditText _textPeriod;
    private EditText _textSecret;

    private Spinner _spinnerType;
    private Spinner _spinnerAlgo;
    private Spinner _spinnerDigits;
    private SpinnerItemSelectedListener _selectedListener = new SpinnerItemSelectedListener();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        _profile = (KeyProfile) getIntent().getSerializableExtra("KeyProfile");

        ActionBar bar = getSupportActionBar();
        bar.setHomeAsUpIndicator(R.drawable.ic_close);
        bar.setDisplayHomeAsUpEnabled(true);

        ImageView imageView = findViewById(R.id.profile_drawable);
        imageView.setImageDrawable(_profile.getDrawable());

        DatabaseEntry entry = _profile.getEntry();
        _textName = findViewById(R.id.text_name);
        _textName.setText(entry.getName());
        _textName.addTextChangedListener(watcher);

        _textIssuer = findViewById(R.id.text_issuer);
        _textIssuer.setText(entry.getInfo().getIssuer());
        _textIssuer.addTextChangedListener(watcher);

        _textPeriod = findViewById(R.id.text_period);
        _textPeriod.setText(Integer.toString(entry.getInfo().getPeriod()));
        _textPeriod.addTextChangedListener(watcher);

        _textSecret = findViewById(R.id.text_secret);
        _textSecret.setText(Base32.encodeOriginal(entry.getInfo().getSecret()));
        _textSecret.addTextChangedListener(watcher);

        String type = entry.getInfo().getType();
        _spinnerType = findViewById(R.id.spinner_type);
        SpinnerHelper.fillSpinner(this, _spinnerType, R.array.otp_types_array);
        _spinnerType.setSelection(getStringResourceIndex(R.array.otp_types_array, type), false);
        _spinnerType.setOnTouchListener(_selectedListener);
        _spinnerType.setOnItemSelectedListener(_selectedListener);

        String algo = entry.getInfo().getAlgorithm(false);
        _spinnerAlgo = findViewById(R.id.spinner_algo);
        SpinnerHelper.fillSpinner(this, _spinnerAlgo, R.array.otp_algo_array);
        _spinnerAlgo.setSelection(getStringResourceIndex(R.array.otp_algo_array, algo), false);
        _spinnerAlgo.setOnTouchListener(_selectedListener);
        _spinnerAlgo.setOnItemSelectedListener(_selectedListener);

        String digits = Integer.toString(entry.getInfo().getDigits());
        _spinnerDigits = findViewById(R.id.spinner_digits);
        SpinnerHelper.fillSpinner(this, _spinnerDigits, R.array.otp_digits_array);
        _spinnerDigits.setSelection(getStringResourceIndex(R.array.otp_digits_array, digits), false);
        _spinnerDigits.setOnTouchListener(_selectedListener);
        _spinnerDigits.setOnItemSelectedListener(_selectedListener);
    }

    @Override
    protected void setPreferredTheme(boolean nightMode) {
        if (nightMode) {
            setTheme(R.style.AppTheme_Dark_TransparentActionBar);
        } else {
            setTheme(R.style.AppTheme_Default_TransparentActionBar);
        }
    }

    @Override
    public void onBackPressed() {
        if (!_edited) {
            super.onBackPressed();
            return;
        }

        new AlertDialog.Builder(this)
                .setMessage("Your changes have not been saved")
                .setPositiveButton(R.string.save, (dialog, which) -> onSave())
                .setNegativeButton(R.string.discard, (dialog, which) -> super.onBackPressed())
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_save:
                return onSave();
            case R.id.action_delete:
                return onDelete();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edit, menu);
        return true;
    }

    private boolean onDelete() {
        return false;
    }

    private boolean onSave() {
        int period;
        try {
            period = Integer.parseInt(_textPeriod.getText().toString());
        } catch (NumberFormatException e) {
            onError("Period is not an integer.");
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

        DatabaseEntry entry = _profile.getEntry();
        entry.setName(_textName.getText().toString());
        KeyInfo info = entry.getInfo();
        info.setIssuer(_textIssuer.getText().toString());
        info.setSecret(Base32.decode(_textSecret.getText().toString()));
        info.setPeriod(period);
        info.setDigits(digits);
        info.setAlgorithm(algo);
        info.setType(type);

        Intent intent = new Intent();
        intent.putExtra("KeyProfile", _profile);
        setResult(RESULT_OK, intent);
        finish();
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

    private TextWatcher watcher = new TextWatcher() {
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
}
