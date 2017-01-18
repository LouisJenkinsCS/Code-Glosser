/* BSD 3-Clause License
 *
 * Copyright (c) 2017, Louis Jenkins <LouisJenkinsCS@hotmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     - Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     - Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *
 *     - Neither the name of Louis Jenkins, Bloomsburg University nor the names of its 
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.bloomu.codeglosser.Model;

import edu.bloomu.codeglosser.Utils.Bounds;
import edu.bloomu.codeglosser.Utils.ColorUtils;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 *
 * @author Louis
 */
public class Markup {

    private static final Logger LOG = Logger.getLogger(Markup.class.getName());
    public static Markup DEFAULT = new Markup("", "<None Selected>");
    
    public static final String COLOR = "color";
    public static final String ID = "id";
    public static final String MESSAGE = "message";
    public static final String BOUNDS = "bounds";
    
    public static Markup template(String msg, Color c) {
        Markup m = new Markup(msg, null, new Bounds[] {});
        m.setHighlightColor(c);
        return m;
        
    }
    
    public static Markup deserialize(JSONObject obj) {
        LOG.info("Deserializing: " + obj);
        Color color = Color.decode((String) obj.get(COLOR));
        String id = (String) obj.get(ID);
        String msg = (String) obj.get(MESSAGE);
        List<Bounds> bounds = (List<Bounds>) ((JSONArray) obj.get(BOUNDS))
                .stream()
                .map(object -> Bounds.deserialize((JSONObject) object))
                .collect(Collectors.toList());
        
        Markup markup = new Markup(msg, id, bounds.toArray(new Bounds[bounds.size()]));
        markup.highlightColor = color;
        
        return markup;
    }
    
    public static Markup template(String msg) {
        return template(msg, null);
    }
    
    public static Markup template(Color c) {
        return template(null, c);
    }
    
    private String msg;
    private String id;
    private List<Bounds> offsets = new ArrayList<>();
    private Color highlightColor;
    private Color textColor;

    public Markup(String msg, String id, Bounds ...offsets) {
        this.msg = msg;
        this.id = id;
        Collections.addAll(this.offsets, offsets);
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

    public Bounds[] getOffsets() {
        return offsets.toArray(new Bounds[offsets.size()]);
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
    
    public JSONObject serialize() {
        JSONObject obj = new JSONObject();
        obj.put(COLOR, ColorUtils.asString(highlightColor));
        obj.put(ID, id);
        obj.put(MESSAGE, msg);
        
        JSONArray serializedBounds = new JSONArray();
        offsets
                .stream()
                .map(Bounds::serialize)
                .forEach(serializedBounds::add);
        obj.put(BOUNDS, serializedBounds);
        
        return obj;
    }

    @Override
    public String toString() {
        return "Markup{" + "msg=" + msg + ", id=" + id + ", offsets=" + offsets + ", highlightColor=" + highlightColor + ", textColor=" + textColor + '}';
    }
}
