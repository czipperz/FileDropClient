package com.github.czipperz.filedrop;

import android.support.annotation.NonNull;

import java.io.Serializable;

public class Computer implements Comparable<Computer>, Serializable {
    public String name;
    public String ip;

    public Computer(String name, String ip) {
        this.name = name;
        this.ip = ip;
    }

    @Override
    public int compareTo(@NonNull Computer computer) {
        int nameCompare = name.compareTo(computer.name);
        if (nameCompare == 0) {
            return ip.compareTo(computer.ip);
        } else {
            return nameCompare;
        }
    }

    public boolean equals(Object other) {
        if (other instanceof Computer) {
            Computer computer = (Computer) other;
            return name.equals(computer.name) && ip.equals(computer.ip);
        }
        return false;
    }
}