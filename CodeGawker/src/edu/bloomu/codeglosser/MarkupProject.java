/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser;

import edu.bloomu.codeglosser.View.NotePropertiesView;
import edu.bloomu.codeglosser.View.ProjectFileViewer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
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
        ProjectFileViewer v = new ProjectFileViewer();
        JFileChooser jfc = new JFileChooser();
        jfc.setCurrentDirectory(new File("."));
        jfc.setDialogTitle("Project");
        jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        jfc.setAcceptAllFileFilterUsed(false);
        
        if (jfc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            LOG.info("Current Directory: " + jfc.getCurrentDirectory());
            v.setDirectory(jfc.getSelectedFile());
        }
        
        JOptionPane.showMessageDialog(null, v);
    }
}
