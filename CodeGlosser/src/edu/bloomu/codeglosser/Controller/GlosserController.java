/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser.Controller;

import edu.bloomu.codeglosser.Session.MarkupManager;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import edu.bloomu.codeglosser.Events.Event;
import edu.bloomu.codeglosser.Events.FileChangeEvent;
import edu.bloomu.codeglosser.Model.MarkupViewModel;
import edu.bloomu.codeglosser.Utils.DocumentHelper;
import edu.bloomu.codeglosser.Utils.HTMLGenerator;
import edu.bloomu.codeglosser.View.MarkupView1;
import io.reactivex.Observable;
import java.awt.Desktop;
import java.io.BufferedOutputStream;
import edu.bloomu.codeglosser.Utils.Bounds;
import edu.bloomu.codeglosser.Events.MarkupColorChangeEvent;
import edu.bloomu.codeglosser.Events.NoteSelectedChangeEvent;
import edu.bloomu.codeglosser.Model.Markup;
import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.text.Document;
import org.openide.util.Exceptions;

/**
 *
 * @author Louis
 */
public class GlosserController {

    private static final Logger LOG = Logger.getLogger(GlosserController.class.getName());
    
    private final HashMap<String, Markup> markupMap = new HashMap<>();
    
    public GlosserController(Observable<Event> source) {
        source
                .filter(this::eventForUs)
                .subscribe(e -> {
                    switch (e.getSender()) {
                        case Event.MARKUP_VIEW:
                            switch (e.getCustom()) {
                                // Handle MarkupView's events
                                case MarkupView1.CREATE_MARKUP:
                                    createMarkup((Bounds []) e.data);
                                    break;
                                case MarkupView1.DELETE_MARKUP:
                                    deleteMarkup((Bounds []) e.data);
                                    break;
                                case MarkupView1.EXPORT_PROJECT:
                                    exportProject();
                                    break;
                                case MarkupView1.PREVIEW_HTML:
                                    previewHTML((MarkupViewModel) e.data);
                                    break;
                                case MarkupView1.SAVE_SESSION:
                                    saveSession();
                                    break;
                            }
                            break;
                        case Event.MARKUP_PROPERTIES:
                            switch (e.getCustom()) {
                                // TODO
                            }
                    }
                });
        
        view.onPreviewHTML()
                .subscribe((ignored) -> {
                    String html = HTMLGenerator.generate(model.getTitle(), model.getText(), manager.getAllNotes());
                    try {
                        File f = new File("tmp.html");
                        f.createNewFile();
                        BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(f));
                        stream.write(html.getBytes());
                        stream.flush();
                        stream.close();
                        Desktop.getDesktop().browse(f.toURI());
                    } catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                });
        
        view.onCreateSelection()
                .doOnNext((b) -> LOG.info("onShowSelection: " + b.toString()))
                .map(model::segmentRange)
                .doOnNext(view::addMarkup)
                .map(b -> manager.createNote(b))
                .map(NoteSelectedChangeEvent::of)
                .subscribe(bus::post);
        
        view.onDeleteSelection()
                .doOnNext(b -> LOG.info("onDeleteSelection: " + b.toString()))
                .map(b -> manager.getNote(b))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(Markup::getRange)
                .doOnNext(view::removeMarkup)
                .subscribe(b -> manager.deleteNote(b));
        
        view.onShowSelection()
                .doOnNext((b) -> LOG.info("onShowSelection: " + b.toString()))
                .map(b -> manager.getNote(b))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(NoteSelectedChangeEvent::of)
                .subscribe(bus::post);
        
        view.onTripleClick()
                .doOnNext(offset -> LOG.info("onTripleClick: " + offset))
                .map(model::getLineBounds)
                .subscribe(view::setSelection);
    }
    
    /**
     * Predicate to determine if the event sent was meant for us.
     * @param e Event
     * @return If meant for us
     */
    private boolean eventForUs(Event e) {
        return (e.getRecipient() & Event.MARKUP_CONTROLLER) != 0;
    }
    
    @Subscribe
    public void highlightColorChange(MarkupColorChangeEvent e) {
        view.setMarkupColor(e.getBounds(), e.getColor());
    }
    
    @Subscribe
    public void noteSelectedChange(NoteSelectedChangeEvent e) {
        if (e.getNote() != Markup.DEFAULT) {
            view.setSelection(e.getNote().getRange());
        }
    }
    
    @Subscribe
    public void handleFileChange(FileChangeEvent event) {
        manager = MarkupManager.getInstance(event.getFileName());
        view.removeAllMarkups();
        try {
            model.setText(event.getFileContents());
            view.setText(model.toHTML());
            manager.getAllNotes()
                    .stream()
                    .peek(n -> view.addMarkup(n.getOffsets()))
                    .forEach((Markup n) -> view.setMarkupColor(n.getRange(), n.getHighlightColor()));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void createMarkup(Bounds[] bounds) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void deleteMarkup(Bounds[] bounds) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * Handler for generating the HTML exportation of a single file. The current
     * file open is the only one marked up; if marking up an entire project is desired,
     * see "exportProject" instead. This method will launch the default browser
     * for the user and display a preview.
     * @param model The model used for generating HTML.
     */
    private void previewHTML(MarkupViewModel model) {
        LOG.info("Generating HTML preview for " + model.getTitle());
        String html = HTMLGenerator.generate(
                model.getTitle(), model.getText(), 
                markupMap.values().stream().collect(Collectors.toList())
        );
        
        // Show in browser.
        LOG.info("Displaying preview in browser...");
        try {
            File f = new File("tmp.html");
            f.createNewFile();
            BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(f));
            stream.write(html.getBytes());
            stream.flush();
            stream.close();
            Desktop.getDesktop().browse(f.toURI());
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private void exportProject() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void saveSession() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
