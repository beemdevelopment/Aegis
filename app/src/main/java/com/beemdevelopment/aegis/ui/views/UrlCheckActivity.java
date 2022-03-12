/* J: 用來檢查網址的Source code */
package com.beemdevelopment.aegis.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
/* 使用EditText */
import android.widget.EditText;
/* 控制鍵盤 */
import android.view.inputmethod.InputMethodManager;



import com.beemdevelopment.aegis.R;


public class UrlCheckActivity extends AegisActivity {

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
        this.setContentView(R.layout.activity_url_check);
        this.setSupportActionBar(findViewById(R.id.toolbar));

        /* 變數宣告 */
        final EditText url_input;
        final Button send_button;


        /* 設定變數 */
        url_input = findViewById(R.id.url_input);
        send_button = findViewById(R.id.send_button);




        /* 監聽器設定 */
        send_button.setOnClickListener(new View.OnClickListener()  {
            @Override
            public void onClick(View view) {
                System.out.println("URL輸入: "+url_input.getText().toString());
                /* 按下send_button就隱藏鍵盤 */
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(send_button.getWindowToken(), 0);

            }
        });




    }
}

