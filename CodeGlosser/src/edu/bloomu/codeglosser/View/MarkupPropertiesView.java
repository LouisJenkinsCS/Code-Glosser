/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser.View;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import edu.bloomu.codeglosser.Session.MarkupManager;
import edu.bloomu.codeglosser.Events.FileChangeEvent;
import edu.bloomu.codeglosser.Model.Markup;
import edu.bloomu.codeglosser.Events.MarkupColorChangeEvent;
import edu.bloomu.codeglosser.Events.NoteSelectedChangeEvent;
import edu.bloomu.codeglosser.Events.NoteUpdateChangeEvent;
import edu.bloomu.codeglosser.Model.ProjectBranch;
import edu.bloomu.codeglosser.Model.ProjectLeaf;
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
public class MarkupPropertiesView extends javax.swing.JPanel implements IMarkupPropertiesView {

    private static final Logger LOG = Logger.getLogger(MarkupPropertiesView.class.getName());
    
    
    Disposable managerUpdateSubscription;
    Disposable managerAddOrRemoveSubscription;
    private MarkupManager manager;
    private Markup note;
    private EventBus bus;
    
    /**
     * Creates new form NoteDescriptorPane
     */
    public MarkupPropertiesView(File project, EventBus bus) {
        initComponents();
        
        this.bus = bus;
        bus.register(this);
        
        propertyMessage3.setMessage("");
        propertyMessage3.observe()
                .throttleLast(1, TimeUnit.SECONDS)
                .doOnNext((str) -> System.out.println("Note Message Change: " + str))
                .subscribe((str) -> {
                    // Markup can be null when a change occurs (normally at initialization)
                    if (note != null) {
                        note.setMsg(str);
                    }
                });
        
        propertyHighlightColor3.setPropertyName("Highlight Color");
        propertyHighlightColor3.observe()
                .doOnNext(c -> System.out.println("Highlight Color Change: " + c.toString()))
                .doOnNext(c -> note.setHighlightColor(c))
                .map(c -> note)
                .map(MarkupColorChangeEvent::of)
                .subscribe(bus::post);
        
        propertyNoteSelected.observe()
                .doOnNext(n -> System.out.println("Note Selection Change: " + n.toString()))
                .map(NoteSelectedChangeEvent::of)
                .subscribe(bus::post);
        
        // Look for template file named "templates.json"
        initTemplate();
        initProjectFiles(project);
        
        // NoteProperties start out with an empty view because there is nothing to select.
        clear();
    }
    
    private void initTemplate() {
        File f = new File("templates.json");
        if (!f.exists()) {
                propertyTemplate.empty();
                return;
        }
        
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
    
    private void initProjectFiles(File project) {
        // Initialize file view if is project
        if (project.isDirectory()) {
            propertyFiles.setRoot(new ProjectBranch(project));
            propertyFiles.observe()
                    .cast(ProjectLeaf.class)
                    .map(ProjectLeaf::getFile)
                    .map(FileChangeEvent::of)
                    .subscribe(bus::post);
        }
    }
    
    public void setEventBus(EventBus bus) {
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
        Markup n = event.getNote();
        
        // Color could have changed...
        bus.post(MarkupColorChangeEvent.of(n));
        if (note == event.getNote()) {
            List<Markup> list = manager.getAllNotes();
            propertyNoteSelected.update(list.toArray(new Markup[list.size()]));
            display(note);
        }
    }
    
    @Subscribe
    public void handleFileChange(FileChangeEvent event) {
        LOG.info("Received FileChangeEvent...");
        manager = MarkupManager.getInstance(event.getFileName());
        if (managerAddOrRemoveSubscription != null) {
            managerAddOrRemoveSubscription.dispose();
            managerAddOrRemoveSubscription = null;
        }
        
        if (managerUpdateSubscription != null) {
            managerUpdateSubscription.dispose();
            managerUpdateSubscription = null;
        }
        
        List<Markup> list = manager.getAllNotes();
        propertyNoteSelected.update(list.toArray(new Markup[list.size()]));
        managerAddOrRemoveSubscription = manager
                .observeAddOrRemove()
                .subscribe(o -> {
                    List<Markup> l = manager.getAllNotes();
                    propertyNoteSelected.update(l.toArray(new Markup[l.size()]));
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
        jTabbedPane1 = new javax.swing.JTabbedPane();
        propertyTemplate = new edu.bloomu.codeglosser.View.PropertyTreeView();
        propertyFiles = new edu.bloomu.codeglosser.View.PropertyTreeView();

        jInternalFrame4.setDefaultCloseOperation(javax.swing.WindowConstants.HIDE_ON_CLOSE);
        jInternalFrame4.setIconifiable(true);
        jInternalFrame4.setTitle(org.openide.util.NbBundle.getMessage(MarkupPropertiesView.class, "MarkupPropertiesView.jInternalFrame4.title")); // NOI18N
        jInternalFrame4.setVisible(true);

        jTabbedPane1.addTab(org.openide.util.NbBundle.getMessage(MarkupPropertiesView.class, "MarkupPropertiesView.propertyTemplate.TabConstraints.tabTitle"), propertyTemplate); // NOI18N
        jTabbedPane1.addTab(org.openide.util.NbBundle.getMessage(MarkupPropertiesView.class, "MarkupPropertiesView.propertyFiles.TabConstraints.tabTitle"), propertyFiles); // NOI18N

        javax.swing.GroupLayout jInternalFrame4Layout = new javax.swing.GroupLayout(jInternalFrame4.getContentPane());
        jInternalFrame4.getContentPane().setLayout(jInternalFrame4Layout);
        jInternalFrame4Layout.setHorizontalGroup(
            jInternalFrame4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(propertyMessage3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jInternalFrame4Layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(propertyNoteSelected, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addComponent(propertyHighlightColor3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
            .addComponent(jTabbedPane1, javax.swing.GroupLayout.Alignment.TRAILING)
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
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE))
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
                .addGap(0, 0, 0)
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
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JPanel propertyArea;
    private edu.bloomu.codeglosser.View.PropertyTreeView propertyFiles;
    private edu.bloomu.codeglosser.View.PropertyColor propertyHighlightColor3;
    private edu.bloomu.codeglosser.View.PropertyTextArea propertyMessage3;
    private edu.bloomu.codeglosser.View.propertyNoteName propertyNoteSelected;
    private edu.bloomu.codeglosser.View.PropertyTreeView propertyTemplate;
    // End of variables declaration//GEN-END:variables

    @Override
    public void display(Markup note) {
        this.note = note;
        if (note == Markup.DEFAULT) {
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
