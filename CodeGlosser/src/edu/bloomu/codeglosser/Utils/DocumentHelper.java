/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser.Utils;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

/**
 *
 * @author Louis
 */
public final class DocumentHelper {
    public static String getDocumentName(Document doc) {
        String name = (String) doc.getProperty(Document.TitleProperty);
        int i = name.lastIndexOf(System.getProperty("file.separator"));
        if (i != -1 && i < name.length()) {
            name = name.substring(i + 1);
        }
        return name;
    }
    
    public static String getText(Document doc) {
        String text = null;
        try {
            text = doc.getText(0, doc.getLength());
        } catch (BadLocationException ex) {
            Logger.getLogger(DocumentHelper.class.getName()).log(Level.SEVERE, null, ex);
        }
        return text;
    }
}
