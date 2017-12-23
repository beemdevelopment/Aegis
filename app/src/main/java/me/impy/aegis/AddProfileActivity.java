package me.impy.aegis;

import android.app.Activity;
import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import me.impy.aegis.crypto.KeyInfo;

public class AddProfileActivity extends AppCompatActivity {
    private KeyProfile _keyProfile;

    private EditText _profileName;
    private TextView _textAlgorithm;
    private TextView _textIssuer;
    private TextView _textPeriod;
    private TextView _textOtp;

    private AegisApplication _app;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _app = (AegisApplication) getApplication();

        setPreferredTheme();
        setContentView(R.layout.activity_add_profile);

        _profileName = findViewById(R.id.addProfileName);
        _textAlgorithm = findViewById(R.id.tvAlgorithm);
        _textIssuer = findViewById(R.id.tvIssuer);
        _textPeriod = findViewById(R.id.tvPeriod);
        _textOtp = findViewById(R.id.tvOtp);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        _keyProfile = (KeyProfile) getIntent().getSerializableExtra("KeyProfile");

        initializeForm();

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent resultIntent = new Intent();

                _keyProfile.getEntry().setName(_profileName.getText().toString());
                resultIntent.putExtra("KeyProfile", _keyProfile);

                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            }
        });
        //_profileName.setText(_keyProfile.Info.getAccountName());
    }

    private void initializeForm() {
        KeyInfo info = _keyProfile.getEntry().getInfo();
        _profileName.setText(info.getAccountName());
        _textAlgorithm.setText(info.getAlgorithm());
        _textIssuer.setText(info.getIssuer());
        _textPeriod.setText(info.getPeriod() + " seconds");

        String otp = _keyProfile.refreshCode();
        _textOtp.setText(otp.substring(0, 3) + " " + otp.substring(3));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setPreferredTheme() {
        if (_app.getPreferences().getBoolean("pref_night_mode", false)) {
            setTheme(R.style.AppTheme_Dark_TransparentActionBar);
        } else {
            setTheme(R.style.AppTheme_Default_TransparentActionBar);
        }
    }
}
