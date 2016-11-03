/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser.Model;

import java.awt.Color;
import java.util.logging.Logger;
import org.json.simple.JSONObject;

/**
 *
 * @author Louis
 */
public class TemplateLeaf implements TreeViewLeaf {
    
    
    private static final Logger LOG = Logger.getLogger(TemplateLeaf.class.getName());
    
    private final String prefix;
    private final String message;
    private final String title;
    private final Color color;

    public TemplateLeaf(JSONObject data) {
        LOG.info("Parsing Leaf:" + data.toJSONString());
        this.prefix = (String) data.get(KEY_PREFIX);
        this.title = (String) data.get(KEY_TITLE);
        this.message = (String) data.get(KEY_MESSAGE);
        if (data.containsKey(KEY_COLOR)) {
            this.color = Color.decode((String) data.get(KEY_COLOR));
        } else { 
            this.color = Color.YELLOW;
        }
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
