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
/* ImageButton的import */
import android.widget.ImageButton;

/* URL lib */
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

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
        final ImageButton clear_button;


        /* 設定變數 */
        url_input = findViewById(R.id.url_input);
        send_button = findViewById(R.id.send_button);
        /* 創造ImageButton需要也宣告成ImageButton */
        clear_button = findViewById(R.id.clear_button);





        /* 監聽器設定 */
        send_button.setOnClickListener(new View.OnClickListener()  {
            @Override
            public void onClick(View view) {
                System.out.println("URL輸入: "+url_input.getText().toString());
                /* 按下send_button就隱藏鍵盤 */
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(send_button.getWindowToken(), 0);
                UrlCheck(url_input.getText().toString());


            }
        });

        clear_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                url_input.setText("");
            }
        });




    }
    /* 檢查URL function */
    public void UrlCheck(String url_text){
        /* URL parameter */
        URL url;
        URLConnection urlConnection; /* 實例可用於讀取和寫入此 URL 引用的資源，可以得到 .html 相關資訊 */
        try{
            /* 設定變數 */
            url = new URL(url_text);
            urlConnection = url.openConnection();
            /* print參數 */
            System.out.println("URL參數：");
            System.out.println("URL：" + url.toString());
            System.out.println("協議：" + url.getProtocol());
            System.out.println("驗證信息：" + url.getAuthority());
            System.out.println("文件名及請求參數：" + url.getFile());
            System.out.println("主機名：" + url.getHost());
            System.out.println("路徑：" + url.getPath());
            System.out.println("端口：" + url.getPort());
            System.out.println("默認端口：" + url.getDefaultPort());
            System.out.println("請求參數：" + url.getQuery());
            System.out.println("定位位置：" + url.getRef());
            /* print UrlConnection */
            System.out.println("");
            System.out.println("URL Connection參數: ");
//            System.out.println(urlConnection.getContentType());
//            System.out.println(urlConnection.getContentLength());
//            System.out.println(urlConnection.getContentEncoding());
//            System.out.println(urlConnection.getDate());
//            System.out.println(urlConnection.getExpiration());
//            System.out.println(urlConnection.getLastModified());

        }catch (IOException e){
            e.printStackTrace();
        }

    }

}

