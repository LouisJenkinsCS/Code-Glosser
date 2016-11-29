/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser.Session;

import edu.bloomu.codeglosser.Events.OnCloseEvent;
import edu.bloomu.codeglosser.Model.Markup;
import edu.bloomu.codeglosser.Model.MarkupFactory;
import edu.bloomu.codeglosser.Model.Templates.MarkupTemplate;
import edu.bloomu.codeglosser.Model.Templates.TemplateLeaf;
import edu.bloomu.codeglosser.Utils.Bounds;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.logging.log4j.*;
import org.json.simple.JSONArray;

/**
 *
 * @author Louis
 */
public final class MarkupManager implements SessionManager {
    
    private final PublishSubject onNoteAddOrRemove = PublishSubject.create();
    private final PublishSubject<Markup> onNoteUpdate = PublishSubject.create();
    
    
    private final static Logger LOG = LogManager.getLogger(MarkupManager.class);
    // One MarkupManager per File
    private static final HashMap<String, MarkupManager> MAPPED_INSTANCES = new HashMap<>();
    
    public static final MarkupManager NULL = new MarkupManager();
    
    private static URI PREFIX_URI;
    
    public static boolean instanceExists(String fileName) {
        return MAPPED_INSTANCES.containsKey(fileName);
    }
    
    public static void setURIPrefix(URI uri) {
        LOG.info("Set URI: " + uri.toString());
        PREFIX_URI = uri;
    }
    
    public static URI getURIPrefix() {
        return PREFIX_URI;
    }
    
    public static MarkupManager getInstance(String fileName) {
        MarkupManager manager = MAPPED_INSTANCES.get(fileName);
        
        // If we haven't created a MarkupManager for this file already, create a new one.
        if (manager == null) {
            manager = new MarkupManager();
            MAPPED_INSTANCES.put(fileName, manager);
        }
        
        return manager;
    }
    
    private final HashMap<String, Markup> noteMap;
    private Markup currentNote;
    
    private MarkupManager() {
        noteMap = new HashMap<>();
        currentNote = null;
    }

    @Override
    public void onClose(OnCloseEvent event) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getFileName() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getTag() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public JSONArray serializeAll() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void deserializeAll(JSONArray arr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    public void setCurrentNote(Markup note) {
        this.currentNote = note;
    }
    
    public List<Markup> getAllNotes() {
        return noteMap.values().stream().collect(Collectors.toList());
    }
    
    public Optional<Markup> getNote(Bounds bounds) {
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
    
    public Optional<Markup> getNote(String id) {
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
        getNote(bounds).map(Markup::getId).ifPresent(this::deleteNote);
    }
    
    public void deleteNote(String id) {
        Markup note = noteMap.get(id);
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
    
    public Observable<Markup> observeNoteUpdate() {
        return onNoteUpdate;
    }
    
    public void deleteAllNotes() {
        noteMap.clear();
        onNoteAddOrRemove.onNext(new Object());
        currentNote = null;
    }
    
    public Markup createNote(Bounds ...bounds) {
        Markup note = MarkupFactory.createNote(bounds);
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
        
        MarkupTemplate mt = template.getTemplate();
        currentNote.setMsg(mt.getMessage());
        currentNote.setHighlightColor(mt.getColor());
        onNoteUpdate.onNext(currentNote);
    }
}
