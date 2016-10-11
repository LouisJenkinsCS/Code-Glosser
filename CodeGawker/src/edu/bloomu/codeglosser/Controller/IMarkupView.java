/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser.Controller;

import edu.bloomu.codeglosser.Utils.Bounds;
import java.awt.Color;

/**
 *
 * @author Louis
 */
public interface IMarkupView {
    void addMarkup(Bounds ...bounds);
    void removeMarkup(Bounds ...bounds);
    void setMarkupColor(Bounds bounds, Color color);
}
