/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser.Session;

import edu.bloomu.codeglosser.Events.OnCloseEvent;
import org.json.simple.JSONArray;

/**
 *
 * @author Louis
 */
public class TemplateManager implements SessionManager {
    
    private static final String FILE_NAME = "Template.json";
    private static final String TAG = "templates";

    @Override
    public void onClose(OnCloseEvent event) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public String getFileName() {
       return "Template.json";
    }

    @Override
    public String getTag() {
        return "Templates";
    }

    @Override
    public JSONArray serializeAll() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void deserializeAll(JSONArray arr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
