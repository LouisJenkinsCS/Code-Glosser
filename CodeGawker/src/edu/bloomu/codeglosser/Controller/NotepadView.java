/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser.Controller;

import java.awt.Color;

/**
 *
 * @author Louis
 */
public interface NotepadView {
    void addMarkup(int start, int end);
    void removeMarkup(int start, int end);
    void setMarkupColor(Color color);
}
