/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser.View;

import edu.bloomu.codeglosser.Utils.Bounds;
import java.awt.Color;
import java.util.HashMap;
import java.util.logging.Logger;
import edu.bloomu.codeglosser.Controller.IMarkupView;
import edu.bloomu.codeglosser.Controller.NotePadController;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.stream.Stream;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;
import javax.swing.text.Highlighter;
import javax.swing.text.Highlighter.Highlight;
import javax.swing.text.Highlighter.HighlightPainter;


/**
 *
 * @author Louis
 */
public class NotePadView extends javax.swing.JPanel implements IMarkupView {
    
    private static final Logger LOG = Logger.getLogger(NotePadView.class.getName());
    
    private final PublishSubject<Bounds> onShowSelection = PublishSubject.create();
    private final PublishSubject<Bounds> onDeleteSelection = PublishSubject.create();
    private final PublishSubject<Bounds> onCreateSelection = PublishSubject.create();
    private final PublishSubject<Object> onPreviewHTML = PublishSubject.create();
    
    private final Highlighter highlighter;
    private final HashMap<Bounds, Highlight> hMap = new HashMap<>();
    
    private final JPopupMenu popup = new JPopupMenu();
    
    /**
     * Creates new form NotePad
     */
    public NotePadView() {
        LOG.info("Initialized...");
        initComponents();
        highlighter = textCode.getHighlighter();
        textCode.setEditable(false);
        initializePopup();
        textCode.addMouseListener(new MouseAdapter() {
            
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
                
                // Double click: select current highlight and open comment editor
                if (e.getClickCount() == 2 && !e.isConsumed()) {
                    LOG.info("Double Click!");
                    onShowSelection.onNext(Bounds.of(textCode.getSelectionStart(), textCode.getSelectionEnd()));
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
    
    private void initializePopup() {
        LOG.finer("Initializing PopupMenu...");
        JMenuItem showSelectedComment = new JMenuItem("Show selected comment");
        JMenuItem deleteSelectedComment = new JMenuItem("Delete selected comment");
        JMenuItem addNote = new JMenuItem("Add Note");
        popup.add(showSelectedComment);
        popup.add(deleteSelectedComment);
        popup.add(addNote);

        showSelectedComment.addActionListener((e) -> {
            LOG.info(e.paramString());
            onShowSelection.onNext(Bounds.of(textCode.getSelectionStart(), textCode.getSelectionEnd()));
        });

        deleteSelectedComment.addActionListener((e) -> {
            LOG.info(e.paramString());
            onDeleteSelection.onNext(Bounds.of(textCode.getSelectionStart(), textCode.getSelectionEnd()));
        });
        
        addNote.addActionListener((e) -> {
            LOG.info(e.paramString());
            LOG.info("Selected Text: " + textCode.getSelectedText().replaceAll("    ", "\n"));
            onCreateSelection.onNext(Bounds.of(textCode.getSelectionStart(), textCode.getSelectionEnd()));
        });

        popup.addSeparator();
        JMenuItem deleteAllNotesItem = new JMenuItem("Delete all notes");
        popup.add(deleteAllNotesItem);

        deleteAllNotesItem.addActionListener((e) -> {
            LOG.info(e.paramString());
        });

        JMenuItem saveAndExitMenuItem = new JMenuItem("Save and exit");
        popup.addSeparator();
        popup.add(saveAndExitMenuItem);

        saveAndExitMenuItem.addActionListener((e) -> {
            LOG.info(e.paramString());
        });
        
        JMenuItem previewHTML = new JMenuItem("Preview");
        previewHTML.addActionListener(onPreviewHTML::onNext);
        popup.add(previewHTML);
    }
    
    public void setText(String str) {
        LOG.info("Text changed...");
        LOG.info(str.trim());
        // Count line breaks
        textCode.setText("<html><body> <pre>" + str.trim() + " </pre></body></html>");
    }

    public Observable<Bounds> onShowSelection() {
        return onShowSelection;
    }
    
    public Observable<Bounds> onDeleteSelection() {
        return onDeleteSelection;
    }
    
    public Observable<Bounds> onCreateSelection() {
        return onCreateSelection;
    }
    
    public Observable<Object> onPreviewHTML() {
        return onPreviewHTML;
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

    @Override
    public void addMarkup(Bounds ...bounds) {
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
                        highlight = (Highlight) highlighter.addHighlight(b.getStart(), b.getEnd(), new DefaultHighlightPainter(Color.YELLOW));
                    } catch (BadLocationException ex) {
                        LOG.throwing(this.getClass().getName(), "addMarkup", ex);
                    }
                    hMap.put(b, highlight);
                });
    }

    @Override
    public void removeMarkup(Bounds ...bounds) {
        LOG.info(Stream
                .of(bounds)
                .map(Bounds::toString)
                .reduce("Removing Markups: {", (s1, s2) -> s1 + "\n\t" + s2)
                .concat("\n}")
        );
        // Remove any highlights within requested bounds
        Stream.of(hMap.keySet().toArray(new Bounds[hMap.size()]))
                .filter((b) -> Stream.of(bounds).anyMatch(b::collidesWith))
                .map(hMap::remove)
                .forEach(highlighter::removeHighlight);
    }

    @Override
    public void setMarkupColor(Bounds bounds, Color color) {
        Stream.of(highlighter.getHighlights())
                .filter((h) -> Bounds.of(h.getStartOffset(), h.getEndOffset()).collidesWith(bounds))
                .forEach((h) -> {
                    Bounds b = Bounds.of(h.getStartOffset(), h.getEndOffset());
                    highlighter.removeHighlight(h);
                    HighlightPainter hp = new DefaultHighlightPainter(color);
                    try {
                        Highlight h1 = (Highlight) highlighter.addHighlight(b.getStart(), b.getEnd(), hp);
                        hMap.put(b, h1);
                    } catch (BadLocationException ex) {
                        ex.printStackTrace();
                    }
                });
    }
}
