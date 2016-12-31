/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser.Utils;

import java.util.HashMap;

/**
 *
 * @author Louis
 */
public class IdentifierGenerator {
    
    private long id = 0;
    private final String prefix;
    
    public IdentifierGenerator(String prefix) {
        this.prefix = prefix;
    }
    
    public String getNextId() {
        return prefix + (id++);
    }
    
    public void setId(long id) {
        this.id = id;
    }
}
