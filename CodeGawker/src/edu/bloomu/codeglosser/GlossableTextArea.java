package edu.bloomu.codeglosser;

import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;
import javax.swing.text.Highlighter;
import javax.swing.text.Highlighter.Highlight;
import javax.swing.text.Highlighter.HighlightPainter;
import org.openide.util.Exceptions;
import de.java2html.Java2Html;
import javax.swing.JTextPane;
import de.java2html.options.JavaSourceConversionOptions;

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
public class GlossableTextArea extends JTextPane {

    private static final Color backgroundColor = new Color(240, 230, 230);    
    
    // Default font properties for text to be displayed
    private static final String FONT_FAMILY = Font.MONOSPACED;
    private static final int FONT_WEIGHT = Font.PLAIN;
    private static final int FONT_SIZE = 12;
    private Font font = new Font(FONT_FAMILY, FONT_WEIGHT, FONT_SIZE);

    private final GlossedDocument glossedDocument;

    private final HashMap<Point, Highlight> map = new HashMap<>();

    // Highlighter and painter
    private final Highlighter highlighter = getHighlighter();
    private Color highlightColor = new Color(255, 255, 0, 150);
    private final HighlightPainter painter = new DefaultHighlightPainter(highlightColor);
    private Color selectedHighlightColor = new Color(20, 220, 60);

    /**
     * Creates an unglossed text area.
     *
     * @param text the text to be glossed
     */
    public GlossableTextArea(GlossedDocument glossedDocument) {
        this.glossedDocument = glossedDocument;
        JavaSourceConversionOptions options = JavaSourceConversionOptions.getDefault();
        options.setShowLineNumbers(true);
        String text = Java2Html.convertToHtml(glossedDocument.getText(), options);
        setContentType("text/html");
        setText("<html style='width:100%;height:100%;'> <body>" + text + "</body></html>");
        setFont(font);
        setMargin(new Insets(5, 5, 5, 5));
        setEditable(false);
//        setBackground(backgroundColor);
        Listener listener = new Listener(this);
        addMouseListener(listener);
        addMouseWheelListener(listener);
    }
    
    
    
    public String getDocumentName() {
        return glossedDocument.getDocumentName();
    }
    
    public GlossedDocument getGlossedDocument() {
        return glossedDocument;
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

    /**
     * Changes the font size by a given increment (can be negative).
     *
     * @param n the increment
     */
    public void zoom(int n) {
        font = new Font(FONT_FAMILY, FONT_WEIGHT, font.getSize() - n);
        setFont(font);
    }

    /**
     * Adds a highlight and comment.
     *
     * @param start starting position of highlight
     * @param end ending position of highlight
     */
    public void addHighlight(int start, int end) {
        if (glossedDocument.canAddComment(start, end)) {
            glossedDocument.addEmptyComment(start, end);
            Highlight highlight = null;
            try {
                highlight = (Highlight) highlighter.addHighlight(start, end, painter);
            } catch (BadLocationException ex) {
                Logger.getLogger(GlossableTextArea.class.getName()).log(Level.SEVERE, null, ex);
            }
            map.put(new Point(start, end), highlight);
        }
    }

    /**
     * Removes the currently selected highlight and comment.
     */
    public void removeHighlight() {
        Point p = new Point(getSelectionStart(), getSelectionEnd());
        Highlight highlight = map.get(p);
        if (highlight != null) {
            map.remove(p);
            glossedDocument.removeComment(p.x, p.y);
            setSelectionStart(0);
            setSelectionEnd(0);
        }
    }

    /**
     * Removes all highlights and associated comments.
     */
    public void removeAllHighlights() {
        highlighter.removeAllHighlights();
        glossedDocument.removeAllComments();
        map.clear();
    }

    /**
     * This method is invoked when the user has double clicked. If the mouse position lies
     * within an existing highlight, the current selection is expanded from that point to
     * encompass the entire highlight and the color of the current selection is changed
     * for visual emphasis.
     */
    public void selectHighlight() {
        int x = getSelectionStart();
        int y = getSelectionEnd();
        Point p = glossedDocument.getCommentRange(x, y);
        if (p != null) {
            setSelectionStart(p.x);
            setSelectionEnd(p.y);
            setSelectionColor(selectedHighlightColor);
        }
    }
    
    public void saveGlossedDocument() {
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(new FileOutputStream("file.gl"));
            out.writeObject(glossedDocument);                    
            out.close();
        } catch (FileNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }        
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
        JMenuItem writeMenuItem = new JMenuItem("Show all comments");
        JMenuItem saveMenuItem = new JMenuItem("Save all comments");
        popup.add(writeMenuItem);
        popup.add(saveMenuItem);

        writeMenuItem.addActionListener((ActionEvent e) -> {

        });

        saveMenuItem.addActionListener((ActionEvent e) -> {

        });

        popup.addSeparator();
        JMenuItem removeMenuItem = new JMenuItem("Remove selected highlight");
        popup.add(removeMenuItem);

        removeMenuItem.addActionListener((ActionEvent e) -> {
            glossableTextArea.removeHighlight();
        });

        JMenuItem saveAndExitMenuItem = new JMenuItem("Save and exit");
        popup.addSeparator();
        popup.add(saveAndExitMenuItem);

        saveAndExitMenuItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
               glossableTextArea.saveGlossedDocument();
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
        int start = glossableTextArea.getSelectionStart();
        int end = glossableTextArea.getSelectionEnd();
        if (start != end) {
            glossableTextArea.addHighlight(start, end);
            glossableTextArea.setSelectionStart(end);
            glossableTextArea.setSelectionEnd(end);
        }
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
            glossableTextArea.selectHighlight();
            CommentEditorTopComponent ceTopComponent 
                    = new CommentEditorTopComponent(glossableTextArea.getGlossedDocument());
            String s = "	✍";
            ceTopComponent.setDisplayName(s);
            ceTopComponent.setToolTipText("Enter comments about selected code.");
            ceTopComponent.open();
            ceTopComponent.requestActive();            
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON2) {
            mouseButtonDown = true;
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (mouseButtonDown) {
            glossableTextArea.zoom(e.getWheelRotation());
        }
    }
}
