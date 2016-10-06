/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser;

import com.google.common.io.Files;
import edu.bloomu.codeglosser.Utils.DocumentHelper;
import edu.bloomu.codeglosser.Utils.HTMLGenerator;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import javax.swing.JOptionPane;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.EditorRegistry;
import org.openide.cookies.EditorCookie;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "Window",
        id = "edu.bloomu.codeglosser.NoteMarkup"
)
@ActionRegistration(
        displayName = "#CTL_NoteMarkup"
)
@ActionReference(path = "Editors/text/x-java/Popup", position = -99, separatorBefore = -149)
@Messages("CTL_NoteMarkup=Markup Source Code")
public final class NoteMarkup implements ActionListener {

    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(NoteMarkup.class.getName());
    private final EditorCookie context;

    public NoteMarkup(EditorCookie context) {
        this.context = context;
        LOG.severe("Initializing...");
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        LOG.info("Clicked...");
        JTextComponent jtc = EditorRegistry.lastFocusedComponent();
        if (jtc == null) {
            JOptionPane.showMessageDialog(null, "Source file must be open in editor.");
            return;
        }
        
        GlossableTopComponent gTopComponent = new GlossableTopComponent(jtc.getDocument());
        gTopComponent.open();
        gTopComponent.requestActive();     
    }
    
    private void testHTML(Document doc) {
        String html = HTMLGenerator.generate(DocumentHelper.getDocumentName(doc), DocumentHelper.getText(doc));
        try {
            File f = new File("tmp.html");
            f.createNewFile();
            BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(f));
            stream.write(html.getBytes());
            stream.flush();
            stream.close();
            Desktop.getDesktop().browse(f.toURI());
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
}
