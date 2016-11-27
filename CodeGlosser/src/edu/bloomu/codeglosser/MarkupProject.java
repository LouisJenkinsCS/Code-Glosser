/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser;

import edu.bloomu.codeglosser.View.MarkupTopComponent;
import edu.bloomu.codeglosser.View.MarkupPropertiesView;
import edu.bloomu.codeglosser.View.PropertyTreeView;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "Window",
        id = "edu.bloomu.codeglosser.MarkupProject"
)
@ActionRegistration(
        displayName = "#CTL_MarkupProject"
)
@ActionReference(path = "Menu/File", position = 1429)
@Messages("CTL_MarkupProject=Markup Project")
public final class MarkupProject implements ActionListener {

    private static final Logger LOG = Logger.getLogger(MarkupProject.class.getName());

    
    
    @Override
    public void actionPerformed(ActionEvent e) {
        JFileChooser jfc = new JFileChooser();
        jfc.setFileFilter(new FileNameExtensionFilter("Java source files...", "java"));
        jfc.setCurrentDirectory(new File("."));
        jfc.setDialogTitle("Project");
        jfc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        jfc.setAcceptAllFileFilterUsed(false);
        
        if (jfc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            LOG.info("Current Directory: " + jfc.getCurrentDirectory().getName() + ", Selected File: " + jfc.getSelectedFile().getName());
            MarkupTopComponent view = new MarkupTopComponent(jfc.getSelectedFile());
            view.open();
            view.requestActive();
        }  
    }
}
