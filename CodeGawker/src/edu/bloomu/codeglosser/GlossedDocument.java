package edu.bloomu.codeglosser;

import java.awt.Point;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

/**
 *
 * @author Drue Coles
 */
public class GlossedDocument implements Serializable {
    
    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(GlossedDocument.class.getName());
    private final Document document;
    private final ArrayList<CommentData> list = new ArrayList<>();

    public GlossedDocument(Document document) {
        this.document = document;       
    }
    
    public String getDocumentName() {
        String name = (String) document.getProperty(Document.TitleProperty);
        int i = name.lastIndexOf(System.getProperty("file.separator"));
        if (i != -1 && i < name.length()) {
            name = name.substring(i + 1);
        }
        return name;
    }
       
    public String getText() {
        String text = null;
        try {
            text = document.getText(0, document.getLength());
        } catch (BadLocationException ex) {
            Logger.getLogger(GlossedDocument.class.getName()).log(Level.SEVERE, null, ex);
        }
        return text;
    }
    
    public boolean canAddComment(int startPos, int endPos) {
        for (CommentData data : list) {
            if (endPos >= data.startPos && startPos <= data.endPos) {
                return false;
            }
        }
        return true;
    }
    
    public void addComment(String comment, int startPos, int endPos) {
        list.add(new CommentData(comment, startPos, endPos));
    }
    
    public void removeComment(int startPos, int endPos) {
        CommentData data = null;
        for (CommentData cData : list) {
            if (cData.startPos == startPos && cData.endPos == endPos) {
                data = cData;
            }
        }
        if (data != null) {
            list.remove(data);
        }
    }
    
    public void removeAllComments() {
        list.clear();
    }
    
    public Point getCommentRange(int startPos, int endPos) {
        for (CommentData data : list) {
            if (endPos >= data.startPos && startPos <= data.endPos) {
                return new Point(data.startPos, data.endPos);
            }
        }
        return null;
    }
    
    public void addEmptyComment(int startPos, int endPos) {
        addComment("", startPos, endPos);
    }
    
    public void editComment(String comment, int startPos, int endPos) {
        for (CommentData data : list) {
            if (data.startPos == startPos && data.endPos == endPos) {
                data.comment = comment;
            }
        }
    }
   
}

class CommentData implements Serializable {

    String comment;
    final int startPos;
    final int endPos;

    CommentData(String comment, int startPos, int endPos) {
        this.startPos = startPos;
        this.endPos = endPos;
        this.comment = "";
    }  
}
