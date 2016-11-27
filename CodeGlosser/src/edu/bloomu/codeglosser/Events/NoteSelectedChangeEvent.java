/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser.Events;

import edu.bloomu.codeglosser.Model.Markup;

/**
 *
 * @author Louis
 */
public class NoteSelectedChangeEvent {
    private Markup note;
    
    public static NoteSelectedChangeEvent of(Markup note) {
        return new NoteSelectedChangeEvent(note);
    }
    
    public NoteSelectedChangeEvent(Markup note) {
        this.note = note;
    }

    public Markup getNote() {
        return note;
    }
}
