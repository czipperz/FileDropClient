package com.github.czipperz.filedrop;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import com.ipaulpro.afilechooser.utils.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class ShareActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);

        Preferences preferences = new Preferences(this);
        if (preferences.pairedComputers.isEmpty()) {
            Snackbar.make(findViewById(R.id.outer_layout), "Could not recognize any saved IP addresses", Snackbar.LENGTH_LONG).show();
        } else {
            for (Computer computer : preferences.pairedComputers) {
                final String ip = computer.ip;
                View.OnClickListener onClickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        sendMessage(ip);
                    }
                };
                MainActivity.showNewPairedIP(this, preferences, (ViewGroup)findViewById(R.id.share_computers),
                        computer, findViewById(R.id.share_not_connected_text), onClickListener);
            }
        }

        if (Intent.ACTION_SEND.equals(getIntent().getAction())) {
            if (preferences.pairedComputers.isEmpty()) {
                MainActivity.showNotification(findViewById(R.id.share_outer_layout), "No computers to share to");
            } else if (preferences.pairedComputers.size() == 1) {
                String pairedIP = preferences.pairedComputers.get(0).ip;
                sendMessage(pairedIP);
            } else {
                MainActivity.showNotification(findViewById(R.id.share_outer_layout), "Select a computer to share to");
            }
        }
    }

    private void sendMessage(String pairedIP) {
        if (getIntent().getType() == null) {
        } else if (getIntent().getType().startsWith("image/")) {
            Uri uri = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
            String filePath = FileUtils.getPath(this, uri);
            if (filePath == null) {
                Snackbar.make(findViewById(R.id.outer_layout), "Could not recognize file name", Snackbar.LENGTH_LONG).show();
            } else {
                try {
                    InputStream stream = getContentResolver().openInputStream(uri);
                    if (stream != null) {
                        System.out.printf("File Path '%s'\n", filePath);
                        String name = new File(filePath).getName();
                        Sender.sendFileTransferRequest(pairedIP, name, stream);
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        } else if (getIntent().getType().startsWith("text/")) {
            String text = getIntent().getStringExtra(Intent.EXTRA_TEXT);
            try {
                new URL(text);
                System.out.printf("Open in browser: '%s'\n", text);
                Sender.sendOpenUriRequest(pairedIP, text);
            } catch (MalformedURLException e) {
                System.out.printf("Shared text will be copied to remote keyboard\n");
                Sender.sendSetClipboardRequest(pairedIP, text);
            }
        }
        setResult(Activity.RESULT_OK);
        finish();
    }
}
