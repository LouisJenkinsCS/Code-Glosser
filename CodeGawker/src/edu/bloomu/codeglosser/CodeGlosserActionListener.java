package edu.bloomu.codeglosser;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JOptionPane;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.EditorRegistry;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

/**
 *
 * @author Drue Coles
 */
/**
 * Action listener invoked when the Code Glosser icon selected in toolbar, in view menu,
 * or by right clicking in source file.
 *
 * @author Drue Coles
 */
@ActionID(category = "Edit", id = "edu.bloomu.gloss.CodeGlosserActionListener")
@ActionRegistration(displayName = "Code Glosser", iconBase = "images/pig.png")
@ActionReferences({
    @ActionReference(path = "Toolbars/File"),
    @ActionReference(path = "Menu/Edit", position = 0, separatorAfter = 1),
    @ActionReference(path = "Editors/text/x-java/Popup", position = 1, separatorAfter = 2),
    @ActionReference(path = "Projects/org-netbeans-modules-java-j2seproject/Customizer/Application", 
            position = 1, separatorAfter = 2)
})
public class CodeGlosserActionListener implements ActionListener {

    /**
     * Create a copy of the Java source file in the editor window of most recent focus.
     * Save in gloss folder (create if necessary), which is a top-level folder of the
     * NB project to which the source file belongs. Open the file in a new gloss window.
     *
     * @param e not used
     */
    @Override
    public void actionPerformed(ActionEvent e) {

        JTextComponent jtc = EditorRegistry.lastFocusedComponent();
        if (jtc == null) {
            JOptionPane.showMessageDialog(null, "Source file must be open in editor.");
            return;
        }
        
        GlossableTopComponent gTopComponent = new GlossableTopComponent(jtc.getDocument());
        gTopComponent.open();
        gTopComponent.requestActive();
    }
}
