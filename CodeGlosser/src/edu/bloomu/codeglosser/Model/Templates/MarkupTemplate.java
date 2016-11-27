/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser.Model.Templates;

import edu.bloomu.codeglosser.Session.SessionObject;
import edu.bloomu.codeglosser.Utils.ColorUtils;
import java.awt.Color;
import org.apache.logging.log4j.util.Strings;
import org.json.simple.JSONObject;

/**
 *
 * @author Louis
 */
public class MarkupTemplate implements SessionObject {
    
    public static final String KEY_PREFIX = "prefix";
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_TITLE = "title";
    public static final String KEY_COLOR = "color";
    
    private String prefix;
    private String message;
    private String title;
    private Color color;
    
    private boolean initialized;
    
    public MarkupTemplate() {
        this.prefix = this.message = this.title = Strings.EMPTY;
        this.color = Color.YELLOW;
        this.initialized = false;
    }

    @Override
    public String getId() {
        return this.title;
    }

    @Override
    public JSONObject serialize() {
        if (!this.initialized) {
            throw new IllegalStateException("Attempt to serialize a non-initalized template!");
        }
        
        JSONObject obj = new JSONObject();
        obj.put(KEY_PREFIX, prefix);
        obj.put(KEY_TITLE, title);
        obj.put(KEY_MESSAGE, message);
        obj.put(KEY_COLOR, ColorUtils.asString(color));
        
        return obj;
    }

    @Override
    public void deserialize(JSONObject obj) {
        this.prefix = (String) obj.get(KEY_PREFIX);
        this.title = (String) obj.get(KEY_TITLE);
        this.message = (String) obj.get(KEY_MESSAGE);
        if (obj.containsKey(KEY_COLOR)) {
            this.color = Color.decode((String) obj.get(KEY_COLOR));
        } else { 
            this.color = Color.YELLOW;
        }
        
        this.initialized = true;
    }
    
    public String getPrefix() {
        return prefix;
    }

    public String getMessage() {
        return message;
    }

    public String getTitle() {
        return title;
    }

    public Color getColor() {
        return color;
    }

    @Override
    public String toString() {
        return title;
    }
    
    
}
