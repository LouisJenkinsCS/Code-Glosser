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
import edu.bloomu.codeglosser.View.MarkupView1;
import io.reactivex.Observable;
import java.awt.Desktop;
import java.io.BufferedOutputStream;
import edu.bloomu.codeglosser.Utils.Bounds;
import edu.bloomu.codeglosser.Model.Markup;
import edu.bloomu.codeglosser.Utils.IdentifierGenerator;
import io.reactivex.subjects.PublishSubject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
    public static final int NEW_MARKUP = 1 << 0;
    public static final int REMOVE_MARKUP = 1 << 1;
    
    // MarkupView events
    public static final int REMOVE_HIGHLIGHTS = 1 << 0;
    public static final int SET_CURSOR = 1 << 1;
    
    private static final Logger LOG = Logger.getLogger(MarkupController.class.getName());
    
    private final HashMap<String, Markup> markupMap = new HashMap<>();
    private Markup currentMarkup = null;
    
    // Handles generation of the identifiers for markups. All markups must have a unique
    // identifier for it to map correctly, so the current tag must be unique.
    private final IdentifierGenerator idGen = new IdentifierGenerator("Markup");
    
    // Our event multiplexer
    private final PublishSubject event = PublishSubject.create();
    
    public MarkupController(Observable<Event> source) {
        // Handle receiving events
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
                                    deleteMarkup();
                                    break;
                                case MarkupView1.GET_MARKUP_SELECTION:
                                     getMarkupSelection((Bounds) e.data);
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
    }
    
    /**
     * Predicate to determine if the event sent was meant for us.
     * @param e Event
     * @return If meant for us
     */
    private boolean eventForUs(Event e) {
        return (e.getRecipient() & Event.MARKUP_CONTROLLER) != 0;
    }
    
    /**
     * Handle events to create a new markup. The new markup is given a unique identifier
     * and as well becomes the current selected Markup. This event is also forwarded to
     * the MarkupProperties component.
     * @param bounds Boundary of the created markup.
     */
    private void createMarkup(Bounds[] bounds) {
        LOG.info("Creating markup with offsets... " + bounds);
        // Create a new markup
        String id = idGen.getNextId();
        currentMarkup = new Markup("", id, bounds);
        markupMap.put(id, currentMarkup);
        
        // Notify the MarkupProperties to display new Markup.
        sendEventToProperties(NEW_MARKUP, currentMarkup);
    }
    
    /**
     * Delete the current markup, if one is selected. If one is selected, we inform
     * the MarkupView that it should delete it's highlights, and then tell MarkupProperties
     * to remove it's own attributes.
     */
    private void deleteMarkup() {
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
    }
    
    /**
     * Handler for generating the HTML exportation of a single file. The current
     * file open is the only one marked up; if marking up an entire project is desired,
     * see "exportProject" instead. This method will launch the default browser
     * for the user and display a preview.
     * @param model The model used for generating HTML.
     */
    private void previewHTML(MarkupViewModel model) {
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
    }

    private void exportProject() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void saveSession() {
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
    private void getMarkupSelection(Bounds b) {
        LOG.log(Level.INFO, "Obtaining Markup Selection for boundary: {0}", b);
        markupMap
                .values()
                .stream()
                .filter(markup -> markup.inRange(b))
                .map(Markup::getRange)
                .findAny()
                .ifPresent(bounds -> sendEventToView(SET_CURSOR, bounds));
    }
}
