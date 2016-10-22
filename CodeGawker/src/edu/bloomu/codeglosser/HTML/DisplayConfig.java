/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser.HTML;

import java.awt.Color;

/**
 *
 * @author Louis
 */
public class DisplayConfig {
    private Color color;
    private HTMLFont font;

    public DisplayConfig(Color color, HTMLFont font) {
        this.color = color;
        this.font = font;
    }

    public DisplayConfig(Color color) {
        this.color = color;
        this.font = HTMLFont.PLAIN;
    }

    public Color getColor() {
        return color;
    }

    public HTMLFont getFont() {
        return font;
    }
}
