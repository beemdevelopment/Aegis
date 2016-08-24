package me.impy.aegis;

import android.app.Activity;
import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.EditText;

public class AddProfileActivity extends AppCompatActivity {

    EditText profileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_profile);

        profileName = (EditText) findViewById(R.id.addProfileName);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        final KeyProfile keyProfile = (KeyProfile)getIntent().getSerializableExtra("KeyProfile");

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
