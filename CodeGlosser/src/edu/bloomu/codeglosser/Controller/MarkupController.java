/* BSD 3-Clause License
 *
 * Copyright (c) 2017, Louis Jenkins <LouisJenkinsCS@hotmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     - Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     - Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *
 *     - Neither the name of Louis Jenkins, Bloomsburg University nor the names of its 
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.bloomu.codeglosser.Controller;

import edu.bloomu.codeglosser.Events.Event;
import edu.bloomu.codeglosser.Model.MarkupViewModel;
import edu.bloomu.codeglosser.Utils.HTMLGenerator;
import edu.bloomu.codeglosser.View.MarkupView;
import io.reactivex.Observable;
import java.awt.Desktop;
import java.io.BufferedOutputStream;
import edu.bloomu.codeglosser.Utils.Bounds;
import edu.bloomu.codeglosser.Model.Markup;
import edu.bloomu.codeglosser.Utils.IdentifierGenerator;
import edu.bloomu.codeglosser.Utils.SwingScheduler;
import edu.bloomu.codeglosser.View.MarkupProperties;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.openide.util.Exceptions;

/**
 *
 * @author Louis
 */
public class MarkupController {
    
    // MarkupProperties events
    public static final int NEW_MARKUP = 0x1;
    public static final int REMOVE_MARKUP = 0x2;
    
    // MarkupView events
    public static final int REMOVE_HIGHLIGHTS = 0x1;
    public static final int CHANGE_HIGHLIGHT_COLOR = 0x2;
    public static final int SET_CURSOR = 0x3;
    public static final int FILE_SELECTED = 0x4;
    
    private static final Logger LOG = Logger.getLogger(MarkupController.class.getName());
    
    private final HashMap<String, Markup> markupMap = new HashMap<>();
    private Markup currentMarkup = null;
    
    // Handles generation of the identifiers for markups. All markups must have a unique
    // identifier for it to map correctly, so the current tag must be unique.
    private final IdentifierGenerator idGen = new IdentifierGenerator("Markup");
    
    // Our event multiplexer
    private final PublishSubject<Event> event = PublishSubject.create();
    
    public MarkupController() {
        // Handle receiving events
        event
                .filter(this::eventForUs)
                .doOnNext(e -> LOG.info("Processing Event..."))
                .flatMap(e -> {
                    switch (e.getSender()) {
                        case Event.MARKUP_VIEW:
                            switch (e.getCustom()) {
                                // Handle MarkupView's events
                                case MarkupView.CREATE_MARKUP:
                                    return createMarkup((Bounds []) e.data);
                                case MarkupView.DELETE_MARKUP:
                                    return deleteMarkup();
                                case MarkupView.GET_MARKUP_SELECTION:
                                     return getMarkupSelection((Bounds) e.data);
                                case MarkupView.EXPORT_PROJECT:
                                    return exportProject();
                                case MarkupView.PREVIEW_HTML:
                                    return previewHTML((MarkupViewModel) e.data);
                                case MarkupView.SAVE_SESSION:
                                    return saveSession();
                                default:
                                    throw new RuntimeException("Bad Custom Tag for MarkupView!");
                            }
                        case Event.MARKUP_PROPERTIES:
                            switch (e.getCustom()) {
                                case MarkupProperties.FILE_SELECTED:
                                    return fileSelected((Path) e.data);
                                case MarkupProperties.APPLY_TEMPLATE:
                                    return applyTemplate((Markup) e.data);
                                default:
                                    throw new RuntimeException("Bad Custom Tag for MarkupProperties!");
                            }
                        default:
                            throw new RuntimeException("Bad Sender!");
                    }
                })
                .subscribe(event::onNext);
    }
    
    /**
     * Predicate to determine if the event sent was meant for us.
     *
     * @param e Event
     * @return If meant for us
     */
    private boolean eventForUs(Event e) {
        return e.getSender() != Event.MARKUP_CONTROLLER && e.getRecipient() == Event.MARKUP_CONTROLLER;
    }
    
    /**
     * Registers the following observable as an event source. This must be called
     * to receive events from other components.
     * @param source 
     */
    public void addEventSource(Observable<Event> source) {
        source.filter(this::eventForUs).subscribe(event::onNext);
    }
    
    /**
     * Returns our own Subject as an event source for listeners. This must be used
     * to receive events from this component.
     * @return Our event source
     */
    public Observable<Event> getEventSource() {
        return event;
    }
    
    private Observable<Event> applyTemplate(Markup template) {
        ArrayList<Event> events = new ArrayList<>();
        Color c = template.getHighlightColor();
        String msg = template.getMsg();
        if (c != null) {
            currentMarkup.setHighlightColor(c);
            events.add(Event.of(Event.MARKUP_CONTROLLER, Event.MARKUP_VIEW, CHANGE_HIGHLIGHT_COLOR, c));
        }
        
        if (msg != null) {
            currentMarkup.setMsg(msg);
        }
        
        return Observable.fromIterable(events);
    }
    
    /**
     * Handle events to create a new markup. The new markup is given a unique identifier
     * and as well becomes the current selected Markup. This event is also forwarded to
     * the MarkupProperties component.
     * @param bounds Boundary of the created markup.
     */
    private Observable<Event> createMarkup(Bounds[] bounds) {
        LOG.info("Creating markup with offsets... " + bounds);
        // Create a new markup
        String id = idGen.getNextId();
        currentMarkup = new Markup("", id, bounds);
        markupMap.put(id, currentMarkup);
        
        // Notify the MarkupProperties to display new Markup.
        sendEventToProperties(NEW_MARKUP, currentMarkup);
        
        // TODO
        return null;
    }
    
    /**
     * Delete the current markup, if one is selected. If one is selected, we inform
     * the MarkupView that it should delete it's highlights, and then tell MarkupProperties
     * to remove it's own attributes.
     */
    private Observable<Event> deleteMarkup() {
        LOG.info("Deleting current markup...");
        // We can only delete the markup if one is currently selected
        if (currentMarkup != null) {
            LOG.log(Level.INFO, "Current Markup: {0}", currentMarkup.getId());
            // Broadcast event
            sendEventToView(REMOVE_HIGHLIGHTS, currentMarkup);
            sendEventToProperties(REMOVE_MARKUP, currentMarkup);
            
            // Remove markup
            markupMap.remove(currentMarkup.getId());
            currentMarkup = null;
        } else {
            LOG.info("No current markup selected...");
        }
        
        // TODO
        return null;
    }
    
    /**
     * Handler for generating the HTML exportation of a single file. The current
     * file open is the only one marked up; if marking up an entire project is desired,
     * see "exportProject" instead. This method will launch the default browser
     * for the user and display a preview.
     * @param model The model used for generating HTML.
     */
    private Observable<Event> previewHTML(MarkupViewModel model) {
        LOG.log(Level.INFO, "Generating HTML preview for {0}", model.getTitle());
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
        
        // TODO
        return null;
    }

    private Observable<Event> fileSelected(Path filePath) {
        LOG.info("Handling file change event for " + filePath);
        
        // Notify MarkupView of file
        return Observable
                .just(filePath)
                // Handle reading on IO thread
                .observeOn(Schedulers.io())
                .map(Files::readAllLines)
                // Handle computation on computation thread
                .observeOn(Schedulers.computation())
                .map(list -> list.stream().collect(Collectors.joining("\n")))
                .map(fileContents -> Event.of(Event.MARKUP_CONTROLLER, Event.MARKUP_VIEW, FILE_SELECTED, fileContents))
                // Switch back to Swing UI thread
                .observeOn(SwingScheduler.getInstance());
    }
    
    private Observable<Event> exportProject() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    private Observable<Event> saveSession() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void sendEventToProperties(int eventTag, Object data) {
        event.onNext(Event.of(Event.MARKUP_CONTROLLER, Event.MARKUP_PROPERTIES, eventTag, data));
    }
    
    private void sendEventToView(int eventTag, Object data) {
        event.onNext(Event.of(Event.MARKUP_CONTROLLER, Event.MARKUP_VIEW, eventTag, data));
    }

    /**
     * Notify the MarkupView of the boundary of a markup that is in range of the
     * selection.
     * @param b Bounds to check.
     */
    private Observable<Event> getMarkupSelection(Bounds b) {
        LOG.log(Level.INFO, "Obtaining Markup Selection for boundary: {0}", b);
        markupMap
                .values()
                .stream()
                .map(Markup::getRange)
                .filter(bounds -> !bounds.collidesWith(b))
                .findAny()
                .ifPresent(bounds -> sendEventToView(SET_CURSOR, bounds));
        
        // TODO
        return null;
    }
}
