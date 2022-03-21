package com.beemdevelopment.aegis.ui;

import android.Manifest;
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

        /* 變數宣告 */
        final SurfaceView scan_area;
        final TextView display_test;
        CameraSource cameraSource;
        BarcodeDetector barcodeDetector;


        /* 設定變數 */
        scan_area = (SurfaceView)findViewById(R.id.scan_area);
        display_test = (TextView)findViewById(R.id.display_test);

        barcodeDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.QR_CODE).build();
        cameraSource = new CameraSource.Builder(this,barcodeDetector).setAutoFocusEnabled(true).build();



        /* SurfaceView implements 實作 */
        scan_area.getHolder().addCallback(new SurfaceHolder.Callback(){
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                if(ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED)
                    return;
                try{
                    cameraSource.start(holder);
                }catch (IOException e){
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

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
                            display_test.setText(qrCodes.valueAt(0).displayValue);
                        }
                    });
                }
            }
        });




    }
}
