/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser.Model;

import java.util.HashMap;
import java.util.NoSuchElementException;

/**
 *
 * @author Louis
 */
public final class NoteManager {
    // One NoteManager per File
    private static final HashMap<String, NoteManager> MAPPED_INSTANCES = new HashMap<>();
    
    public static NoteManager getInstance(String fileName) {
        NoteManager manager = MAPPED_INSTANCES.get(fileName);
        
        // If we haven't created a NoteManager for this file already, create a new one.
        if (manager == null) {
            manager = new NoteManager();
            MAPPED_INSTANCES.put(fileName, manager);
        }
        
        return manager;
    }
    
    private final HashMap<String, Note> notes;

    private NoteManager() {
        notes = new HashMap<>();
    }
    
    public Note getNote(int start, int end) throws NoSuchElementException {
        return notes
                .values()
                .stream()
                .filter((note) -> note.getStart() == start && note.getEnd() == end)
                .findFirst()
                .get();
    }
    
    public boolean isValidPosition(int start, int end) {
        return notes
                .values()
                .stream()
                .noneMatch((note) -> start >= note.getStart() && end <= note.getEnd());
    }
    
    public Note getNote(String id) {
        return notes.get(id);
    }
    
    public void debug() {
        notes
                .entrySet()
                .stream()
                .sorted((entry1, entry2) -> entry1.getValue().getStart() - entry2.getValue().getStart())
                .forEach((entry) -> System.err.println("Key:" + entry.getKey() + ";Value:" + entry.getValue()));
    }
    
    public void deleteNote(int start, int end) throws NoSuchElementException {
        notes.values()
                .stream()
                .filter((note) -> note.getStart() == start && note.getEnd() == end)
                .map((note) -> note.getId())
                .findFirst()
                .ifPresent(notes::remove);
    }
    
    public void deleteNote(String id) {
        notes.remove(id);
    }
    
    public void deleteAllNotes() {
        notes.clear();
    }
    
    public void createNote(int start, int end) {
        Note note = NoteFactory.createNote(start, end);
        notes.put(note.getId(), note);
    }
}
