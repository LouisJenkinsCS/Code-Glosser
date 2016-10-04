package edu.bloomu.codeglosser;

import static com.google.common.collect.ComparisonChain.start;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;
import javax.swing.text.Highlighter;
import javax.swing.text.Highlighter.Highlight;
import javax.swing.text.Highlighter.HighlightPainter;
import de.java2html.Java2Html;
import de.java2html.javasource.JavaSourceType;
import javax.swing.JTextPane;
import de.java2html.options.JavaSourceConversionOptions;
import de.java2html.options.JavaSourceStyleEntry;
import de.java2html.util.RGB;
import edu.bloomu.codeglosser.Controller.NoteManager;
import edu.bloomu.codeglosser.Controller.NotepadView;
import edu.bloomu.codeglosser.Model.Note;
import edu.bloomu.codeglosser.Utils.Bounds;
import edu.bloomu.codeglosser.Utils.DocumentHelper;
import java.awt.Color;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.JOptionPane;
import javax.swing.text.Document;

/**
 * This class enables text to be glossed with highlights and associated comments. The
 * mouse is used to highlight text, which causes a comment editor to appear. Highlights
 * cannot overlap.
 *
 * An existing highlight can be selected by double-clicking, which opens the comment
 * editor.
 *
 * The pop-up trigger creates a pop-up menu with options for showing/saving all comments
 * and deleting the selected highlight and associated comment.
 *
 * @author Drue Coles
 */
public class GlossableTextArea extends JTextPane implements NotepadView {
    
    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(GlossableTextArea.class.getName());
    
    private static final String cssText = ".note {\n" +
                                    "  display: inline;\n" +
                                    "  position: relative;\n" +
                                    "  border-bottom: 1px dotted #0000FF\n" +
                                    "}\n" +
                                    "\n" +
                                    ".note:hover:after {\n" +
                                    "  background: #333;\n" +
                                    "  background: rgba(0,0,0,.8);\n" +
                                    "  border-radius: 5px;\n" +
                                    "  bottom: 26px;\n" +
                                    "  white-space: pre-wrap;\n" +
                                    "  color: #fff;\n" +
                                    "  content: attr(msg);\n" +
                                    "  left: 20%;\n" +
                                    "  padding: 5px 15px;\n" +
                                    "  position: absolute;\n" +
                                    "  z-index: 98;\n" +
                                    "  width:500px;\n" +
                                    "  display:block;\n" +
                                    "  word-wrap: normal;\n" +
                                    "}\n" +
                                    "\n" +
                                    ".note:hover:before{\n" +
                                    "  border:solid;\n" +
                                    "  border-color: #333 transparent;\n" +
                                    "  border-width: 6px 6px 0 6px;\n" +
                                    "  bottom: 20px;\n" +
                                    "  content: \"\";\n" +
                                    "  left: 50%;\n" +
                                    "  position: absolute;\n" +
                                    "  display:block;\n" +
                                    "  z-index: 99;\n" +
                                    "}";
    
    public String ReadOnlyHTML;
    
    private final HashMap<Point, ArrayList<Highlight>> highlightMap = new HashMap<>();
    
    // Highlighter and painter
    private final Highlighter highlighter = getHighlighter();
    private Color highlightColor = new Color(255, 255, 0, 150);
    private final HighlightPainter painter = new DefaultHighlightPainter(highlightColor);
    private Color selectedHighlightColor = new Color(20, 220, 60);
    private NoteManager controller;
    
    /**
     * Creates an unglossed text area.
     *
     * @param text the text to be glossed
     */
    public GlossableTextArea(Document doc) {
        JavaSourceConversionOptions options = JavaSourceConversionOptions.getDefault();
        options.getStyleTable().put(JavaSourceType.KEYWORD, new JavaSourceStyleEntry(RGB.BLUE, true, false));
        options.getStyleTable().put(JavaSourceType.STRING, new JavaSourceStyleEntry(new RGB(206, 133, 0)));
        options.getStyleTable().put(JavaSourceType.LINE_NUMBERS, new JavaSourceStyleEntry(RGB.BLACK));
        options.getStyleTable().put(JavaSourceType.NUM_CONSTANT, new JavaSourceStyleEntry(RGB.BLACK));
//        options.setAddLineAnchors(true);
//        options.setShowLineNumbers(true);
        options.getStyleTable().put(JavaSourceType.CODE_TYPE, new JavaSourceStyleEntry(RGB.BLUE));
        String text = Java2Html.convertToHtml(DocumentHelper.getText(doc), options);
        System.err.println(text);
        setContentType("text/html");
        setText((ReadOnlyHTML = "<html style='width:100%;height:100%;'> <style>" + cssText + "</style> <body>" + text + "</body></html>"));
        setMargin(new Insets(5, 5, 5, 5));
        setEditable(false);
//        setBackground(backgroundColor);
        Listener listener = new Listener(this);
        addMouseListener(listener);
        addMouseWheelListener(listener);
    }
    
    public void setController(NoteManager manager) {
        this.controller = manager;
    }

    /**
     * @return the color used for highlights
     */
    public Color getHighlightColor() {
        return highlightColor;
    }

    /**
     * Sets the color to be used to indicated a selected highlight.
     *
     * @param color
     */
    public void setHighlightColor(Color color) {
        this.highlightColor = color;
    }

    /**
     * @return the color used to indicate a selected highlight
     */
    public Color getSelectedHighlightColor() {
        return selectedHighlightColor;
    }

    /**
     * Changes the color used to indicate a selected highlight.
     *
     * @param color
     */
    public void setSelectedHighlightColor(Color color) {
        this.selectedHighlightColor = color;
    }
    
    public NoteManager getController() {
        return controller;
    }

    /**
     * Removes the currently selected highlight and comment.
     */
    public void removeHighlight() {
        Point p = new Point(getSelectionStart(), getSelectionEnd());
        ArrayList<Highlight> highlight = highlightMap.get(p);
        if (highlight != null) {
            highlightMap.remove(p);
            controller.deleteNote(p.x, p.y);
            setSelectionStart(0);
            setSelectionEnd(0);
        }
    }

    /**
     * Removes all highlights and associated comments.
     */
    public void removeAllHighlights() {
        highlighter.removeAllHighlights();
        controller.deleteAllNotes();
        highlightMap.clear();
    }
    
    public void saveGlossedDocument() {
        // TODO: Write save code.    
    }

    @Override
    public void addMarkup(Bounds ...bounds) {
        // TODO: FIX
        ArrayList<Highlight> highlighters = new ArrayList<>();
        try {
            String text = getText(start, end-start);
            logger.trace("Selected: {Begin: " + start + ",End: " + end + "}\nText: " + text);
            StringCharacterIterator iter = new StringCharacterIterator(text);
            int startOffset = 0, endOffset = 0;
            for (char c = iter.current(); c != CharacterIterator.DONE; c = iter.next()) {
                // Stop up to newline character
                if (c == '\n') {
                    // Minus newline character.
                    endOffset--;
                    
                    // Find last character before this reported newline...
                    StringCharacterIterator tmp = (StringCharacterIterator) iter.clone();
                    for (char ch = tmp.current(); Character.isSpaceChar(ch); ch = tmp.previous()) {
                        endOffset--;
                    }
                    
                    logger.trace("Newline Character at: " + start + endOffset);
                    if (endOffset != 0) {
                        highlighters.add((Highlight) highlighter.addHighlight(start + startOffset, start + endOffset, painter));
                    }
                    char ch;

                    // Consume any spaces or tabs after newline
                    while((ch = iter.next()) != CharacterIterator.DONE && !Character.isLetterOrDigit(ch)) {
                        endOffset++;
                    }
                    

                    if (ch == CharacterIterator.DONE) {
                        break;
                    }

                    // Update start offset.
                    startOffset = endOffset;
                    logger.trace("Continuing at: " + start + startOffset);
                } else {
                    endOffset++;
                }
            }
            // Only false when we are skipping ahead past newline characters
            if (startOffset < endOffset) {
                logger.trace("Added highlighter at: {Start: " + (start + startOffset) + ",End: " + (start + endOffset));
                highlighters.add((Highlight) highlighter.addHighlight(start + startOffset, start + endOffset, painter));
            }
        } catch (BadLocationException ex) {
            logger.catching(ex);
        }
        highlightMap.put(new Point(start, end), highlighters);
    }

    @Override
    public void removeMarkup(int start, int end) {
        Point p = new Point(getSelectionStart(), getSelectionEnd());
        ArrayList<Highlight> highlight = highlightMap.remove(p);
        if (highlight != null) {
            highlight.forEach(highlighter::removeHighlight);
        }
    }

    @Override
    public void setMarkupColor(Color color) {
        // Remove selected highlights from map
        Note n = controller.currentNote().get();
        Point p = new Point(n.getStart(), n.getEnd());
        highlightMap.remove(p);
        
        // Update list of selected highlights
        highlightMap.put(p, (ArrayList<Highlight>) Stream.of(highlighter.getHighlights())
                // Get all Highlights within the selected range
                .filter((h) -> h.getStartOffset() >= p.x && h.getEndOffset() <= p.y)
                // Intermediate Step: Remove the old highlights from parent object
                .peek(highlighter::removeHighlight)
                // Create new highlights from old highlights
                .map((h) -> {
                    HighlightPainter hp = new DefaultHighlightPainter(color);
                    try {
                        return (Highlight) highlighter.addHighlight(h.getStartOffset(), h.getEndOffset(), hp);
                    } catch (BadLocationException ex) {
                        throw new IllegalStateException("Unable to find highlight position: {" + h.getStartOffset() + "," + h.getEndOffset() + "}\n" + ex.getMessage());
                    }
                })
                // Collect new highlights to add to map.
                .collect(Collectors.toList()));
    }

}

/**
 * Handles mouse events in a GlossableTextArea.
 *
 * @author Drue Coles
 */
class Listener extends MouseAdapter {

    JPopupMenu popup;
    GlossableTextArea glossableTextArea;
    boolean mouseButtonDown = false;
    
    Listener(GlossableTextArea glossableTextArea) {
        this.glossableTextArea = glossableTextArea;
        popup = new JPopupMenu();
        JMenuItem showSelectedComment = new JMenuItem("Show selected comment");
        JMenuItem deleteSelectedComment = new JMenuItem("Delete selected comment");
        JMenuItem addNote = new JMenuItem("Add Note");
        popup.add(showSelectedComment);
        popup.add(deleteSelectedComment);
        popup.add(addNote);

        showSelectedComment.addActionListener((ActionEvent e) -> {
            NoteManager controller = glossableTextArea.getController();
            int start = glossableTextArea.getSelectionStart();
            int end = glossableTextArea.getSelectionEnd();
            controller.showNote(start, end);
        });

        deleteSelectedComment.addActionListener((ActionEvent e) -> {
            NoteManager controller = glossableTextArea.getController();
            controller.deleteCurrentNote();
        });
        
        addNote.addActionListener((event) -> {
            NoteManager controller = glossableTextArea.getController();
            int start = glossableTextArea.getSelectionStart();
            int end = glossableTextArea.getSelectionEnd();
            if (start != end && controller.isValidPosition(start, end)) {
                controller.createNote(start, end);
                controller.showNote(start, end);
            }
            glossableTextArea.getController().debug();
        });

        popup.addSeparator();
        JMenuItem deleteAllNotesItem = new JMenuItem("Delete all notes");
        popup.add(deleteAllNotesItem);

        deleteAllNotesItem.addActionListener((ActionEvent e) -> {
            NoteManager controller = glossableTextArea.getController();
            controller.deleteAllNotes();
        });

        JMenuItem saveAndExitMenuItem = new JMenuItem("Save and exit");
        popup.addSeparator();
        popup.add(saveAndExitMenuItem);

        saveAndExitMenuItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(null, "Not implemented!", "Saving and Restoring State", JOptionPane.PLAIN_MESSAGE);
            }
        });
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        mouseButtonDown = false;
        if (e.isPopupTrigger()) {
            popup.show(e.getComponent(), e.getX(), e.getY());
            return;
        }
//        int start = glossableTextArea.getSelectionStart();
//        int end = glossableTextArea.getSelectionEnd();
//        if (start != end) {
//            glossableTextArea.addHighlight(start, end);
//            glossableTextArea.setSelectionStart(end);
//            glossableTextArea.setSelectionEnd(end);
//        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.isPopupTrigger()) {
            popup.show(e.getComponent(), e.getX(), e.getY());
            return;
        }

        // Double click: select current highlight and open comment editor
        if (e.getClickCount() == 2 && !e.isConsumed()) {
            e.consume();
            NoteManager controller = glossableTextArea.getController();
            controller.getNote(glossableTextArea.getSelectionStart())
                    .ifPresent((note) -> controller.showNote(note.getStart(), note.getEnd()));
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger()) {
            mouseButtonDown = true;
            popup.show(e.getComponent(), e.getX(), e.getY());
        }
    }
//
//    @Override
//    public void mouseWheelMoved(MouseWheelEvent e) {
//        if (mouseButtonDown) {
//            glossableTextArea.zoom(e.getWheelRotation());
//        }
//    }
}
