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
import edu.bloomu.codeglosser.Globals;
import edu.bloomu.codeglosser.Utils.SessionManager;
import java.awt.BorderLayout;
import java.io.File;
import java.util.logging.Logger;
import javax.swing.JFileChooser;

/**
 *
 * @author Louis Jenkins
 */
public class MarkupFrame extends javax.swing.JFrame {

    private static final Logger LOG = Globals.LOGGER;
    
    public MarkupFrame() {
        initComponents();
        
        // Setup global data
        Globals.initGlobals();
        
        File project = null;
        JFileChooser jfc = new JFileChooser();
        jfc.setCurrentDirectory(new File("."));
        jfc.setDialogTitle("Project");
        jfc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        jfc.setAcceptAllFileFilterUsed(false);
        
        if (jfc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            LOG.info("Current Directory: " + jfc.getCurrentDirectory().getName() + ", Selected File: " + jfc.getSelectedFile().getName());
            project = jfc.getSelectedFile();
        } else {
            LOG.severe("User Did Not Select File!!!");
            System.exit(0);
        }
        
        // Setup global data
        Globals.initGlobals();
        Globals.PROJECT_FOLDER = project.toPath();
        Globals.URI_PREFIX = project.toURI();
        
        // Initialize needed components
        SessionManager.init();
        
        // Create the main components
        MarkupController controller = new MarkupController();
        MarkupView view = new MarkupView();
        MarkupProperties properties = new MarkupProperties();
        
        // Connect the MarkupController to the MarkupView and MarkupProperty
        controller.getEventEngine().register(view.getEventEngine());
        controller.getEventEngine().register(properties.getEventEngine());
        
        this.setLayout(new BorderLayout());
        
        add(view, BorderLayout.CENTER);
        add(properties, BorderLayout.LINE_END);
        pack();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
