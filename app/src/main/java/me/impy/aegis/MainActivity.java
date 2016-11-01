package me.impy.aegis;

import android.app.FragmentManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import me.impy.aegis.crypto.CryptoUtils;
import me.impy.aegis.crypto.OTP;
import me.impy.aegis.db.Database;
import me.impy.aegis.helpers.SimpleItemTouchHelperCallback;

public class MainActivity extends AppCompatActivity  {

    static final int GET_KEYINFO = 1;
    static final int ADD_KEYINFO = 2;

    RecyclerView rvKeyProfiles;
    KeyProfileAdapter mKeyProfileAdapter;
    ArrayList<KeyProfile> mKeyProfiles;
    Database database;

    boolean nightMode = false;
    int clickedItemPosition = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = this.getSharedPreferences("me.impy.aegis", Context.MODE_PRIVATE);
        if(!prefs.getBoolean("passedIntro", false))
        {
            Intent intro = new Intent(this, IntroActivity.class);
            startActivity(intro);
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if(sharedPreferences.getBoolean("pref_night_mode", false))
        {
            nightMode = true;
            setTheme(R.style.AppTheme_Dark_NoActionBar);
        } else
        {
            setPreferredTheme();
        }

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            Intent scannerActivity = new Intent(getApplicationContext(), ScannerActivity.class);
            startActivityForResult(scannerActivity, GET_KEYINFO);
        });

        char[] password = "test".toCharArray();
        database = Database.createInstance(getApplicationContext(), "keys.db", password);
        CryptoUtils.zero(password);

        mKeyProfiles = new ArrayList<>();

        rvKeyProfiles = (RecyclerView) findViewById(R.id.rvKeyProfiles);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        rvKeyProfiles.setLayoutManager(mLayoutManager);

        final Context context = this.getApplicationContext();
        //EditProfileBottomSheetdialog bottomSheetDialog = EditProfileBottomSheetdialog.getInstance();

        mKeyProfileAdapter = new KeyProfileAdapter(mKeyProfiles);
        mKeyProfileAdapter.setOnItemClickListener((position, v) -> {
            clickedItemPosition = position;
            InitializeBottomSheet().show();
        });

        //View dialogView = bottomSheetDialog.getView();
        //LinearLayout copyLayout = (LinearLayout)dialogView.findViewById(R.id.copy_button);
        /*copyLayout.setOnClickListener(view -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("text/plain", mKeyProfiles.get(clickedItemPosition).Code);
            clipboard.setPrimaryClip(clip);

            Toast.makeText(context, "Code successfully copied to the clipboard", Toast.LENGTH_SHORT).show();
        });*/

        mKeyProfileAdapter.setOnLongItemClickListener((position, v) -> {


        });

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mKeyProfileAdapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(rvKeyProfiles);

        rvKeyProfiles.setAdapter(mKeyProfileAdapter);
        Comparator<KeyProfile> comparator = new Comparator<KeyProfile>() {
            @Override
            public int compare(KeyProfile keyProfile, KeyProfile t1) {
                return keyProfile.Order - t1.Order;
            }
        };
        Collections.sort(mKeyProfiles, comparator);
        
        try {
            mKeyProfiles.addAll(database.getKeys());
            mKeyProfileAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == GET_KEYINFO) {
            if (resultCode == RESULT_OK) {
                final KeyProfile keyProfile = (KeyProfile)data.getSerializableExtra("KeyProfile");

                Intent intent = new Intent(this, AddProfileActivity.class);
                intent.putExtra("KeyProfile", keyProfile);
                startActivityForResult(intent, ADD_KEYINFO);
            }
        }
        else if (requestCode == ADD_KEYINFO) {
            if (resultCode == RESULT_OK) {
                final KeyProfile keyProfile = (KeyProfile)data.getSerializableExtra("KeyProfile");

                String otp;
                try {
                    otp = OTP.generateOTP(keyProfile.Info);
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }

                keyProfile.Order = mKeyProfiles.size() + 1;
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
    protected void onResume() {
        super.onResume();
        mKeyProfileAdapter.notifyDataSetChanged();
        setPreferredTheme();
    }

    @Override
    protected void onPause() {
        for(int i = 0; i < mKeyProfiles.size(); i++)
        {
            try {
                database.updateKey(mKeyProfiles.get(i));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        super.onPause();
    }

    private BottomSheetDialog InitializeBottomSheet()
    {
        View bottomSheetView = getLayoutInflater ().inflate (R.layout.bottom_sheet_edit_profile, null);
        LinearLayout copyLayout = (LinearLayout)bottomSheetView.findViewById(R.id.copy_button);
        LinearLayout deleteLayout = (LinearLayout)bottomSheetView.findViewById(R.id.delete_button);
        LinearLayout editLayout = (LinearLayout)bottomSheetView.findViewById(R.id.edit_button);
        bottomSheetView.findViewById(R.id.edit_button);
        BottomSheetDialog bottomDialog =  new BottomSheetDialog(this);
        bottomDialog.setContentView(bottomSheetView);
        bottomDialog.setCancelable (true);
        bottomDialog.getWindow ().setLayout (LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        bottomDialog.show();

        copyLayout.setOnClickListener(view -> {
            bottomDialog.dismiss();
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("text/plain", mKeyProfiles.get(clickedItemPosition).Code);
            clipboard.setPrimaryClip(clip);

            Toast.makeText(this.getApplicationContext(), "Code successfully copied to the clipboard", Toast.LENGTH_SHORT).show();
        });

        deleteLayout.setOnClickListener(view -> {
            bottomDialog.dismiss();

            KeyProfile keyProfile = mKeyProfiles.get(clickedItemPosition);
            deleteProfile(keyProfile);
        });

        editLayout.setOnClickListener(view -> {
            bottomDialog.dismiss();
            Toast.makeText(this.getApplicationContext(), "Coming soon", Toast.LENGTH_SHORT).show();
        });

        return bottomDialog;
    }

    private void deleteProfile(KeyProfile profile)
    {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Delete entry")
                .setMessage("Are you sure you want to delete this profile?")
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    database.removeKey(profile);
                    mKeyProfiles.remove(clickedItemPosition);
                    mKeyProfileAdapter.notifyItemRemoved(clickedItemPosition);
                })
                .setNegativeButton(android.R.string.no, (dialog, which) -> {

                })
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {

            Intent preferencesActivity = new Intent(this, PreferencesActivity.class);
            startActivity(preferencesActivity);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setPreferredTheme()
    {
        boolean restart = false;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if(sharedPreferences.getBoolean("pref_night_mode", false)) {
            if(!nightMode) {
                setTheme(R.style.AppTheme_Dark_NoActionBar);
                restart = true;
            }
        } else {
            if(nightMode) {
                setTheme(R.style.AppTheme_Default_NoActionBar);
                restart = true;
            }
        }

        if(restart){
            finish();
            startActivity(new Intent(this, this.getClass()));
        }
    }
}
