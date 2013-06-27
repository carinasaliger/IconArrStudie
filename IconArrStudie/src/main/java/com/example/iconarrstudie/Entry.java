package com.example.iconarrstudie;

import java.util.Arrays;

/**
 * Created by john-louis on 27.05.13.
 */
public class Entry {
    private int x;
    private int y;
    private int span_x;
    private int span_y;
    private byte[] icon;
    private int tag;
    private String title;



    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    private String intent;
    private int container;
    public static final int FOLDER = 2;
    public static final int ICON = 1;
    public static final int WIDGET = 4;
    public static final int GENERATED = 5;

    public Entry(int x, int y, int span_x, int span_y, byte[] icon, int tag, String title, String intent, int container) {
        this.x = x;
        this.y = y;
        this.span_x = span_x;
        this.span_y = span_y;
        this.icon = icon;
        this.tag = tag;
        this.title = title;
        this.intent = intent;
        this.container = container;
    }

    @Override
    public String toString() {
        return "Entry{" +
                "x=" + x +
                ", y=" + y +
                ", span_x=" + span_x +
                ", span_y=" + span_y +
                ", tag=" + tag +
                ", title='" + title + '\'' +
                ", container=" + container +
                '}';
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }
    public void setY(int y) {
        this.y = y;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getSpan_x() {
        return span_x;
    }

    public void setSpan_x(int span_x) {
        this.span_x = span_x;
    }

    public int getSpan_y() {
        return span_y;
    }

    public void setSpan_y(int span_y) {
        this.span_y = span_y;
    }

    public byte[] getIcon() {
        return icon;
    }

    public void setIcon(byte[] icon) {
        this.icon = icon;
    }

    public int getTag() {
        return tag;
    }

    public void setTag(int tag) {
        this.tag = tag;
    }

    public int getContainer() {
        return container;
    }

    public void setContainer(int container) {
        this.container = container;
    }
}
