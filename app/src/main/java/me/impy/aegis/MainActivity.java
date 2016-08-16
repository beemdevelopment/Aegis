package me.impy.aegis;

import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import me.impy.aegis.crypto.KeyInfo;
import me.impy.aegis.crypto.OTP;

public class MainActivity extends AppCompatActivity {

    static final int GET_KEYINFO = 1;
    TextView tvTotp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent scannerActivity = new Intent(getApplicationContext(), ScannerActivity.class);
                startActivityForResult(scannerActivity, GET_KEYINFO);
            }
        });

        tvTotp = (TextView) findViewById(R.id.textView2);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
