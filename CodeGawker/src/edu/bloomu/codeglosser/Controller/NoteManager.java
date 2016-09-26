/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser.Controller;

import edu.bloomu.codeglosser.Model.Note;
import edu.bloomu.codeglosser.Model.NoteFactory;
import java.awt.Color;
import java.util.HashMap;
import java.util.Optional;

/**
 *
 * @author Louis
 */
public final class NoteManager {
    // One NoteManager per File
    private static final HashMap<String, NoteManager> MAPPED_INSTANCES = new HashMap<>();
    
    private static NoteView NOTE_VIEW;
    private static NotepadView NOTEPAD_VIEW;
    
    public static void setNoteView(NoteView nView) {
        NOTE_VIEW = nView;
    }
    
    public static void setNotepadView(NotepadView npView) {
        NOTEPAD_VIEW = npView;
    }
    
    public static NoteManager getInstance(String fileName) {
        // Ensure 'setNoteView' is called before 'getInstance'
        if (NOTE_VIEW == null) {
            throw new IllegalStateException("getInstance called before setNoteView!");
        }
        
        // Ensure 'setNotepadView' is called before 'getInstance'
        if (NOTEPAD_VIEW == null) {
            throw new IllegalStateException("getInstance called before setNotepadView!");
        }
        
        NoteManager manager = MAPPED_INSTANCES.get(fileName);
        
        // If we haven't created a NoteManager for this file already, create a new one.
        if (manager == null) {
            manager = new NoteManager();
            MAPPED_INSTANCES.put(fileName, manager);
        }
        
        return manager;
    }
    
    private final HashMap<String, Note> notes;
    private Note currentNote;

    private NoteManager() {
        notes = new HashMap<>();
        currentNote = null;
    }

    public Optional<Note> getNote(int start, int end) {
        return notes
                .values()
                .stream()
                .filter((note) -> note.getStart() == start && note.getEnd() == end)
                .findFirst();
    }
    
    public boolean isValidPosition(int start, int end) {
        return !getNote(start, end).isPresent();
    }
    
    public Optional<Note> getNote(String id) {
        return Optional.ofNullable(notes.get(id));
    }
    
    public void debug() {
        notes
                .entrySet()
                .stream()
                .sorted((entry1, entry2) -> entry1.getValue().getStart() - entry2.getValue().getStart())
                .forEach((entry) -> System.err.println("Key:" + entry.getKey() + ";Value:" + entry.getValue()));
    }
    
    public void deleteNote(int start, int end) {
        getNote(start, end).map(Note::getId).ifPresent(this::deleteNote);
    }
    
    public void deleteNote(String id) {
        Note note = notes.get(id);
        if (note != null) {
            NOTEPAD_VIEW.removeMarkup(note.getStart(), note.getEnd());
            notes.remove(id);
            if (note == currentNote) {
                currentNote = null;
                NOTE_VIEW.clear();
            }
        }
    }
    
    public void deleteAllNotes() {
        notes.values()
                .stream()
                .forEach((note) -> NOTEPAD_VIEW.removeMarkup(note.getStart(), note.getEnd()));
        notes.clear();
    }
    
    public void createNote(int start, int end) {
        Note note = NoteFactory.createNote(start, end);
        notes.put(note.getId(), note);
        NOTEPAD_VIEW.addMarkup(start, end);
    }
    
    public void showNote(int start, int end) {
        getNote(start, end).ifPresent((note) -> {
            currentNote = note;
            NOTE_VIEW.display(note);
        });
    }
    
    public Optional<Note> getNote(int offset) {
        return notes.values()
                .stream()
                .filter((note) -> offset >= note.getStart() && offset <= note.getEnd())
                .findFirst();
    }
    
    public Optional<Note> currentNote() {
        return Optional.ofNullable(currentNote);
    }
    
    public void setHighlightColor(Color c) {
        NOTEPAD_VIEW.setMarkupColor(c);
    }
}
