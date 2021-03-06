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
import edu.bloomu.codeglosser.Events.EventBus;
import edu.bloomu.codeglosser.Globals;
import edu.bloomu.codeglosser.Model.MarkupViewModel;
import edu.bloomu.codeglosser.Utils.HTMLGenerator;
import edu.bloomu.codeglosser.View.MarkupView;
import io.reactivex.Observable;
import java.awt.Desktop;
import java.io.BufferedOutputStream;
import edu.bloomu.codeglosser.Utils.Bounds;
import edu.bloomu.codeglosser.Model.Markup;
import edu.bloomu.codeglosser.Utils.IdentifierGenerator;
import edu.bloomu.codeglosser.Utils.SessionManager;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.javatuples.Pair;
import org.javatuples.Unit;
import edu.bloomu.codeglosser.Events.EventProcessor;
import edu.bloomu.codeglosser.Utils.Toast;
import javax.swing.JOptionPane;

/**
 *
 * @author Louis
 */
public class MarkupController implements EventProcessor {
    
    // MarkupProperties events
    public static final String NEW_MARKUP = "New Markup";
    public static final String REMOVE_MARKUP = "Remove Markup";
    public static final String DISPLAY_MARKUP = "Display Markup";
    public static final String RESTORE_MARKUPS = "Restore Markup";
    public static final String SELECTED_ID_RESPONSE = "(Response) Sending Requested Markup";
    
    // MarkupView events
    public static final String REMOVE_HIGHLIGHTS = "Remove Highlights";
    public static final String CHANGE_HIGHLIGHT_COLOR = "Change Highlight Color";
    public static final String SET_CURSOR = "Set Cursor Position";
    public static final String FILE_SELECTED = "Display Selected File";
    
    private static final Logger LOG = Globals.LOGGER;
    
    private final HashMap<String, Markup> markupMap = new HashMap<>();
    private Markup currentMarkup = null;
    
    // Handles generation of the identifiers for markups. All markups must have a unique
    // identifier for it to map correctly, so the current tag must be unique.
    private final IdentifierGenerator idGen = new IdentifierGenerator("Markup");
    
    private final EventBus engine = new EventBus(this, Event.MARKUP_CONTROLLER);
    
    public MarkupController() {
        
    }
    
    @Override
    public Observable<Event> process(Event e) {
        switch (e.sender) {
            case Event.MARKUP_VIEW:
                switch (e.descriptor) {
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
                switch (e.descriptor) {
                    case MarkupProperties.FILE_SELECTED:
                        return fileSelected((Path) e.data);
                    case MarkupProperties.APPLY_TEMPLATE:
                        return applyTemplate((Markup) e.data);
                    case MarkupProperties.SELECTED_ID:
                        return selectedId((String) e.data);
                    default:
                        throw new RuntimeException("Bad Custom Tag for MarkupProperties!");
                }
            default:
                throw new RuntimeException("Bad Sender!");
        }
    }

    @Override
    public EventBus getEventEngine() {
        return engine;
    }
    
    private Observable<Event> selectedId(String id) {
        LOG.fine("Handling event for id selection: " + id);
        
        // MarkupView must update its cursor and MarkupProperties needs to be notified
        return Observable
                .just(id)
                // Check if it is currently selected
                .filter(id_ -> !id_.equals(Markup.DEFAULT.getId()) && (currentMarkup == null || !currentMarkup.getId().equals(id_)))
                // Find the markup's range
                .map(markupMap::get)
                // Set as currently selected
                .doOnNext(markup -> currentMarkup = markup)
                .flatMap(markup -> Observable.just(
                        Event.of(Event.MARKUP_CONTROLLER, Event.MARKUP_VIEW, SET_CURSOR, markup.getRange()),
                        Event.of(Event.MARKUP_CONTROLLER, Event.MARKUP_PROPERTIES, SELECTED_ID_RESPONSE, markup)
                ));
    }
    
    private Observable<Event> applyTemplate(Markup template) {
        // If there is no currently selected markup, we can't do anything.
        if (currentMarkup == null) {
            return Observable.empty();
        }
        
        ArrayList<Event> events = new ArrayList<>();
        Color c = template.getHighlightColor();
        String msg = template.getMsg();
        if (c != null) {
            currentMarkup.setHighlightColor(c);
            events.add(Event.of(Event.MARKUP_CONTROLLER, Event.MARKUP_VIEW, CHANGE_HIGHLIGHT_COLOR, currentMarkup));
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
        LOG.fine("Creating markup with offsets... " + Arrays.toString(bounds));
        
        // Create a new markup. We must ensure each id is unique however.
        while (true) {
            String id = idGen.getNextId();
            if (markupMap.containsKey(id)) {
                continue;
            }
            
            currentMarkup = new Markup("", id, bounds);
            markupMap.put(id, currentMarkup);
            break;
        }
        
        
        // Notify the MarkupProperties to display new Markup.
        return Observable.just(Event.of(Event.MARKUP_CONTROLLER, Event.MARKUP_PROPERTIES, NEW_MARKUP, currentMarkup));
    }
    
    /**
     * Delete the current markup, if one is selected. If one is selected, we inform
     * the MarkupView that it should delete it's highlights, and then tell MarkupProperties
     * to remove it's own attributes.
     */
    private Observable<Event> deleteMarkup() {
        LOG.fine("Deleting current markup...");
        
        Observable<Event> obs;
        
        // We can only delete the markup if one is currently selected
        if (currentMarkup != null) {
            // Remove markup and propagate event
            obs = Observable
                    .just(currentMarkup)
                    .doOnNext(markup -> LOG.log(Level.INFO, "Current Markup: {0}", markup))
                    .doOnNext(markup -> markupMap.remove(markup.getId()))
                    // Inform MarkupView and MarkupProperties
                    .flatMap(markup -> Observable.just(
                            Event.of(Event.MARKUP_CONTROLLER, Event.MARKUP_VIEW, REMOVE_HIGHLIGHTS, markup),
                            Event.of(Event.MARKUP_CONTROLLER, Event.MARKUP_PROPERTIES, REMOVE_MARKUP, markup)
                    ));
                    
            currentMarkup = null;
        } else {
            LOG.info("No current markup selected...");
            obs = Observable.empty();
        }
        
        return obs;
    }
    
    /**
     * Handler for generating the HTML exportation of a single file. The current
     * file open is the only one marked up; if marking up an entire project is desired,
     * see "exportProject" instead. This method will launch the default browser
     * for the user and display a preview.
     * @param model The model used for generating HTML.
     */
    private Observable<Event> previewHTML(MarkupViewModel model) {
        LOG.log(Level.FINE, "Generating HTML preview for {0}", model.getTitle());
        String html = HTMLGenerator.syntaxHighlight(
                model.getText(), model.getTitle(), 
                markupMap.values().stream().collect(Collectors.toList())
        ).blockingFirst();
        
        // Show in browser.
        LOG.fine("Displaying preview in browser...");
        try {
            File f = new File("tmp.html");
            f.createNewFile();
            BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(f));
            stream.write(html.getBytes());
            stream.flush();
            stream.close();
            Desktop.getDesktop().browse(f.toURI());
        } catch (IOException ex) {
            LOG.severe("Error attempting to display HTML Preview: " + ex.getMessage());
            JOptionPane.showMessageDialog(null, "Error attempting to display HTML Preview", "Error", JOptionPane.ERROR_MESSAGE);
        }
        
        return Observable.empty();
    }

    /**
     * Save the current and restore a previous session (if present) and display
     * the requested file contents. If a previous session of the current file exists,
     * it will overwrite it. If a previous session of the new file exists, it will be
     * loaded and the preserved markups are restored.
     * @param filePath Path to file
     * @return Observable
     */
    private Observable<Event> fileSelected(Path filePath) {        
        return Observable
                .just(filePath)
                // First, save the user's session. 
                .doOnNext(ignored -> SessionManager.saveSession(markupMap.values().stream().collect(Collectors.toList())))
                .doOnNext(path -> Globals.CURRENT_FILE = path)
                // Second, if there exists any, restore any preserved markups. 
                // To maintain this pipeline, we tuple the list of
                // restored markups with the current path, becoming of type 
                // Pair<Path, List<Markup>>.
                .map(path -> Pair.with(path, SessionManager.loadSession(path)))
                // Third, update the current file, as it is used in the the SessionManager.
                // Fourth, we begin reading in the requested file. We drop the path 
                // for it's files respective contents (as lines) as it is no longer needed,
                // becoming of type Pair<List<String>, List<Markup>>
                .map(pair -> pair.setAt0(Files.readAllLines(pair.getValue0())))
                // Fifth, as we have both the file contents and (potentially) the Markups,
                // we need to send the markups to MarkupProperties (for it to update itself)
                // and both contents and Markups to the MarkupView, as well as update our own. 
                // However, first we must reduce all lines into a single stream, becoming of type Pair<String, List<Markup>>
                .map(pair -> pair.setAt0(pair.getValue0().stream().collect(Collectors.joining("\n"))))
                .doOnNext(pair -> restoreMarkups(pair.getValue1()))
                .flatMap(pair -> {
                    List<Event> events = new ArrayList<>();
                    
                    // We only inform the MarkupProperties if and only if there are previous Markups to restore.
                    if (!pair.getValue1().isEmpty()) {
                        events.add(Event.of(Event.MARKUP_CONTROLLER, Event.MARKUP_PROPERTIES, RESTORE_MARKUPS, pair.getValue1()));
                    }
                    
                    // We always inform the MarkupView
                    events.add(Event.of(Event.MARKUP_CONTROLLER, Event.MARKUP_VIEW, FILE_SELECTED, pair));

                    return Observable.fromIterable(events);
                });
    }
    
    private Observable<Event> exportProject() {
        LOG.fine("Exporting project...");
        Toast.displayText("Exporting project to directory: " + Globals.PROJECT_FOLDER);
        
        HTMLGenerator.generateAll();
        return Observable.empty();
    }
    
    private Observable<Event> saveSession() {
        SessionManager.saveSession(markupMap.values().stream().collect(Collectors.toList()));
        
        return Observable.empty();
    }

    /**
     * Notify the MarkupView of the boundary of a markup that is in range of the
     * selection.
     * @param b Bounds to check.
     */
    private Observable<Event> getMarkupSelection(Bounds bounds) {
        LOG.log(Level.FINE, "Obtaining Markup Selection for boundary: {0}", bounds);
        
        // Return markup selections (if present) and notify MarkupProperties that selection changed
        return Observable
                .fromIterable(markupMap.values())
                // Obtain the range from start to end of markup and check for collision
                .filter(markup -> markup.getRange().collidesWith(bounds))
                // Note: There should only ever be one, but just in case.
                .take(1)
                // Set as current markup
                .doOnNext(markup -> currentMarkup = markup)
                // Broadcast events
                .flatMap(markup -> Observable.just(
                        Event.of(Event.MARKUP_CONTROLLER, Event.MARKUP_VIEW, SET_CURSOR, markup.getRange()),
                        Event.of(Event.MARKUP_CONTROLLER, Event.MARKUP_PROPERTIES, DISPLAY_MARKUP, markup)
                ));
    }

    private void restoreMarkups(List<Markup> markups) {
        markupMap.clear();
        
        if (markups.isEmpty()) {
            LOG.fine("No Markups to restore...");
            return;
        }
        
        markups.forEach(markup -> markupMap.put(markup.getId(), markup));
    }
}
