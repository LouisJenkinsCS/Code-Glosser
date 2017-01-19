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
import edu.bloomu.codeglosser.Events.EventBus;
import edu.bloomu.codeglosser.Globals;
import edu.bloomu.codeglosser.Model.Templates.TemplateLeaf;
import edu.bloomu.codeglosser.Model.Templates.TemplateNodeFactory;
import edu.bloomu.codeglosser.Model.TreeViewBranch;
import edu.bloomu.codeglosser.Model.TreeViewNode;
import edu.bloomu.codeglosser.Utils.SwingScheduler;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import edu.bloomu.codeglosser.Events.EventProcessor;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 *
 * @author Louis Jenkins
 */
public class PropertyTemplates extends javax.swing.JPanel implements EventProcessor {
    
    public static final String APPLY_TEMPLATE = "Apply Markup Template";
    
    private static final Logger LOG = Globals.LOGGER;
    private final EventBus engine = new EventBus(this, Event.PROPERTY_TEMPLATES);

    /**
     * Creates new form PropertyTemplates
     */
    public PropertyTemplates() {
        initComponents();
        initTemplateTree();
        initListeners();
    }
    
    @Override
    public Observable<Event> process(Event e) {
        return Observable.empty();
    }

    @Override
    public EventBus getEventEngine() {
        return engine;
    }
    
    private void initTemplateTree() {
        // Construct template tree (in background)
        Observable
                .just(Globals.TEMPLATE_FILE)
                .subscribeOn(Globals.WORKER_THREAD)
                // Only do so if it exists
                .filter(path -> path.toFile().exists())
                // Convert from JSON to JSONObject
                .map(path -> Files.lines(path).collect(Collectors.joining("\n")))
                .map(json -> (JSONObject) new JSONParser().parse(json))
                // Templates begin under the "templates" key
                .map(obj -> (JSONArray) obj.get("templates"))
                // We can now obtain each JSONObject from the JSONArray
                .flatMap(Observable::fromIterable)
                // The TemplateBranch constructor handles recursively discovering nested definitions
                .map(obj -> TemplateNodeFactory.getTemplateNode((JSONObject) obj))
                // Buffer all values into a single list.
                .buffer(Integer.MAX_VALUE)
                .observeOn(SwingScheduler.getInstance())
                // Set the root to a dummy which returns the parsed information above.
                .subscribe(list ->
                    setRoot(new TreeViewBranch() {
                        @Override
                        public TreeViewNode[] getChildren() {
                            List<TreeViewNode> l = (List<TreeViewNode>) list;
                            return l.toArray(new TreeViewNode[l.size()]);
                        }

                        @Override
                        public String toString() {
                            return "Templates";
                        }     
                    }
                ));
    }
    
    public void setRoot(TreeViewBranch root) {
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(root);
        try {
            populateTree(root, rootNode);
        } catch (IOException ex) {
            LOG.severe("Error attempting to populate tree: " + ex.getMessage());
            JOptionPane.showMessageDialog(null, "Error attempting to populate tree", "Error", JOptionPane.ERROR_MESSAGE);
        }
        DefaultTreeModel model = (DefaultTreeModel) templateTree.getModel();
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
    
    private void initListeners() {
        // Setup double click listener
        templateTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int selRow = templateTree.getRowForLocation(e.getX(), e.getY());
                TreePath selPath = templateTree.getPathForLocation(e.getX(), e.getY());
                if(selRow != -1) {
                    if (e.getClickCount() == 2) {
                        TreeViewNode selectedNode = (TreeViewNode) ((DefaultMutableTreeNode)selPath.getLastPathComponent()).getUserObject();
                        if (selectedNode != null && selectedNode instanceof TemplateLeaf) {
                            engine.broadcast(Event.MARKUP_PROPERTIES, APPLY_TEMPLATE, ((TemplateLeaf) selectedNode).getTemplate());
                        }
                    }
                }
            }
        });
                
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
        templateTree = new javax.swing.JTree();

        jScrollPane1.setViewportView(templateTree);

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
    private javax.swing.JTree templateTree;
    // End of variables declaration//GEN-END:variables
}
