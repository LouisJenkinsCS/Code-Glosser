/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser.Model;

import edu.bloomu.codeglosser.Utils.Bounds;
import edu.bloomu.codeglosser.Utils.IdentifierGenerator;

/**
 *
 * @author Louis
 */
public final class NoteFactory {
    private static long notes;
    
    public static Note createNote(Bounds ...bounds) {
        return new Note(null, IdentifierGenerator.generateIdentifier("Note"), bounds);
    }
}
