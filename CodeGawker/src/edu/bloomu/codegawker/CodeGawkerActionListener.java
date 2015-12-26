package edu.bloomu.codegawker;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.EditorRegistry;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

/**
 * Action listener invoked when the Code Gawker icon selected in toolbar, in view menu, or
 * by right clicking in source file.
 *
 * @author Drue Coles
 */
@ActionID(category = "View", id = "edu.bloomu.codefrag.CodeGawkerActionListener")
@ActionRegistration(displayName = "Code Gawker", 
        iconBase = "edu/bloomu/codegawker/glass32x32.png")
@ActionReferences({
    @ActionReference(path = "Toolbars/File", position = 1),
    @ActionReference(path = "Menu/View", position = 0, separatorAfter = 1),
    @ActionReference(path = "Editors/text/x-java/Popup", position = 0)
})
public class CodeGawkerActionListener implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        JTextComponent jtc = EditorRegistry.lastFocusedComponent();
        if (jtc == null) {
            JOptionPane.showMessageDialog(null, "Source file must be open in editor.");
            return;
        }
        String source = jtc.getText();
        String fragment = jtc.getSelectedText(); 
        if (fragment == null) {
            JOptionPane.showMessageDialog(null, "No code selected.");
            return;
        }
        
        // Get name of file displayed by editor
        Document doc = jtc.getDocument();
        String name = (String) doc.getProperty(Document.TitleProperty);
        int i = name.lastIndexOf(System.getProperty("file.separator"));
        if (i != -1 && i < name.length()) {
            name = name.substring(i + 1);
        }
 
        // capture unselected whitespace at beginning of first line
        i = source.indexOf(fragment) - 1;
        while (i >= 0 && source.charAt(i) != '\n') {
            fragment = " " + fragment;
            i--;
        }
        
        // remove initial whitespace-only lines 
        i = 0; // index to be advanced until finding first non-whitespace character
        int backupTo = -1; // index of last newline before first non-whitespace character
        boolean done = false;
        while (i < fragment.length() && !done) {
            if (fragment.charAt(i) == '\n') {
                backupTo = i;
            }
            if (!Character.isWhitespace(fragment.charAt(i))) {
                done = true;
            }
            i++;
        }
        if (backupTo > -1) {
            fragment = fragment.substring(backupTo + 1);
        }
        
        // remove whitespace-only lines after last non-whitespace character
        i = fragment.length() - 1;
        while (i >= 0 && Character.isWhitespace(fragment.charAt(i))) {
            i--;
        }
        fragment = fragment.substring(0, i + 1);
        
        // remove first min spaces from each line, where min is the smallest number of 
        // initial spaces
        String[] lines = fragment.split("\n");
        int min = Integer.MAX_VALUE;
        for (String aLine : lines) {
            int x = 0;
            while (x < aLine.length() && Character.isWhitespace(aLine.charAt(x))) {
                x++;
            }
            if (x < min) {
                min = x;
            }
        }
        
        // concatenate the lines back into a single string
        fragment = "";
        for (i = 0; i < lines.length - 1; i++) {
            fragment += (lines[i].substring(min) + '\n');
        }
        fragment += lines[i].substring(min);
        
        if (fragment.isEmpty()) {
            JOptionPane.showMessageDialog(null, "No code selected.");
        } else {
            CodeFragmentViewer frame = new CodeFragmentViewer(name, fragment);   
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        }
    }
}
