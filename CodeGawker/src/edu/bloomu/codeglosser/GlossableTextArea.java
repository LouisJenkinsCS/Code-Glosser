package edu.bloomu.codeglosser;

import org.jsoup.Jsoup;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;
import javax.swing.text.Highlighter;
import javax.swing.text.Highlighter.Highlight;
import javax.swing.text.Highlighter.HighlightPainter;
import org.openide.util.Exceptions;
import de.java2html.Java2Html;
import de.java2html.javasource.JavaSourceType;
import javax.swing.JTextPane;
import de.java2html.options.JavaSourceConversionOptions;
import de.java2html.options.JavaSourceStyleEntry;
import de.java2html.util.RGB;
import java.awt.Color;
import java.util.List;
import java.util.ArrayList;
import java.util.Enumeration;
import javax.swing.JOptionPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.ElementIterator;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

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
        options.getStyleTable().put(JavaSourceType.KEYWORD, new JavaSourceStyleEntry(RGB.BLUE, true, false));
        options.getStyleTable().put(JavaSourceType.STRING, new JavaSourceStyleEntry(new RGB(206, 133, 0)));
        options.getStyleTable().put(JavaSourceType.LINE_NUMBERS, new JavaSourceStyleEntry(RGB.BLACK));
        options.getStyleTable().put(JavaSourceType.NUM_CONSTANT, new JavaSourceStyleEntry(RGB.BLACK));
        options.setAddLineAnchors(true);
        options.setShowLineNumbers(true);
        options.getStyleTable().put(JavaSourceType.CODE_TYPE, new JavaSourceStyleEntry(RGB.BLUE));
        String text = Java2Html.convertToHtml(glossedDocument.getText(), options);
        System.err.println(text);
        setContentType("text/html");
        setText((ReadOnlyHTML = "<html style='width:100%;height:100%;'> <style>" + cssText + "</style> <body>" + text + "</body></html>"));
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
    
    /**
    * Returns all elements of a particular tag name.
    *
    * @param tagName The tag name of the elements to return (e.g., HTML.Tag.DIV).
    * @param document The HTML document to find tags in.
    * @return The set of all elements in the HTML document having the specified tag name.
    */
    public static Element[] getElementsByTagName(HTML.Tag tagName, HTMLDocument document)
    {
        List<Element> elements = new ArrayList<Element>();

        for (ElementIterator iterator = new ElementIterator(document); iterator.next() != null;)
        {
            Element currentEl = iterator.current();

            AttributeSet attributes = currentEl.getAttributes();

            HTML.Tag currentTagName = (HTML.Tag) attributes.getAttribute(StyleConstants.NameAttribute);

            if (currentTagName == tagName)
            {
                elements.add(iterator.current());
            } else if (currentTagName == HTML.Tag.CONTENT) {
                for (Enumeration<?> e = attributes.getAttributeNames(); e.hasMoreElements();)
                {
                    if (tagName == e.nextElement())
                    {
                        elements.add(iterator.current());
                        break;
                    }
                }
            }
        }

        return elements.toArray(new Element[0]);
    }

    Listener(GlossableTextArea glossableTextArea) {
        this.glossableTextArea = glossableTextArea;
        popup = new JPopupMenu();
        JMenuItem writeMenuItem = new JMenuItem("Show all comments");
        JMenuItem saveMenuItem = new JMenuItem("Save all comments");
        JMenuItem addNote = new JMenuItem("Add Note");
        popup.add(writeMenuItem);
        popup.add(saveMenuItem);
        popup.add(addNote);

        writeMenuItem.addActionListener((ActionEvent e) -> {

        });

        saveMenuItem.addActionListener((ActionEvent e) -> {

        });
        
        addNote.addActionListener((event) -> {
            try {
                int start = glossableTextArea.getSelectionStart();
                int end = glossableTextArea.getSelectionEnd();
                
                Document d = Jsoup.parse(glossableTextArea.ReadOnlyHTML);
                
                boolean isDigit = false;
                for (int i = 0;;i++) {
                    if (!isDigit && Character.isDigit(glossableTextArea.getText(start-i, 1).charAt(0))) {
                        isDigit = true;
                    } else if (isDigit && !Character.isDigit(glossableTextArea.getText(start-i, 1).charAt(0))) {
                        start -= (i-1);
                        break;
                    }
                }
                
                for (int i = 0;;i++) {
                    if (!isDigit && Character.isDigit(glossableTextArea.getText(start-i, 1).charAt(0))) {
                        isDigit = true;
                    } else if (isDigit && !Character.isDigit(glossableTextArea.getText(start-i, 1).charAt(0))) {
                        end += (i-i);
                        break;
                    }
                }
                
                String lineNo = "";
                for (int i = 0;;i++) {
                    try {
                        Integer.parseInt(glossableTextArea.getText(start+i, 1));
                        lineNo += glossableTextArea.getText(start+i, 1);
                    } catch (BadLocationException | NumberFormatException ex) {
                        if (lineNo.isEmpty()) {
                            throw new RuntimeException("Could not find line number!Start: " +
                                    start + ";End: " + end + ";Text: " + glossableTextArea.getText(start, start+i));
                        }
                        break;
                    }
                }
                
                
                
                String text = glossableTextArea.getText(start, end-start);
                Elements e = d.getElementsContainingOwnText(text);
                System.err.println(e.toString());
                
                
                HTMLDocument doc = (HTMLDocument) glossableTextArea.getDocument();                
                Element[] elems = getElementsByTagName(HTML.Tag.A, doc);
                Element startElem = elems[Integer.parseInt(lineNo)], endElem = elems[Integer.parseInt(lineNo)+1];
                
                if (endElem == null || startElem == null) {
                    throw new RuntimeException("Could not find specified elements!Start Obj: " +
                            startElem + "Start Line: " + Integer.parseInt(lineNo));
                }
                doc.insertAfterEnd(startElem, "<p class=\"note\" msg=\"Example Text!!!\">");
                doc.insertBeforeStart(endElem, "</p>");
                
                try {
                    JOptionPane.showMessageDialog(null, "Selected:\n" + glossableTextArea.getText(start, end-start) +
                            "\n\n\nHTML:\n", "Debug", JOptionPane.PLAIN_MESSAGE, null);
                } catch (BadLocationException ex) {
                    Exceptions.printStackTrace(ex);
                }
            } catch (BadLocationException ex) {
                Exceptions.printStackTrace(ex);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
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
//
//    @Override
//    public void mouseWheelMoved(MouseWheelEvent e) {
//        if (mouseButtonDown) {
//            glossableTextArea.zoom(e.getWheelRotation());
//        }
//    }
}
