package com.beemdevelopment.aegis.ui;

import android.os.Bundle;

import com.beemdevelopment.aegis.R;

public class UrlCheckActivity_ScanQrcodeActivity extends AegisActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /* 孤兒進程導致系統重啟 */
        if (abortIfOrphan(savedInstanceState)) {
            return;
        }
        /* 設定Content是 layout裡面的 activity_url_check檔案
         * 原本要寫為 final View variablename = setContentView(R.layout.activityName);
         * 這裡應該是因為 extends Aegis，用this即可
         *  */
        this.setContentView(R.layout.activity_url_check_scan_qrcode);
        this.setSupportActionBar(findViewById(R.id.toolbar));


    }
}
