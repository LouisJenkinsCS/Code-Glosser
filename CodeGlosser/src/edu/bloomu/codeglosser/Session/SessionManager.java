/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser.Session;

import org.json.simple.JSONObject;

/**
 *
 * @author Louis
 */
public interface SessionManager {
    
    public String getFileName();
    
    public String getTag();
    
    JSONObject[] serializeAll();
    
    void deserializeAll(JSONObject[] objs);
}
