package com.github.czipperz.filedrop;

import android.content.Context;
import android.preference.Preference;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class Preferences {
    public ArrayList<Computer> pairedComputers;
    public boolean sendClipboard;
    public boolean receiveClipboard;

    private static final String file = "preferences";

    public Preferences(Context context) {
        try (FileInputStream fos = context.openFileInput(file)) {
            try (ObjectInputStream obs = new ObjectInputStream(new BufferedInputStream(fos))) {
                pairedComputers = (ArrayList<Computer>) obs.readObject();
                sendClipboard = obs.readBoolean();
                receiveClipboard = obs.readBoolean();
            }
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
            pairedComputers = new ArrayList<>();
        }
    }

    public void write(Context context) {
        try (FileOutputStream fos = context.openFileOutput(file, Context.MODE_PRIVATE)) {
            try (ObjectOutputStream obs = new ObjectOutputStream(new BufferedOutputStream(fos))) {
                obs.writeObject(pairedComputers);
                obs.writeBoolean(sendClipboard);
                obs.writeBoolean(receiveClipboard);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
