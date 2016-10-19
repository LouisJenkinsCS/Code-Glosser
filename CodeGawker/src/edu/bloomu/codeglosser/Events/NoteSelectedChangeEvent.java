/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser.Events;

import edu.bloomu.codeglosser.Model.Note;

/**
 *
 * @author Louis
 */
public class NoteSelectedChangeEvent {
    private Note note;
    
    public static NoteSelectedChangeEvent of(Note note) {
        return new NoteSelectedChangeEvent(note);
    }
    
    public NoteSelectedChangeEvent(Note note) {
        this.note = note;
    }

    public Note getNote() {
        return note;
    }
}
