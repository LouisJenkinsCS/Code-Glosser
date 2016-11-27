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
public interface SessionObject {
    
    String getId();
    
    JSONObject serialize();
    
    void deserialize(JSONObject obj);
}
