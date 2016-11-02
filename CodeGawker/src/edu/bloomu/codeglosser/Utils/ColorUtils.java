/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser.Utils;

import java.awt.Color;

/**
 *
 * @author Louis
 */
public class ColorUtils {
    public static Color makeTransparent(Color color) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), 127);
    }
}
