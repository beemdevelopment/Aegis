package me.impy.aegis;

import android.app.Activity;
import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import me.impy.aegis.crypto.KeyInfo;

public class AddProfileActivity extends AegisActivity {
    private KeyProfile _keyProfile;

    private EditText _profileName;
    private TextView _textAlgorithm;
    private TextView _textIssuer;
    private TextView _textPeriod;
    private TextView _textOtp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

    @Override
    protected void setPreferredTheme(boolean nightMode) {
        if (nightMode) {
            setTheme(R.style.AppTheme_Dark_TransparentActionBar);
        } else {
            setTheme(R.style.AppTheme_Default_TransparentActionBar);
        }
    }

    private void initializeForm() {
        KeyInfo info = _keyProfile.getEntry().getInfo();
        _profileName.setText(info.getAccountName());
        _textAlgorithm.setText(info.getAlgorithm(false));
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
}
