package me.impy.aegis;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import agency.tango.materialintroscreen.MaterialIntroActivity;
import agency.tango.materialintroscreen.MessageButtonBehaviour;
import agency.tango.materialintroscreen.SlideFragmentBuilder;

public class IntroActivity extends MaterialIntroActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addSlide(new SlideFragmentBuilder()
                .backgroundColor(R.color.colorPrimary)
                .buttonsColor(R.color.colorAccent)
                .image(R.drawable.intro_shield)
                .title("Welcome")
                .description("Aegis is a brand new open source(!) authenticator app which generates tokens for your accounts.")
                .build());

        addSlide(new SlideFragmentBuilder()
                .backgroundColor(R.color.colorAccent)
                .buttonsColor(R.color.colorPrimary)
                .neededPermissions(new String[]{Manifest.permission.CAMERA})
                .image(R.drawable.intro_scanner)
                .title("Permissions")
                .description("Aegis needs permission to your camera in order to function properly. This is needed to scan QR codes.")
                .build(),
                new MessageButtonBehaviour(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                }, "Permission granted"));
    }

    @Override
    public void onFinish() {
        super.onFinish();
        SharedPreferences prefs = this.getSharedPreferences("me.impy.aegis", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("passedIntro", true).apply();
    }
}
