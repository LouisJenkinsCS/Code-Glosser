/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser.View;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import edu.bloomu.codeglosser.Controller.NoteManager;
import edu.bloomu.codeglosser.Controller.NoteView;
import edu.bloomu.codeglosser.Events.FileChangeEvent;
import edu.bloomu.codeglosser.Model.Note;
import edu.bloomu.codeglosser.Events.MarkupColorChangeEvent;
import edu.bloomu.codeglosser.Events.NoteSelectedChangeEvent;
import edu.bloomu.codeglosser.Events.NoteUpdateChangeEvent;
import edu.bloomu.codeglosser.Model.TemplateBranch;
import static edu.bloomu.codeglosser.Model.TemplateBranch.KEY_CATEGORY;
import edu.bloomu.codeglosser.Model.TemplateLeaf;
import edu.bloomu.codeglosser.Model.TreeViewBranch;
import edu.bloomu.codeglosser.Model.TreeViewNode;
import io.reactivex.disposables.Disposable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openide.util.Exceptions;
import org.reactivestreams.Subscription;

/**
 *
 * @author Louis
 */
public class NotePropertiesView extends javax.swing.JPanel implements NoteView {

    private static final Logger LOG = Logger.getLogger(NotePropertiesView.class.getName());
    
    
    Disposable managerUpdateSubscription;
    Disposable managerAddOrRemoveSubscription;
    private NoteManager manager;
    private Note note;
    private EventBus bus;
    
    /**
     * Creates new form NoteDescriptorPane
     */
    public NotePropertiesView() {
        initComponents();
        
        propertyMessage3.setMessage("");
        propertyMessage3.observe()
                .throttleLast(1, TimeUnit.SECONDS)
                .doOnNext((str) -> System.out.println("Note Message Change: " + str))
                .subscribe((str) -> {
                    // Note can be null when a change occurs (normally at initialization)
                    if (note != null) {
                        note.setMsg(str);
                    }
                });
        
        propertyHighlightColor3.setPropertyName("Highlight Color");
        propertyHighlightColor3.observe()
                .doOnNext((c) -> System.out.println("Highlight Color Change: " + c.toString()))
                .subscribe((c) -> {
                    bus.post(MarkupColorChangeEvent.of(note.getRange(), c));
                    note.setHighlightColor(c);
                });
        
        propertyNoteSelected.observe()
                .doOnNext((n) -> System.out.println("Note Selection Change: " + n.toString()))
                .subscribe((n) -> { 
                    if (n != null && bus != null) {
                        bus.post(NoteSelectedChangeEvent.of(n));
                    }
                });
        
        // Look for template file named "templates.json"
        initTemplate();
        
        clear();
    }
    
    private void initTemplate() {
        File f = new File("templates.json");
        if (!f.exists())
                propertyTemplate.setEnabled(false);
        
        try {
            JSONObject root = (JSONObject) new JSONParser().parse(new FileReader(f));
            JSONArray templates = (JSONArray) root.get("templates");
            TreeViewNode[] children = new TreeViewNode[templates.size()];
            for (int i = 0; i < templates.size(); i++) {
                JSONObject obj = (JSONObject) templates.get(i);
                // Is another branch...
                if (obj.containsKey(KEY_CATEGORY)) {
                    children[i] = new TemplateBranch(obj);
                } else {
                    children[i] = new TemplateLeaf((JSONObject) templates.get(i));
                }
            }
            
            propertyTemplate.setRoot(new TreeViewBranch() {
                @Override
                public TreeViewNode[] getChildren() {
                    return children;
                }

                @Override
                public String toString() {
                    return "Templates";
                }
            });
            
            propertyTemplate.observe()
                    .cast(TemplateLeaf.class)
                    .subscribe(t -> manager.applyTemplate(t));
        } catch (IOException | ParseException ex) {
            Exceptions.printStackTrace(ex);
            propertyTemplate.setEnabled(false);
        }
    }
    
    public void setEventBus(EventBus bus) {
        this.bus = bus;
    }
    
    @Subscribe
    public void handleNoteSelectionChange(NoteSelectedChangeEvent event) {
        LOG.info("Received NoteSelectionChangeEvent...");
        manager.setCurrentNote(event.getNote());
        display(event.getNote());
    }
    
    @Subscribe
    public void handleNoteUpdateChange(NoteUpdateChangeEvent event) {
        LOG.info("Received NoteUpdateChangeEvent...");
        Note n = event.getNote();
        bus.post(MarkupColorChangeEvent.of(n.getRange(), n.getHighlightColor()));
        if (note == event.getNote()) {
            List<Note> list = manager.getAllNotes();
            propertyNoteSelected.update(list.toArray(new Note[list.size()]));
            display(note);
        }
    }
    
    @Subscribe
    public void handleFileChange(FileChangeEvent event) {
        LOG.info("Received FileChangeEvent...");
        manager = NoteManager.getInstance(event.getFileName());
        if (managerAddOrRemoveSubscription != null) {
            managerAddOrRemoveSubscription.dispose();
            managerAddOrRemoveSubscription = null;
        }
        
        if (managerUpdateSubscription != null) {
            managerUpdateSubscription.dispose();
            managerUpdateSubscription = null;
        }
        
        List<Note> list = manager.getAllNotes();
        propertyNoteSelected.update(list.toArray(new Note[list.size()]));
        managerAddOrRemoveSubscription = manager
                .observeAddOrRemove()
                .subscribe(o -> {
                    List<Note> l = manager.getAllNotes();
                    propertyNoteSelected.update(l.toArray(new Note[l.size()]));
                });
        
        managerUpdateSubscription = manager
                .observeNoteUpdate()
                .filter(n -> n == note)
                .map(NoteUpdateChangeEvent::of)
                .subscribe(bus::post);
    }
    

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLayeredPane1 = new javax.swing.JLayeredPane();
        propertyArea = new javax.swing.JPanel();
        jInternalFrame4 = new javax.swing.JInternalFrame();
        propertyNoteSelected = new edu.bloomu.codeglosser.View.propertyNoteName();
        propertyHighlightColor3 = new edu.bloomu.codeglosser.View.PropertyColor();
        propertyMessage3 = new edu.bloomu.codeglosser.View.PropertyTextArea();
        propertyTemplate = new edu.bloomu.codeglosser.View.PropertyTreeView();

        jInternalFrame4.setDefaultCloseOperation(javax.swing.WindowConstants.HIDE_ON_CLOSE);
        jInternalFrame4.setIconifiable(true);
        jInternalFrame4.setTitle(org.openide.util.NbBundle.getMessage(NotePropertiesView.class, "NotePropertiesView.jInternalFrame4.title")); // NOI18N
        jInternalFrame4.setVisible(true);

        javax.swing.GroupLayout jInternalFrame4Layout = new javax.swing.GroupLayout(jInternalFrame4.getContentPane());
        jInternalFrame4.getContentPane().setLayout(jInternalFrame4Layout);
        jInternalFrame4Layout.setHorizontalGroup(
            jInternalFrame4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(propertyMessage3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(propertyHighlightColor3, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
            .addComponent(propertyNoteSelected, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addComponent(propertyTemplate, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jInternalFrame4Layout.setVerticalGroup(
            jInternalFrame4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jInternalFrame4Layout.createSequentialGroup()
                .addComponent(propertyNoteSelected, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(propertyMessage3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(propertyHighlightColor3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(propertyTemplate, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(0, 0, 0))
        );

        javax.swing.GroupLayout propertyAreaLayout = new javax.swing.GroupLayout(propertyArea);
        propertyArea.setLayout(propertyAreaLayout);
        propertyAreaLayout.setHorizontalGroup(
            propertyAreaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(propertyAreaLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(jInternalFrame4))
        );
        propertyAreaLayout.setVerticalGroup(
            propertyAreaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(propertyAreaLayout.createSequentialGroup()
                .addComponent(jInternalFrame4)
                .addGap(0, 0, 0))
        );

        try {
            jInternalFrame4.setMaximum(true);
        } catch (java.beans.PropertyVetoException e1) {
            e1.printStackTrace();
        }

        jLayeredPane1.setLayer(propertyArea, javax.swing.JLayeredPane.DEFAULT_LAYER);

        javax.swing.GroupLayout jLayeredPane1Layout = new javax.swing.GroupLayout(jLayeredPane1);
        jLayeredPane1.setLayout(jLayeredPane1Layout);
        jLayeredPane1Layout.setHorizontalGroup(
            jLayeredPane1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(propertyArea, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jLayeredPane1Layout.setVerticalGroup(
            jLayeredPane1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jLayeredPane1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(propertyArea, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLayeredPane1)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(jLayeredPane1))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JInternalFrame jInternalFrame4;
    private javax.swing.JLayeredPane jLayeredPane1;
    private javax.swing.JPanel propertyArea;
    private edu.bloomu.codeglosser.View.PropertyColor propertyHighlightColor3;
    private edu.bloomu.codeglosser.View.PropertyTextArea propertyMessage3;
    private edu.bloomu.codeglosser.View.propertyNoteName propertyNoteSelected;
    private edu.bloomu.codeglosser.View.PropertyTreeView propertyTemplate;
    // End of variables declaration//GEN-END:variables

    @Override
    public void display(Note note) {
        this.note = note;
        if (note == Note.DEFAULT) {
            clear();
            return;
        }
        this.propertyNoteSelected.setSelectedNote(note);
        this.propertyMessage3.setMessage(note.getMsg());
        this.propertyMessage3.setVisible(true);
        this.propertyHighlightColor3.setColor(note.getHighlightColor());
        this.propertyHighlightColor3.setVisible(true);
        this.propertyTemplate.setVisible(true);
    }

    @Override
    public void clear() {
        this.note = null;
        this.propertyMessage3.setVisible(false);
        this.propertyHighlightColor3.setVisible(false);
        this.propertyTemplate.setVisible(false);
    }
}
