package com.github.czipperz.filedrop;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.InputStream;

public class MainActivity extends AppCompatActivity implements ServerVisitor {
    private ClipboardManager.OnPrimaryClipChangedListener clipChangedListener =
            new ClipboardManager.OnPrimaryClipChangedListener() {
                @Override
                public void onPrimaryClipChanged() {
                    if (preferences.sendClipboard) {
                        ClipData data = getSystemService(ClipboardManager.class).getPrimaryClip();
                        if (data != null) {
                            for (int i = 0; i < data.getItemCount(); ++i) {
                                System.out.printf("Item %d is %s\n", i, data.getItemAt(i));
                            }
                            String s = data.getItemAt(0).coerceToText(MainActivity.this).toString();
                            System.out.printf("Sending clipboard string '%s'\n", s);
                            for (Computer pairedIP : preferences.pairedComputers) {
                                Sender.sendSetClipboardRequest(pairedIP.ip, s);
                            }
                        }
                    }
                }
            };
    private Preferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferences = new Preferences(this);
        if (preferences.pairedComputers.isEmpty()) {
            Snackbar.make(findViewById(R.id.outer_layout), "Could not recognize any saved IP addresses", Snackbar.LENGTH_LONG).show();
        } else {
            for (Computer computer : preferences.pairedComputers) {
                showNewPairedIP(this, preferences, (ViewGroup) findViewById(R.id.computers),
                        computer, findViewById(R.id.not_connected_text), null);
            }
        }

        //getSystemService(ClipboardManager.class).removePrimaryClipChangedListener(clipChangedListener);
        getSystemService(ClipboardManager.class).addPrimaryClipChangedListener(clipChangedListener);

        if (serverThread != null) {
            throw new AssertionError();
        }
        serverThread = new Server(this);
        serverThread.start();
    }

    private Server serverThread = null;

    protected void onDestroy() {
        System.out.printf("On destroy main\n");
        if (serverThread != null) {
            try {
                serverThread.join();
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.putExtra("sendClipboard", preferences.sendClipboard);
            intent.putExtra("receiveClipboard", preferences.receiveClipboard);
            startActivityForResult(intent, 2);
        }
        return super.onOptionsItemSelected(item);
    }

    public void addButtonPressed(View view) {
        startActivityForResult(new Intent(this, QRScannerActivity.class), 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
                final Computer computer = new Computer(
                        getResources().getString(R.string.default_computer_name),
                        data.getStringExtra("QRCode"));
                System.out.printf("Add IP: '%s'\n", computer);
                preferences.pairedComputers.add(computer);
                preferences.write(this);
                showNewPairedIP(this, preferences, (ViewGroup) findViewById(R.id.computers),
                        computer, findViewById(R.id.not_connected_text), null);
                Sender.sendSaveDeviceRequest(computer.ip, new Runnable() {
                    @Override
                    public void run() {
                        showNotification(findViewById(R.id.outer_layout),
                                String.format("Paired with %s (%s)", computer.name, computer.ip));
                    }
                });
            } else {
                System.out.printf("Canceled QR reader\n");
            }
        }
        if (requestCode == 2) {
            if (resultCode == Activity.RESULT_OK) {
                preferences.sendClipboard = data.getBooleanExtra("sendClipboard", preferences.sendClipboard);
                preferences.receiveClipboard = data.getBooleanExtra("receiveClipboard", preferences.receiveClipboard);
                preferences.write(this);
            } else {
                System.out.printf("Canceled settings\n");
            }
        }
    }

    public static void showNotification(View viewById, String message) {
        Snackbar.make(viewById, message, Snackbar.LENGTH_LONG).show();
    }

    public static void showNewPairedIP(final Context context, final Preferences preferences,
                                       final ViewGroup computers, final Computer computer,
                                       final View notConnectedView, View.OnClickListener onClickListener) {
        if (computer == null) {
            throw new NullPointerException();
        } else {
            notConnectedView.setVisibility(View.GONE);

            //final LinearLayout computers = findViewById(R.id.computers);
            int verticalMargin = context.getResources().getDimensionPixelSize(R.dimen.activity_vertical_margin);
            int horizontalMargin = context.getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin);

            final RelativeLayout layout = new RelativeLayout(context);
            LinearLayout.LayoutParams layoutLP = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            layoutLP.setMargins(verticalMargin, horizontalMargin, verticalMargin, 0);
            layout.setLayoutParams(layoutLP);
            layout.setBackgroundResource(R.color.cardBackground);
            computers.addView(layout);

            final LinearLayout infoLayout = new LinearLayout(context);
            infoLayout.setLayoutParams(new RelativeLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            infoLayout.setPadding(verticalMargin, horizontalMargin, verticalMargin, horizontalMargin);
            infoLayout.setOrientation(LinearLayout.VERTICAL);
            if (onClickListener != null) {
                infoLayout.setOnClickListener(onClickListener);
            }
            layout.addView(infoLayout);

            final TextView computerNameView = new TextView(context);
            computerNameView.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            computerNameView.setText(computer.name);
            computerNameView.setTextSize(30);
            infoLayout.addView(computerNameView);

            TextView pairedIPView = new TextView(context);
            pairedIPView.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            pairedIPView.setText(String.format("(%s)", computer.ip));
            pairedIPView.setTextSize(20);
            infoLayout.addView(pairedIPView);

            final LinearLayout editLayout = new LinearLayout(context);
            editLayout.setLayoutParams(new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT, 0));
            editLayout.setOrientation(LinearLayout.HORIZONTAL);
            editLayout.setVisibility(View.GONE);
            layout.addView(editLayout);

            final View.OnLongClickListener toggleEditOnLongClick = new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if (editLayout.getVisibility() == View.GONE) {
                        editLayout.getLayoutParams().height = infoLayout.getHeight();
                        editLayout.setVisibility(View.VISIBLE);
                        infoLayout.setVisibility(View.GONE);
                    } else {
                        editLayout.setVisibility(View.GONE);
                        infoLayout.setVisibility(View.VISIBLE);
                    }
                    return true;
                }
            };

            ImageView editButton = new ImageButton(context);
            LinearLayout.LayoutParams editButtonLP = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT);
            editButtonLP.weight = 1;
            editButton.setLayoutParams(editButtonLP);
            editButton.setImageResource(R.drawable.edit);
            editButton.setColorFilter(ContextCompat.getColor(context, R.color.colorPrimary), android.graphics.PorterDuff.Mode.SRC_IN);
            editButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    System.out.printf("Edit Pressed\n");
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle("Computer Name");

                    final EditText input = new EditText(context);
                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                    input.setText(computerNameView.getText());
                    builder.setView(input);

                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String text = input.getText().toString();
                            computer.name = text;
                            computerNameView.setText(text);
                            preferences.write(context);
                            toggleEditOnLongClick.onLongClick(null);
                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });

                    AlertDialog alert = builder.create();
                    alert.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                    alert.show();
                }
            });
            editButton.setOnLongClickListener(toggleEditOnLongClick);
            editLayout.addView(editButton);

            ImageView deleteButton = new ImageButton(context);
            LinearLayout.LayoutParams deleteButtonLP = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT);
            deleteButtonLP.weight = 1;
            deleteButton.setLayoutParams(deleteButtonLP);
            deleteButton.setImageResource(R.drawable.delete);
            deleteButton.setColorFilter(ContextCompat.getColor(context, R.color.colorPrimary), android.graphics.PorterDuff.Mode.SRC_IN);
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    System.out.printf("Delete Pressed\n");
                    preferences.pairedComputers.remove(computer);
                    preferences.write(context);
                    if (preferences.pairedComputers.isEmpty()) {
                        notConnectedView.setVisibility(View.VISIBLE);
                    }
                    computers.removeView(layout);
                }
            });
            deleteButton.setOnLongClickListener(toggleEditOnLongClick);
            editLayout.addView(deleteButton);

            layout.setOnLongClickListener(toggleEditOnLongClick);
        }
    }

    /*
    public void setPairedIP(final String pairedIP) {
        System.out.printf("IP: '%s'\n", pairedIP);
        this.pairedIP = pairedIP;
        writePreferences();
        showNewPairedIP(getResources().getString(R.string.default_computer_name), pairedIP);
        if (pairedIP != null) {
            Sender.sendSaveDeviceRequest(pairedIP, new Runnable() {
                @Override
                public void run() {
                    Snackbar.make(findViewById(R.id.outer_layout), "Paired with " + pairedIP, Snackbar.LENGTH_LONG).show();
                }
            });
        }
    }
    */

    @Override
    public void onFileTransfer(String fileName, InputStream contents) {

    }

    @Override
    public void onOpenUri(String uri) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        this.startActivity(browserIntent);
    }

    @Override
    public void onSetClipboard(String clipString) {
        if (preferences.receiveClipboard) {
            getSystemService(ClipboardManager.class).removePrimaryClipChangedListener(clipChangedListener);
            ClipData clip = ClipData.newPlainText("FileDrop", clipString);
            this.getSystemService(ClipboardManager.class).setPrimaryClip(clip);
            getSystemService(ClipboardManager.class).addPrimaryClipChangedListener(clipChangedListener);
        }
    }

    @Override
    public void onSaveDevice(String ip) {

    }
}
