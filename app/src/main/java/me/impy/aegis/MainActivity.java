package me.impy.aegis;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.Keyboard;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.yarolegovich.lovelydialog.LovelyCustomDialog;
import com.yarolegovich.lovelydialog.LovelyTextInputDialog;

import java.util.ArrayList;

import me.impy.aegis.crypto.KeyInfo;
import me.impy.aegis.crypto.OTP;
import me.impy.aegis.helpers.DividerItemDecoration;
import me.impy.aegis.helpers.ItemClickListener;

public class MainActivity extends AppCompatActivity  {

    static final int GET_KEYINFO = 1;
    RecyclerView rvKeyProfiles;
    KeyProfileAdapter mKeyProfileAdapter;
    ArrayList<KeyProfile> mKeyProfiles;
    
    int count = 0;

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

        mKeyProfiles = new ArrayList<>();

        rvKeyProfiles = (RecyclerView) findViewById(R.id.rvKeyProfiles);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        rvKeyProfiles.setLayoutManager(mLayoutManager);

        rvKeyProfiles.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));

        final Context context = this.getApplicationContext();
        ItemClickListener itemClickListener = new ItemClickListener() {
            @Override
            public void onItemClicked(Object item, Object view) {
                Toast.makeText(context, ((KeyProfile)item).Code, Toast.LENGTH_SHORT).show();
            }
        };

        mKeyProfileAdapter = new KeyProfileAdapter(mKeyProfiles, itemClickListener);
        rvKeyProfiles.setAdapter(mKeyProfileAdapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == GET_KEYINFO) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                final KeyProfile keyProfile = (KeyProfile)data.getSerializableExtra("KeyProfile");

                String otp;
                try {
                    otp = OTP.generateOTP(keyProfile.KeyInfo);
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }

                keyProfile.Code = otp;

                new LovelyTextInputDialog(this, R.style.EditTextTintTheme)
                        .setTopColorRes(R.color.colorHeaderSuccess)
                        .setTitle("New profile added")
                        .setMessage("How do you want to call it?")
                        .setIcon(R.drawable.ic_check)
                        .setInitialInput(keyProfile.Name)
                        .setInputFilter("Nah, not possible man.", new LovelyTextInputDialog.TextFilter() {
                            @Override
                            public boolean check(String text) {
                                return true;
                                //return text.matches("\\w+");
                            }
                        })
                        .setConfirmButton(android.R.string.ok, new LovelyTextInputDialog.OnTextInputConfirmListener() {
                            @Override
                            public void onTextInputConfirmed(String text) {
                                keyProfile.Name = text;
                                mKeyProfiles.add(keyProfile);
                                mKeyProfileAdapter.notifyDataSetChanged();
                            }
                        })
                        .show();

                //TODO: do something with the result.
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
