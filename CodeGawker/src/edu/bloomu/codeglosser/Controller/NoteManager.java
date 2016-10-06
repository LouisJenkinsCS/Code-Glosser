/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser.Controller;

import edu.bloomu.codeglosser.Model.Note;
import edu.bloomu.codeglosser.Model.NoteFactory;
import edu.bloomu.codeglosser.Utils.Bounds;
import java.awt.Color;
import java.util.HashMap;
import java.util.Optional;
import org.apache.logging.log4j.*;

/**
 *
 * @author Louis
 */
public final class NoteManager {
    
    private final static Logger logger = LogManager.getLogger(NoteManager.class);
    // One NoteManager per File
    private static final HashMap<String, NoteManager> MAPPED_INSTANCES = new HashMap<>();
    
    private static NoteView NOTE_VIEW;
    private static IMarkupView NOTEPAD_VIEW;
    
    public static void setNoteView(NoteView nView) {
        NOTE_VIEW = nView;
    }
    
    public static void setNotepadView(IMarkupView npView) {
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
                .filter((note) -> note.inRange(start, end))
                .findFirst();
    }
    
    public boolean isValidPosition(int start, int end) {
        return !getNote(start, end).isPresent();
    }
    
    public Optional<Note> getNote(String id) {
        return Optional.ofNullable(notes.get(id));
    }
    
    public void debug() {
        logger.debug(notes
                .entrySet()
                .stream()
                .sorted((entry1, entry2) -> entry1.getValue().getRange().compareTo(entry2.getValue().getRange()))
                .map((e) -> "Key: " + e.getKey() + ", Value: " + e.getValue())
                .reduce("Notes: {", (s1, s2) -> s1 + "\n\t" + s2)
                .concat("\n}")
        );
    }
    
    public void deleteNote(int start, int end) {
        getNote(start, end).map(Note::getId).ifPresent(this::deleteNote);
    }
    
    public void deleteCurrentNote() {
        if (currentNote != null) {
            deleteNote(currentNote.getId());
        }
    }
    
    public void deleteNote(String id) {
        Note note = notes.get(id);
        if (note != null) {
            NOTEPAD_VIEW.removeMarkup(note.getRange());
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
                // TODO: Flatmap
                .map(Note::getRange)
                .forEach(NOTEPAD_VIEW::removeMarkup);
        notes.clear();
    }
    
    public void createNote(int start, int end) {
        createNote(Bounds.of(start, end));
    }
    
    public void createNote(Bounds bounds) {
        Note note = NoteFactory.createNote(bounds);
        // TODO: Bounds -> [Bounds]
        notes.put(note.getId(), note);
        NOTEPAD_VIEW.addMarkup(bounds);
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
                .filter((note) -> note.inRange(offset, offset))
                .findFirst();
    }
    
    public Optional<Note> currentNote() {
        return Optional.ofNullable(currentNote);
    }
    
    public void setHighlightColor(Color c) {
        NOTEPAD_VIEW.setMarkupColor(c);
    }
}
