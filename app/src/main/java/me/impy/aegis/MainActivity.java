package me.impy.aegis;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.yarolegovich.lovelydialog.LovelyTextInputDialog;

import java.util.ArrayList;

import github.nisrulz.recyclerviewhelper.RVHItemClickListener;
import github.nisrulz.recyclerviewhelper.RVHItemDividerDecoration;
import github.nisrulz.recyclerviewhelper.RVHItemTouchHelperCallback;
import me.impy.aegis.crypto.CryptoUtils;
import me.impy.aegis.crypto.OTP;
import me.impy.aegis.db.Database;

public class MainActivity extends AppCompatActivity  {

    static final int GET_KEYINFO = 1;
    static final int ADD_KEYINFO = 2;

    RecyclerView rvKeyProfiles;
    KeyProfileAdapter mKeyProfileAdapter;
    ArrayList<KeyProfile> mKeyProfiles;
    Database database;
    
    int count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = this.getSharedPreferences("me.impy.aegis", Context.MODE_PRIVATE);
        if(!prefs.getBoolean("passedIntro", false))
        {
            Intent intro = new Intent(this, IntroActivity.class);
            startActivity(intro);
        }

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent scannerActivity = new Intent(getApplicationContext(), ScannerActivity.class);
                startActivityForResult(scannerActivity, GET_KEYINFO);
            }
        });

        // demo
        char[] password = "test".toCharArray();
        database = Database.createInstance(getApplicationContext(), "keys.db", password);
        CryptoUtils.zero(password);

        mKeyProfiles = new ArrayList<>();

        rvKeyProfiles = (RecyclerView) findViewById(R.id.rvKeyProfiles);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        rvKeyProfiles.setLayoutManager(mLayoutManager);

        final Context context = this.getApplicationContext();
        rvKeyProfiles.addOnItemTouchListener(new RVHItemClickListener(this, new RVHItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("text/plain", mKeyProfiles.get(position).Code);
                clipboard.setPrimaryClip(clip);

                Toast.makeText(context, "Code successfully copied to the clipboard", Toast.LENGTH_SHORT).show();
            }
        }));

        mKeyProfileAdapter = new KeyProfileAdapter(mKeyProfiles);
        rvKeyProfiles.addItemDecoration(new RVHItemDividerDecoration(this, LinearLayoutManager.VERTICAL));

        ItemTouchHelper.Callback callback = new RVHItemTouchHelperCallback(mKeyProfileAdapter, true, false, false);
        ItemTouchHelper helper = new ItemTouchHelper(callback);
        helper.attachToRecyclerView(rvKeyProfiles);

        rvKeyProfiles.setAdapter(mKeyProfileAdapter);

        try {
            for (KeyProfile profile : database.getKeys()) {
                mKeyProfiles.add(profile);
            }
            mKeyProfileAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == GET_KEYINFO) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                final KeyProfile keyProfile = (KeyProfile)data.getSerializableExtra("KeyProfile");

                Intent intent = new Intent(this, AddProfileActivity.class);
                intent.putExtra("KeyProfile", keyProfile);
                startActivityForResult(intent, ADD_KEYINFO);
                //TODO: do something with the result.
            }
        }
        else if (requestCode == ADD_KEYINFO) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                final KeyProfile keyProfile = (KeyProfile)data.getSerializableExtra("KeyProfile");

                String otp;
                try {
                    otp = OTP.generateOTP(keyProfile.Info);
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }

                keyProfile.Code = otp;
                mKeyProfiles.add(keyProfile);
                mKeyProfileAdapter.notifyDataSetChanged();

                try {
                    database.addKey(keyProfile);
                } catch (Exception e) {
                    e.printStackTrace();
                }
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
