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
public class NoteUpdateChangeEvent {
    private Markup note;
    
    public static NoteUpdateChangeEvent of(Markup note) {
        return new NoteUpdateChangeEvent(note);
    }
    
    public NoteUpdateChangeEvent(Markup note) {
        this.note = note;
    }

    public Markup getNote() {
        return note;
    }
}
