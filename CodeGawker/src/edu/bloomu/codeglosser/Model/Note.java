/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser.Model;

import com.google.common.collect.Lists;
import edu.bloomu.codeglosser.Utils.Bounds;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.logging.log4j.util.Strings;

/**
 *
 * @author Louis
 */
public class Note {
    public static Note DEFAULT = new Note(Strings.EMPTY, "<None Selected>");
    
    private String msg;
    private String id;
    private List<Bounds> offsets = new ArrayList<>();
    private Color highlightColor;
    private Color textColor;

    public Note(String msg, String id, Bounds ...offsets) {
        this.msg = msg;
        this.id = id;
        this.offsets.addAll(Lists.newArrayList(offsets));
        Collections.sort(this.offsets);
        this.highlightColor = Color.YELLOW;
        this.textColor = Color.BLACK;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean inRange(Bounds bounds) {
        return getRange().collidesWith(bounds);
    }
    
    public Bounds getRange() {
        return Bounds.of(offsets.get(0).getStart(), offsets.get(offsets.size()-1).getEnd());
    }

    public Color getHighlightColor() {
        return highlightColor;
    }

    public void setHighlightColor(Color highlightColor) {
        this.highlightColor = highlightColor;
    }

    public Color getTextColor() {
        return textColor;
    }

    public void setTextColor(Color textColor) {
        this.textColor = textColor;
    }

    @Override
    public String toString() {
        return id;
    }
}
