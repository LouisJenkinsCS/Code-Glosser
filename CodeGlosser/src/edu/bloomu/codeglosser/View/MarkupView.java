/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser.View;

import com.google.common.eventbus.EventBus;
import edu.bloomu.codeglosser.Controller.GlosserController;
import edu.bloomu.codeglosser.Events.FileChangeEvent;
import edu.bloomu.codeglosser.Session.MarkupManager;
import java.io.File;
import java.util.logging.Logger;
import javax.swing.text.Document;

/**
 *
 * @author Louis
 */
public class MarkupView extends javax.swing.JPanel {

    private static final Logger LOG = Logger.getLogger(MarkupView.class.getName());
    
    
    private final GlosserController npController;
    private final EventBus eventBus;
    private final File project;
    
    /**
     * Creates new form MarkupView
     */
    public MarkupView(File project, EventBus eventBus) {
        this.eventBus = eventBus;
        this.project = project;
        MarkupManager.setURIPrefix(project.toURI());
        initComponents();
        npController = new GlosserController(eventBus, notePadView1);
        
        // Display standalone files immediately
        if (project.isFile()) {
            eventBus.post(FileChangeEvent.of(project));
        }
    }
    
    public void setNotePadText(String str) {
        notePadView1.setText(str);
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        notePadView1 = new edu.bloomu.codeglosser.View.MarkupView1();
        notePropertiesView1 = new edu.bloomu.codeglosser.View.MarkupPropertiesView(this.project, this.eventBus);

        notePropertiesView1.setEventBus(null);
        notePropertiesView1.setMaximumSize(new java.awt.Dimension(500, 37));
        notePropertiesView1.setName(""); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(notePadView1, javax.swing.GroupLayout.DEFAULT_SIZE, 708, Short.MAX_VALUE)
                .addGap(0, 0, 0)
                .addComponent(notePropertiesView1, javax.swing.GroupLayout.PREFERRED_SIZE, 500, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(notePadView1, javax.swing.GroupLayout.DEFAULT_SIZE, 638, Short.MAX_VALUE)
            .addComponent(notePropertiesView1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private edu.bloomu.codeglosser.View.MarkupView1 notePadView1;
    private edu.bloomu.codeglosser.View.MarkupPropertiesView notePropertiesView1;
    // End of variables declaration//GEN-END:variables
}
