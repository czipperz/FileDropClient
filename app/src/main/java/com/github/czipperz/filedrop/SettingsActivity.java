package com.github.czipperz.filedrop;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Switch;

public class SettingsActivity extends AppCompatActivity {
    public boolean sendClipboard = true;
    public boolean receiveClipboard = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Bundle data = getIntent().getExtras();
        if (data != null) {
            sendClipboard = data.getBoolean("sendClipboard", sendClipboard);
            receiveClipboard = data.getBoolean("receiveClipboard", receiveClipboard);
        }

        Switch sendClipboardSwitch = findViewById(R.id.send_clipboard);
        sendClipboardSwitch.setChecked(sendClipboard);
        Switch receiveClipboardSwitch = findViewById(R.id.receive_clipboard);
        receiveClipboardSwitch.setChecked(receiveClipboard);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra("sendClipboard", sendClipboard);
        intent.putExtra("receiveClipboard", receiveClipboard);
        setResult(Activity.RESULT_OK, intent);
        finish();
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        System.out.printf("On destroy settings\n");
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        System.out.printf("On stop settings\n");
        super.onStop();
    }

    public void sendClipboardClicked(View view) {
        sendClipboard = !sendClipboard;
        Switch sendClipboardSwitch = findViewById(R.id.send_clipboard);
        sendClipboardSwitch.setChecked(sendClipboard);
        SharedPreferences.Editor e = this.getPreferences(MODE_PRIVATE).edit();
        e.putBoolean("sendClipboard", sendClipboard);
        e.commit();
    }

    public void receiveClipboardClicked(View view) {
        receiveClipboard = !receiveClipboard;
        Switch receiveClipboardSwitch = findViewById(R.id.receive_clipboard);
        receiveClipboardSwitch.setChecked(receiveClipboard);
        SharedPreferences.Editor e = this.getPreferences(MODE_PRIVATE).edit();
        e.putBoolean("receiveClipboard", receiveClipboard);
        e.commit();
    }
}
