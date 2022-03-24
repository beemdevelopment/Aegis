package com.beemdevelopment.aegis.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.core.app.ActivityCompat;

import com.beemdevelopment.aegis.R;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;
import java.util.ResourceBundle;

public class UrlCheckActivity_ScanQrcodeActivity extends AegisActivity {
    //branch judy test
    /* 變數宣告 */
    Activity this_activity;
    SurfaceView scan_area;
    TextView display_test;
    /* 相機元件 */
    CameraSource cameraSource;
    /* build.gradle裡面dependencies implementation 裡面已經改好有添加 Google的Vision套件
     * 所以可以直接用 BarcodeDetector去分析條碼 */
    BarcodeDetector barcodeDetector;
    private static final String pass_name = "URL_text"; /* 傳遞資料的string名，新增變數避免寫死 */
    private static final int CAMERA_PERMISSION_CODE = 1;
    private static final int Scan_QR_CODE = 2;


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
        /* content含義: 它是用來訪問全局信息的接口。想訪問全局的信息必須得通過Content(透過我們寫的layout來訪問)。
         * 所謂的全局信息是指：應用程序的資源，圖片資源，字符串資源等 */
        this.setContentView(R.layout.activity_url_check_scan_qrcode);
        this.setSupportActionBar(findViewById(R.id.toolbar));




        /* 設定變數 */
        this_activity = this;
        scan_area = (SurfaceView) findViewById(R.id.scan_area);
        display_test = (TextView) findViewById(R.id.display_test);

        barcodeDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.QR_CODE).build();

        cameraSource = new CameraSource.Builder(this, barcodeDetector).setAutoFocusEnabled(true).build();



        /* SurfaceView implements 實作 */
        /* SurfaceView介紹：它擁有獨立的繪圖表面
         * 所以不會和父級ui共享同一個繪圖表面。(用surfaceView去呈現相機照的內容)  getHolder()得到 SurfaceHolder*/
        scan_area.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                try {
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this_activity, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
//                        return;
                    }
                    cameraSource.start(scan_area.getHolder()); /* 在surfaceHolder開啟相機 */
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
                /* 判斷surfaceView有任何change就啟動cameraSource */
                try {
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this_activity, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
//                        return;
                    }
                    cameraSource.start(scan_area.getHolder()); /* 在surfaceHolder開啟相機 */
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                cameraSource.stop();
            }
        });

        /* 條碼判斷 */
        barcodeDetector.setProcessor(new Detector.Processor<Barcode>(){

            @Override
            public void release() {

            }

            @Override
            public void receiveDetections(@NonNull Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> qrCodes=detections.getDetectedItems();
                if(qrCodes.size()!=0){
                    display_test.post(new Runnable() {
                        @Override
                        public void run() {
                            /* 偵測掃描網址內容有無變化 */
                            String previous_text = display_test.getText().toString();
                            display_test.setText(qrCodes.valueAt(0).displayValue);
                            /* 偵測到掃到網址就傳遞網址資料 */
                            if(!display_test.getText().equals(previous_text)){
                                pass_text();
                            }
                        }
                    });
                }
            }
        });




    }
    private void pass_text(){
        Intent intent = getIntent();
        intent.putExtra(pass_name, display_test.getText().toString());
        setResult(Scan_QR_CODE, intent);
        finish();
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch(requestCode){
            case CAMERA_PERMISSION_CODE:
                recreate(); /* 若get到 camera的 permission就 recreate整個 activity 以讓相機元件可以正常運作 */
                break;
        }
    }



}
