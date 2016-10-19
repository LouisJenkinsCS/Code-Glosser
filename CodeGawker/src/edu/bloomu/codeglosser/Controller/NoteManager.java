/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser.Controller;

import edu.bloomu.codeglosser.Model.Note;
import edu.bloomu.codeglosser.Model.NoteFactory;
import edu.bloomu.codeglosser.Utils.Bounds;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import java.awt.Color;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.logging.log4j.*;

/**
 *
 * @author Louis
 */
public final class NoteManager {
    
    PublishSubject onChange = PublishSubject.create();
    
    private final static Logger logger = LogManager.getLogger(NoteManager.class);
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
    private Note currentNote;
    
    private NoteManager() {
        notes = new HashMap<>();
        currentNote = null;
    }
    
    public List<Note> getAllNotes() {
        return notes.values().stream().collect(Collectors.toList());
    }
    
    public Optional<Note> getNote(Bounds bounds) {
        return notes
                .values()
                .stream()
                .filter((n) -> n.inRange(bounds))
                .findFirst();
    }
    
    public boolean isValidPosition(Bounds bounds) {
        return !getNote(bounds).isPresent();
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
    
    public void deleteNote(Bounds bounds) {
        getNote(bounds).map(Note::getId).ifPresent(this::deleteNote);
    }
    
    public void deleteNote(String id) {
        Note note = notes.get(id);
        if (note != null) {
            notes.remove(id);
            if (note == currentNote) {
                currentNote = null;
            }
            onChange.onNext(new Object());
        }
    }
    
    public Observable observe() {
        return onChange;
    }
    
    public void deleteAllNotes() {
        notes.clear();
        onChange.onNext(new Object());
    }
    
    public Note createNote(Bounds bounds) {
        Note note = NoteFactory.createNote(bounds);
        // TODO: Bounds -> [Bounds]
        notes.put(note.getId(), note);
        onChange.onNext(new Object());
        return note;
    }
}
