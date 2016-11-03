/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser.Controller;

import edu.bloomu.codeglosser.Model.Note;

/**
 *
 * @author Louis
 */
public interface NoteView {    
    void display(Note note);
    void clear();
}
