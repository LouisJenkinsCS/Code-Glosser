/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser.Events;

import edu.bloomu.codeglosser.Utils.Bounds;
import java.awt.Color;

/**
 *
 * @author Louis
 */
public class MarkupColorChangeEvent {
    private Color color;
    private Bounds bounds;
    
    public static MarkupColorChangeEvent of(Bounds b, Color c) {
        return new MarkupColorChangeEvent(b, c);
    }

    public MarkupColorChangeEvent(Bounds bounds, Color color) {
        this.color = color;
        this.bounds = bounds;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public Bounds getBounds() {
        return bounds;
    }

    public void setBounds(Bounds bounds) {
        this.bounds = bounds;
    }
}
