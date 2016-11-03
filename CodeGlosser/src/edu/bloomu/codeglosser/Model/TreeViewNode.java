/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser.Model;

/**
 *
 * @author Louis
 */
public interface TreeViewNode {
    public static final String KEY_PREFIX = "prefix";
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_TITLE = "title";
    public static final String KEY_COLOR = "color";
    
    @Override
    public String toString();
    
}
