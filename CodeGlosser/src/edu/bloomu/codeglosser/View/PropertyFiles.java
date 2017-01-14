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

import edu.bloomu.codeglosser.Events.Event;
import edu.bloomu.codeglosser.Events.EventEngine;
import edu.bloomu.codeglosser.Events.EventHandler;
import edu.bloomu.codeglosser.Globals;
import edu.bloomu.codeglosser.Model.ProjectBranch;
import edu.bloomu.codeglosser.Model.ProjectLeaf;
import edu.bloomu.codeglosser.Model.TreeViewBranch;
import edu.bloomu.codeglosser.Model.TreeViewLeaf;
import edu.bloomu.codeglosser.Model.TreeViewNode;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

/**
 *
 * @author Louis Jenkins
 */
public class PropertyFiles extends javax.swing.JPanel implements EventHandler {
    
    public static final int FILE_SELECTED = 0x1;
    
    private final EventEngine engine = new EventEngine(this, Event.PROPERTIES_FILES);
    
    /**
     * Creates new form PropertyFiles
     */
    public PropertyFiles() {
        initComponents();
        initFileTree();
        initListener();
    }
    
    @Override
    public Observable<Event> handleEvent(Event e) {
        return Observable.empty();
    }

    @Override
    public EventEngine getEventEngine() {
        return engine;
    }
    
    private void initListener() {
        // Setup double click listener (and observer)
        MouseListener doubleClickListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int selRow = projectFileTree.getRowForLocation(e.getX(), e.getY());
                TreePath selPath = projectFileTree.getPathForLocation(e.getX(), e.getY());
                if(selRow != -1) {
                    if (e.getClickCount() == 2) {
                        TreeViewNode selectedNode = (TreeViewNode) ((DefaultMutableTreeNode)selPath.getLastPathComponent()).getUserObject();
                        if (selectedNode != null && selectedNode instanceof TreeViewLeaf) {
                            engine.broadcast(Event.MARKUP_PROPERTIES, FILE_SELECTED, ((ProjectLeaf) selectedNode).getFile().toPath());
                        }
                    }
                }
            }
        };
        projectFileTree.addMouseListener(doubleClickListener);
    }
    
    private void initFileTree() {
        ProjectBranch root = new ProjectBranch(Globals.PROJECT_FOLDER.toFile());
        setRoot(root);
    }
    
    public void setRoot(TreeViewBranch root) {
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(root);
        try {
            populateTree(root, rootNode);
        } catch (IOException ex) {
            
        }
        DefaultTreeModel model = (DefaultTreeModel) projectFileTree.getModel();
        model.setRoot(rootNode);
    }
    
    private void populateTree(TreeViewBranch branch, DefaultMutableTreeNode root) throws IOException {
        for (TreeViewNode v : branch.getChildren()) {
            DefaultMutableTreeNode n = new DefaultMutableTreeNode(v);
            root.add(n);
            
            if (v instanceof TreeViewBranch) {
                populateTree((TreeViewBranch) v, n);
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        projectFileTree = new javax.swing.JTree();

        jScrollPane1.setViewportView(projectFileTree);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTree projectFileTree;
    // End of variables declaration//GEN-END:variables
}
