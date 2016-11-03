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
public class NoteUpdateChangeEvent {
    private Note note;
    
    public static NoteUpdateChangeEvent of(Note note) {
        return new NoteUpdateChangeEvent(note);
    }
    
    public NoteUpdateChangeEvent(Note note) {
        this.note = note;
    }

    public Note getNote() {
        return note;
    }
}
