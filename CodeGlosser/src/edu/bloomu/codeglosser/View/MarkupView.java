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
package edu.bloomu.codeglosser.View;

import edu.bloomu.codeglosser.Controller.MarkupController;
import edu.bloomu.codeglosser.Utils.Bounds;
import java.awt.Color;
import java.util.HashMap;
import java.util.logging.Logger;
import edu.bloomu.codeglosser.Events.Event;
import edu.bloomu.codeglosser.Events.EventEngine;
import edu.bloomu.codeglosser.Events.EventHandler;
import edu.bloomu.codeglosser.Exceptions.InvalidTextSelectionException;
import edu.bloomu.codeglosser.HTML.Java2HTML;
import edu.bloomu.codeglosser.Model.Markup;
import edu.bloomu.codeglosser.Model.MarkupViewModel;
import edu.bloomu.codeglosser.Utils.ColorUtils;
import edu.bloomu.codeglosser.Utils.SwingScheduler;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Stream;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;
import javax.swing.text.Highlighter;
import javax.swing.text.Highlighter.Highlight;
import javax.swing.text.Highlighter.HighlightPainter;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import org.javatuples.Pair;

/**
 *
 * @author Louis
 *
 * The MarkupView is responsible for presentation of the code to be marked up,
 * as well as supplying highlighting (and syntax highlighting). The MarkupView
 * also contains a contextual pop-up menu that is one of the primers for the
 * event propagation machine.
 *
 * The MarkupView is, currently, only connected to the MarkupController.
 *
 */
public class MarkupView extends javax.swing.JPanel implements EventHandler {

    // The events we send
    public static final int CREATE_MARKUP = 0x1;
    public static final int DELETE_MARKUP = 0x2;
    public static final int GET_MARKUP_SELECTION = 0x3;
    public static final int SAVE_SESSION = 0x4;
    public static final int PREVIEW_HTML = 0x5;
    public static final int EXPORT_PROJECT = 0x6;

    private static final Logger LOG = Logger.getLogger(MarkupView.class.getName());

    // The model for this view; the model handles the computational work such as segmentation of highlighting
    private final MarkupViewModel model = new MarkupViewModel();

    private final EventEngine engine = new EventEngine(this, Event.MARKUP_VIEW);

    // The highlighter and it's respective mapping of highlights
    private final Highlighter highlighter;
    private final HashMap<Bounds, Highlight> highlightMap = new HashMap<>();

    // Our contextual popup menu
    private final JPopupMenu popup = new JPopupMenu();

    public MarkupView() {
        LOG.info("Initializing MarkupView...");

        // Initialize NetBeans' generated GUI components
        initComponents();

        // Initialize our own GUI components
        highlighter = textCode.getHighlighter();
        textCode.setEditable(false);
        initializePopup();
        initializeView();
        initializeListeners();
    }
    
    private void initializeView() {
        StyleSheet stylesheet = new StyleSheet();
        stylesheet.addRule(".hljs {\n" +
                "  display: block;\n" +
                "  overflow-x: auto;\n" +
                "  padding: 0.5em;\n" +
                "  color: #383a42;\n" +
                "  background: #fafafa;\n" +
                "}\n" +
                "\n" +
                ".hljs-comment,\n" +
                ".hljs-quote {\n" +
                "  color: #a0a1a7;\n" +
                "  font-style: italic;\n" +
                "}\n" +
                "\n" +
                ".hljs-doctag,\n" +
                ".hljs-keyword,\n" +
                ".hljs-formula {\n" +
                "  color: #a626a4;\n" +
                "}\n" +
                "\n" +
                ".hljs-section,\n" +
                ".hljs-name,\n" +
                ".hljs-selector-tag,\n" +
                ".hljs-deletion,\n" +
                ".hljs-subst {\n" +
                "  color: #e45649;\n" +
                "}\n" +
                "\n" +
                ".hljs-literal {\n" +
                "  color: #0184bb;\n" +
                "}\n" +
                "\n" +
                ".hljs-string,\n" +
                ".hljs-regexp,\n" +
                ".hljs-addition,\n" +
                ".hljs-attribute,\n" +
                ".hljs-meta-string {\n" +
                "  color: #50a14f;\n" +
                "}\n" +
                "\n" +
                ".hljs-built_in,\n" +
                ".hljs-class .hljs-title {\n" +
                "  color: #c18401;\n" +
                "}\n" +
                "\n" +
                ".hljs-attr,\n" +
                ".hljs-variable,\n" +
                ".hljs-template-variable,\n" +
                ".hljs-type,\n" +
                ".hljs-selector-class,\n" +
                ".hljs-selector-attr,\n" +
                ".hljs-selector-pseudo,\n" +
                ".hljs-number {\n" +
                "  color: #986801;\n" +
                "}\n" +
                "\n" +
                ".hljs-symbol,\n" +
                ".hljs-bullet,\n" +
                ".hljs-link,\n" +
                ".hljs-meta,\n" +
                ".hljs-selector-id,\n" +
                ".hljs-title {\n" +
                "  color: #4078f2;\n" +
                "}\n" +
                "\n" +
                ".hljs-emphasis {\n" +
                "  font-style: italic;\n" +
                "}\n" +
                "\n" +
                ".hljs-strong {\n" +
                "  font-weight: bold;\n" +
                "}\n" +
                "\n" +
                ".hljs-link {\n" +
                "  text-decoration: underline;\n" +
                "}\n" +
                "</style>"
        );
        
        HTMLEditorKit htmlEditorKit = new HTMLEditorKit();
        htmlEditorKit.setStyleSheet(stylesheet);
        HTMLDocument htmlDocument = (HTMLDocument) htmlEditorKit.createDefaultDocument();
        textCode.setEditorKit(htmlEditorKit);
        textCode.setDocument(htmlDocument);
    }

    @Override
    public Observable<Event> handleEvent(Event e) {
        switch (e.getSender()) {
            case Event.MARKUP_CONTROLLER:
                switch (e.getCustom()) {
                    case MarkupController.REMOVE_HIGHLIGHTS:
                        return removeHighlight((Markup) e.data);
                    case MarkupController.CHANGE_HIGHLIGHT_COLOR:
                        return changeHighlightColor((Markup) e.data);
                    case MarkupController.SET_CURSOR:
                        return setCursorPosition((Bounds) e.data);
                    case MarkupController.FILE_SELECTED:
                        return fileSelected((Pair<String, List<Markup>>) e.data);
                    default:
                        throw new RuntimeException("Bad Custom Tag!");
                }
            default:
                throw new RuntimeException("Bad Sender!");
        }
    }

    @Override
    public EventEngine getEventEngine() {
        return engine;
    }
    
    private void initializePopup() {
        LOG.finer("Initializing PopupMenu...");

        // Create all menu items
        JMenuItem deleteMarkup = new JMenuItem("Delete Markup");
        JMenuItem createMarkup = new JMenuItem("Create Markup");
        JMenuItem preview = new JMenuItem("Preview");
        JMenuItem exportProject = new JMenuItem("Export Project");
        JMenuItem saveSession = new JMenuItem("Save Session");

        // Assign callback of menu items
        createMarkup.addActionListener(e -> {
            Bounds b = getSelected();
            LOG.log(Level.INFO, "Creating Markup of range: {0}-{1}", new Object[]{b.getStart(), b.getEnd()});

            // Only send event if we do not have a current highlight for it (meaning it already exists)
            if (highlightExists(b)) {
                LOG.info("Markup already exists...");
                return;
            }
            
            try {
                // Segment boundary
                Bounds[] bounds = model.segmentRange(b);
                addMarkup(Color.YELLOW, bounds);
                engine.broadcast(Event.MARKUP_CONTROLLER, CREATE_MARKUP, bounds);
            } catch (InvalidTextSelectionException ex) {
                LOG.severe("Bad Bounds!!!");
                return;
            }
            
        });

        deleteMarkup.addActionListener(e -> {
            LOG.info("Propogating DELETE_MARKUP event...");
            engine.broadcast(Event.MARKUP_CONTROLLER, DELETE_MARKUP, null);
        });

        preview.addActionListener(e -> {
            LOG.info("Propogating PREVIEW_HTML event...");
            engine.broadcast(Event.MARKUP_CONTROLLER, PREVIEW_HTML, model);
        });

        exportProject.addActionListener(e -> {
            LOG.info("Propogating EXPORT_PROJECT event...");
            engine.broadcast(Event.MARKUP_CONTROLLER, EXPORT_PROJECT, null);
        });

        saveSession.addActionListener(e -> {
            LOG.info("Propogating SAVE_SESSION event...");
            engine.broadcast(Event.MARKUP_CONTROLLER, SAVE_SESSION, null);
        });

        // Add all menu items to the popup menu.
        popup.add(createMarkup);
        popup.add(deleteMarkup);
        popup.add(preview);
        popup.add(exportProject);
        popup.add(saveSession);
    }
    
    private void initializeListeners() {
        // We handle displaying the contextual menu, as well certain other actions detailed below,
        // based on the user's click events. Since the 'popup trigger' is dependent on the OS,
        // the safest way is to check all mouse events for a popup trigger event and handle it
        // accordingly.
        textCode.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                // We show popups on the architectural-dependent popup trigger, I.E: Right Click
                if (e.isPopupTrigger()) {
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }

                // On Double Click: If the user's selection contains a markup, the
                // controller will respond with an event telling us to update the
                // cursor's range to that of the markup itself.
                if (e.getClickCount() == 2 && !e.isConsumed()) {
                    LOG.info("Double Click!");
                    doubleClickHandler(() -> e.consume());
                }

                // On Triple Click: Changes the cursor's range to the entire line.
                if (e.getClickCount() >= 3 && !e.isConsumed()) {
                    LOG.info("Triple Click!");
                    tripleClickHandler();
                    e.consume();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }

    /**
     * Converts the range of currently selected text to Bounds.
     *
     * @return Bounds of currently selected text.
     */
    private Bounds getSelected() {
        return Bounds.of(textCode.getSelectionStart(), textCode.getSelectionEnd());
    }

    /**
     * Helper to determine if a highlight already exists within the given range.
     *
     * @param bounds Boundary to check
     * @return If any highlight already exists
     */
    private boolean highlightExists(Bounds bounds) {
        return highlightMap.keySet().stream().anyMatch(bounds::collidesWith);
    }

    public void setText(String str) {
        LOG.info("Text changed...");
        textCode.setText("<html><head></head><body style=\"font-size: 1.15em\"><pre><code>" + str.trim() + "</code></pre></body></html>");
    }

    /**
     * On a double click, it will attempt to locate a markup that is within the
     * selected boundary, and if one exists it will notify the MarkupController
     * about it. If one does not exist, it simply ignores the user request. For
     * simplicity, if we find one, we consume the click event so as not to
     * trigger a triple click.
     */
    public void doubleClickHandler(Runnable consume) {
        // We only consume the MouseEvent and inform MarkupController
        // if double click refers to a valid highlight
        if (highlightExists(getSelected())) {
            consume.run();
            engine.broadcast(Event.MARKUP_CONTROLLER, GET_MARKUP_SELECTION, getSelected());
        }
    }

    /**
     * On a triple click, it will refocus the cursor to select the current line,
     * excluding additional white space.
     *
     * TODO: Perform work on background thread???
     */
    public void tripleClickHandler() {
        setSelection(model.getLineBounds(textCode.getSelectionStart()));
    }

    public void addMarkup(Color color, Bounds... bounds) {
        LOG.info(Stream
                .of(bounds)
                .map(Bounds::toString)
                .reduce("Adding Markups: {", (s1, s2) -> s1 + "\n\t" + s2)
                .concat("\n}")
        );
        Stream.of(bounds)
                .filter((b) -> b.getStart() != b.getEnd())
                .forEach((b) -> {
                    Highlight highlight = null;
                    try {
                        highlight = (Highlight) highlighter.addHighlight(b.getStart(), b.getEnd(), new DefaultHighlightPainter(ColorUtils.makeTransparent(color)));
                    } catch (BadLocationException ex) {
                        LOG.throwing(this.getClass().getName(), "addMarkup", ex);
                    }
                    highlightMap.put(b, highlight);
                });
    }

    public Observable<Event> removeMarkup(Bounds... bounds) {
        LOG.info(Stream
                .of(bounds)
                .map(Bounds::toString)
                .reduce("Removing Markups: {", (s1, s2) -> s1 + "\n\t" + s2)
                .concat("\n}")
        );

        // Remove any highlights within requested bounds
        return Observable
                .fromIterable(highlightMap.keySet())
                // Perform the work on background thread
                .observeOn(Schedulers.computation())
                // Find any highlights that match the request and remove them
                .filter(bound -> Stream.of(bounds).anyMatch(bound::collidesWith))
                .map(highlightMap::remove)
                // Remove the visual highlights on UI Thread
                .observeOn(SwingScheduler.getInstance())
                .doOnNext(highlighter::removeHighlight)
                // We have handled the event, so no need for anything else.
                .flatMap(Event::empty);
    }

    public void removeAllMarkups() {
        LOG.info("Removing all markups!");
        highlightMap.values().stream()
                .forEach(highlighter::removeHighlight);
        highlightMap.clear();
    }
    
    private Observable<Event> changeHighlightColor(Markup markup) {
        setMarkupColor(markup.getRange(), markup.getHighlightColor());
        
        return Observable.empty();
    }

    public void setMarkupColor(Bounds bounds, Color color) {
        // Save the currently selected offsets
        int start = textCode.getSelectionStart();
        int end = textCode.getSelectionEnd();

        // Remove selection
        textCode.setSelectionStart(start);
        textCode.setSelectionEnd(start);

        Stream.of(highlighter.getHighlights())
                .filter((h) -> Bounds.of(h.getStartOffset(), h.getEndOffset()).collidesWith(bounds))
                .forEach((h) -> {
                    Bounds b = Bounds.of(h.getStartOffset(), h.getEndOffset());
                    highlighter.removeHighlight(h);
                    HighlightPainter hp = new DefaultHighlightPainter(ColorUtils.makeTransparent(color));
                    try {
                        Highlight h1 = (Highlight) highlighter.addHighlight(b.getStart(), b.getEnd(), hp);
                        highlightMap.put(b, h1);
                    } catch (BadLocationException ex) {
                        ex.printStackTrace();
                    }
                });

        // Restore saved offsets
        if (start != 0 || end != 0) {
            setSelection(Bounds.of(start, end));
        }
    }

    public void setSelection(Bounds bounds) {
        this.requestFocus(true);
        textCode.setSelectionStart(bounds.getStart());
        textCode.setSelectionEnd(bounds.getEnd());
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        textCode = new javax.swing.JTextPane();

        setLayout(new java.awt.BorderLayout());

        textCode.setEditable(false);
        textCode.setContentType("text/html"); // NOI18N
        jScrollPane1.setViewportView(textCode);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 781, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 592, Short.MAX_VALUE)
        );

        add(jPanel1, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextPane textCode;
    // End of variables declaration//GEN-END:variables

    /**
     * Sets the current cursor position. Used for displaying to the user which markup
     * was recently selected.
     * @param bounds Cursor position to set.
     * @return Pipelined instructions
     */
    private Observable<Event> setCursorPosition(Bounds bounds) {
        LOG.info("Setting Cursor Position: " + bounds);
        
        return Observable
                .just(bounds)
                // Adjust cursor
                .doOnNext(this::setSelection)
                // We have handled the event, so no need for anything else.
                .flatMap(Event::empty);
                
    }
    
    private Observable<Event> fileSelected(Pair<String, List<Markup>> pair) {
        return Observable
                .just(pair)
                // Handle syntax highlighting in background
                .observeOn(Schedulers.computation())
                .doOnNext(p -> model.setText(p.getValue0()))
                .flatMap(p -> new Java2HTML()
                        .translate(p.getValue0())
                        .map(p::setAt0)
                )
                // Display text on UI Thread
                .observeOn(SwingScheduler.getInstance())
                .doOnNext(p -> setText(p.getValue0()))
                // Set highlights
                .map(Pair::getValue1)
                .flatMap(Observable::fromIterable)
                .doOnNext(markup -> addMarkup(markup.getHighlightColor(), markup.getOffsets()))
                // We have handled the event, so no need for anything else.
                .flatMap(Event::empty);
    }
    
    /**
     * Removes the requested markup's highlights. All highlights that the markup
     * contains are removed from both the highlight map and from the GUI as well,
     * with the latter being performed in the background.
     * @param markup Markup to remove
     * @return Pipelined instructions
     */
    private Observable<Event> removeHighlight(Markup markup) {
        LOG.info(Stream
                .of(markup.getOffsets())
                .map(Bounds::toString)
                .reduce("Removing Markups: {", (s1, s2) -> s1 + "\n\t" + s2)
                .concat("\n}")
        );

        // Remove any highlights within requested bounds
        return Observable
                .fromIterable(highlightMap.keySet())
                // Perform the work on background thread
                .observeOn(Schedulers.computation())
                // Find any highlights that match the request and remove them
                .filter(markup::inRange)
                .map(highlightMap::remove)
                // Remove the visual highlights on UI Thread
                .observeOn(SwingScheduler.getInstance())
                .doOnNext(highlighter::removeHighlight)
                // We have handled the event, so no need for anything else.
                .flatMap(Event::empty);
    }

}
