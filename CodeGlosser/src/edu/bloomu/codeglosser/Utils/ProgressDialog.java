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
package edu.bloomu.codeglosser.Utils;

import java.awt.BorderLayout;
import java.awt.Dialog;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/**
 *
 * @author Louis Jenkins
 */
public class ProgressDialog {
    private JTextArea msgLabel;
    private JDialog dialog;
    private JProgressBar progressBar;
            
    public ProgressDialog() {
        final int MAXIMUM = 100;
        JPanel panel;

        progressBar = new JProgressBar(0, MAXIMUM);
        progressBar.setIndeterminate(true);
        msgLabel = new JTextArea("");
        msgLabel.setEditable(false);


        panel = new JPanel(new BorderLayout(5, 5));
        panel.add(msgLabel, BorderLayout.PAGE_START);
        panel.add(progressBar, BorderLayout.CENTER);
        panel.setBorder(BorderFactory.createEmptyBorder(11, 11, 11, 11));

        dialog = new JDialog();
        dialog.setModalityType(Dialog.ModalityType.MODELESS);
        dialog.getContentPane().add(panel);
        dialog.setResizable(false);
        dialog.pack();
        dialog.setSize(500, dialog.getHeight());
        dialog.setLocationRelativeTo(null);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.setAlwaysOnTop(true);
        dialog.setModal(true);
    }
    
    public ProgressDialog setText(String text) {
        msgLabel.setText(text);
        return this;
    }
    
    public ProgressDialog setTitle(String title) {
        dialog.setTitle(title);
        return this;
    }
    
    public void show() {
        SwingUtilities.invokeLater(() -> dialog.setVisible(true));
    }
    
    public void done() {
        SwingUtilities.invokeLater(() -> dialog.setVisible(false));
    }
}
