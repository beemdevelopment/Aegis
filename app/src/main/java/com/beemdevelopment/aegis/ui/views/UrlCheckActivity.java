/* J: 用來檢查網址的Source code */
package com.beemdevelopment.aegis.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
/* 使用EditText */
import android.widget.EditText;

import com.beemdevelopment.aegis.R;


public class UrlCheckActivity extends AegisActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /* 孤兒進程導致系統重啟 */
        if (abortIfOrphan(savedInstanceState)) {
            return;
        }
        /* 設定Content是 layout裡面的 activity_url_check檔案 */
        setContentView(R.layout.activity_url_check);
        setSupportActionBar(findViewById(R.id.toolbar));

        /* 變數宣告 */
        final EditText url_input;
        final Button send_button;


        /* 設定變數 */
        url_input = findViewById(R.id.url_input);
        send_button = findViewById(R.id.send_button);

        /* 監聽器設定 */
        send_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.out.println(url_input.getText().toString());
            }
        });



    }
}

