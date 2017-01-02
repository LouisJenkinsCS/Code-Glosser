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
package edu.bloomu.codeglosser;

import edu.bloomu.codeglosser.View.MarkupTopComponent;
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
