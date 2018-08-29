package com.github.czipperz.filedrop;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.RelativeLayout;

import com.google.zxing.Result;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class QRScannerActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler {
    private ZXingScannerView scannerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrscanner);

        RelativeLayout topLevel = findViewById(R.id.qrscanner_top_level);
        scannerView = new ZXingScannerView(this);
        topLevel.addView(scannerView);

        setResult(Activity.RESULT_CANCELED);
    }

    @Override
    protected void onResume() {
        super.onResume();
        scannerView.setResultHandler(this);
        scannerView.startCamera();
    }

    @Override
    protected void onPause() {
        scannerView.stopCamera();
        super.onPause();
    }

    @Override
    public void handleResult(Result result) {
        Intent intent = new Intent();
        intent.putExtra("QRCode", result.getText());
        setResult(Activity.RESULT_OK, intent);
        finish();
    }
}
