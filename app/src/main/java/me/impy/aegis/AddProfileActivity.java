package me.impy.aegis;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import me.impy.aegis.crypto.otp.OTP;

public class AddProfileActivity extends AppCompatActivity {

    KeyProfile keyProfile;

    EditText profileName;
    TextView tvAlgorithm;
    TextView tvIssuer;
    TextView tvPeriod;
    TextView tvOtp;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setPreferredTheme();
        setContentView(R.layout.activity_add_profile);

        profileName = (EditText) findViewById(R.id.addProfileName);
        tvAlgorithm = (TextView) findViewById(R.id.tvAlgorithm);
        tvIssuer = (TextView) findViewById(R.id.tvIssuer);
        tvPeriod = (TextView) findViewById(R.id.tvPeriod);
        tvOtp = (TextView) findViewById(R.id.tvOtp);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        keyProfile = (KeyProfile)getIntent().getSerializableExtra("KeyProfile");

        initializeForm();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent resultIntent = new Intent();

                keyProfile.Name = profileName.getText().toString();
                resultIntent.putExtra("KeyProfile", keyProfile);

                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            }
        });
        //profileName.setText(keyProfile.Info.getAccountName());
    }

    private void initializeForm()
    {
        profileName.setText(keyProfile.Info.getAccountName());
        tvAlgorithm.setText(keyProfile.Info.getAlgorithm());
        tvIssuer.setText(keyProfile.Info.getIssuer());
        tvPeriod.setText(keyProfile.Info.getPeriod() + " seconds");

        String otp;
        try {
            otp = OTP.generateOTP(keyProfile.Info);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        keyProfile.Code = otp;
        tvOtp.setText(otp.substring(0, 3) + " " + otp.substring(3));
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


    private void setPreferredTheme()
    {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if(sharedPreferences.getBoolean("pref_night_mode", false))
        {
                setTheme(R.style.AppTheme_Dark_TransparentActionBar);
        } else
        {
                setTheme(R.style.AppTheme_Default_TransparentActionBar);
        }
    }
}
