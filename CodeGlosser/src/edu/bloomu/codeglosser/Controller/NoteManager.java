/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser.Controller;

import edu.bloomu.codeglosser.Model.Note;
import edu.bloomu.codeglosser.Model.NoteFactory;
import edu.bloomu.codeglosser.Model.TemplateLeaf;
import edu.bloomu.codeglosser.Utils.Bounds;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
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
    
    private final PublishSubject onNoteAddOrRemove = PublishSubject.create();
    private final PublishSubject<Note> onNoteUpdate = PublishSubject.create();
    
    
    private final static Logger LOG = LogManager.getLogger(NoteManager.class);
    // One NoteManager per File
    private static final HashMap<String, NoteManager> MAPPED_INSTANCES = new HashMap<>();
    
    public static final NoteManager NULL = new NoteManager();
    
    public static NoteManager getInstance(String fileName) {
        NoteManager manager = MAPPED_INSTANCES.get(fileName);
        
        // If we haven't created a NoteManager for this file already, create a new one.
        if (manager == null) {
            manager = new NoteManager();
            MAPPED_INSTANCES.put(fileName, manager);
        }
        
        return manager;
    }
    
    private final HashMap<String, Note> noteMap;
    private Note currentNote;
    
    private NoteManager() {
        noteMap = new HashMap<>();
        currentNote = null;
    }
    
    public void setCurrentNote(Note note) {
        this.currentNote = note;
    }
    
    public List<Note> getAllNotes() {
        return noteMap.values().stream().collect(Collectors.toList());
    }
    
    public Optional<Note> getNote(Bounds bounds) {
        return noteMap
                .values()
                .stream()
                .filter((n) -> n.inRange(bounds))
                .peek(n -> currentNote = n)
                .findFirst();
    }
    
    public boolean isValidPosition(Bounds bounds) {
        return !getNote(bounds).isPresent();
    }
    
    public Optional<Note> getNote(String id) {
        return Optional.ofNullable(noteMap.get(id));
    }
    
    public void debug() {
        LOG.debug(noteMap
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
        Note note = noteMap.get(id);
        if (note != null) {
            noteMap.remove(id);
            if (note == currentNote) {
                currentNote = null;
            }
            onNoteAddOrRemove.onNext(new Object());
        }
    }
    
    public Observable observeAddOrRemove() {
        return onNoteAddOrRemove;
    }
    
    public Observable<Note> observeNoteUpdate() {
        return onNoteUpdate;
    }
    
    public void deleteAllNotes() {
        noteMap.clear();
        onNoteAddOrRemove.onNext(new Object());
        currentNote = null;
    }
    
    public Note createNote(Bounds ...bounds) {
        Note note = NoteFactory.createNote(bounds);
        // TODO: Bounds -> [Bounds]
        noteMap.put(note.getId(), note);
        onNoteAddOrRemove.onNext(new Object());
        currentNote = note;
        return note;
    }
    
    public void applyTemplate(TemplateLeaf template) {
        if (currentNote == null) {
            LOG.warn("Attempt to apply template when currentNote is null.");
            return;
        }
        
        currentNote.setMsg(template.getMessage());
        currentNote.setHighlightColor(template.getColor());
        onNoteUpdate.onNext(currentNote);
    }
}
