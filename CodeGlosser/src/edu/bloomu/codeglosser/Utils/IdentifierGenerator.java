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
    // Mapping of counters to prefixes (used to prevent generating duplicates.
    public static final HashMap<String, Long> PREFIX_MAP = new HashMap<>();
    
    public static String generateIdentifier(String prefix) {
        // Increment current counter for identifier; Note that due to automatic
        // unboxing of primitive types, if the PREFIX_MAP returns null, it will
        // throw a NullPointerException.
        Long counter = PREFIX_MAP.get(prefix);
        if (counter == null) {
            counter = 0L;
        }
        PREFIX_MAP.put(prefix, counter + 1);
        
        // Generate the actual identifier.
        return prefix + counter;
    }
}
