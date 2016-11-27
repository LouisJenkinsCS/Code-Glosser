/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser.Session;

import com.google.common.eventbus.Subscribe;
import edu.bloomu.codeglosser.Events.OnCloseEvent;
import org.json.simple.JSONArray;

/**
 *
 * @author Louis
 */
public interface SessionManager {
    
    @Subscribe
    void onClose(OnCloseEvent event);
    
    String getFileName();
    
    String getTag();
    
    JSONArray serializeAll();
    
    void deserializeAll(JSONArray arr);
}
