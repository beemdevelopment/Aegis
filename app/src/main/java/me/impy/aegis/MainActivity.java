package me.impy.aegis;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import me.impy.aegis.crypto.KeyInfo;
import me.impy.aegis.crypto.OTP;

public class MainActivity extends AppCompatActivity {

    static final int GET_KEYINFO = 1;
    Button btnScan;
    TextView tvTotp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnScan = (Button) findViewById(R.id.button);
        tvTotp = (TextView) findViewById(R.id.textView2);
        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent scannerActivity = new Intent(getApplicationContext(), ScannerActivity.class);
                startActivityForResult(scannerActivity, GET_KEYINFO);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == GET_KEYINFO) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                KeyInfo info = (KeyInfo)data.getSerializableExtra("Keyinfo");

                String otp;
                try {
                    otp = OTP.generateOTP(info);
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
                tvTotp.setText(otp);
            }
        }
    }
}
