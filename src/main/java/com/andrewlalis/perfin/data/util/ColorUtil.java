package com.andrewlalis.perfin.data.util;

import javafx.scene.paint.Color;

public class ColorUtil {
    public static String toHex(Color color) {
        return formatColorDouble(color.getRed()) + formatColorDouble(color.getGreen()) + formatColorDouble(color.getBlue());
    }

    private static String formatColorDouble(double val) {
        String in = Integer.toHexString((int) Math.round(val * 255));
        return in.length() == 1 ? "0" + in : in;
    }
}
