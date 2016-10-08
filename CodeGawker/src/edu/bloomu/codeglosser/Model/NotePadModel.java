/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser.Model;

import de.java2html.Java2Html;
import de.java2html.javasource.JavaSourceType;
import de.java2html.options.JavaSourceConversionOptions;
import de.java2html.options.JavaSourceStyleEntry;
import de.java2html.util.RGB;
import edu.bloomu.codeglosser.Exceptions.InvalidTextSelectionException;
import edu.bloomu.codeglosser.Utils.Bounds;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

/**
 *
 * @author Louis
 * 
 * Model for the the NotePad view.
 */
public class NotePadModel {
    
    private static final Logger LOG = Logger.getLogger(NotePadModel.class.getName());
    private String text = "";
    private String title = "";

    public NotePadModel() {
        LOG.info("Initialized...");
    }
    
    /**
     * Segments the range of the selected highlighted text based on the
     * following criteria:
     * 
     * 1. Grab the start of the word being highlighted.
     * 2. Wrap to the end of the line.
     * 3. On a new line, skip to the first Word
     * @param start Starting offset of the note
     * @param end Ending offset of the note.
     * @return Array of segmented bounds.
     */
    public Collection<Bounds> segmentRange(Bounds range) throws InvalidTextSelectionException {
        int start = range.getStart();
        int end = range.getEnd();
        if (start == end) {
            LOG.warning("Nothing was selected...");
            throw new InvalidTextSelectionException();
        }
        
        LOG.info("Offsets: " + start + " to " + end);
        LOG.info("[Phase 1] Text: \"" + text.substring(start, end) + "\"");
        StringCharacterIterator iter = new StringCharacterIterator(text.substring(start, end));
        
        // Segment highlighting based on newline character
        {
            char ch = iter.current();
            
            // If white space, skip to next non-whitespace.
            while (Character.isSpaceChar(ch) && ch != CharacterIterator.DONE) {
                ch = iter.next();
                start++;
            }
            
            
            
            // If we actually skipped over the 'end', it's invalid.
            if (start > end) {
                LOG.warning("Skipped over all selected text without finding" +
                        " valid text...");
                throw new InvalidTextSelectionException();
            }
        }
        
        LOG.info("[Phase 2] Text: \"" + text.substring(start, end) + "\"");
        Collection<Bounds> bounds = new ArrayList<>();
        // Find everything up to newline character.
        int whiteSpaceOffset = start;
        int currOffset = start;
        char ch = iter.current();
        while (true) {
            switch (ch) {
                case ' ':
                case '\t':
                    whiteSpaceOffset = currOffset;
                    break;
                case '\r':
                    ch = iter.next();
                case '\n':
                    String orig = text.substring(start, currOffset);
                    String trimmed = orig.trim();
                    LOG.info("Chunk: " + trimmed);
                    bounds.add(Bounds.of(start + (orig.length() - trimmed.length()), currOffset));
                    start = currOffset + 1;
                    break;
                case CharacterIterator.DONE:
                    LOG.severe("EOF before finish...");
                    throw new InvalidTextSelectionException();
            }
            currOffset++;
            if (currOffset >= end) {
                if (start != end) {
                    String orig = text.substring(start, currOffset);
                    String trimmed = orig.trim();
                    LOG.info("End: " + trimmed);
                    bounds.add(Bounds.of(start + (orig.length() - trimmed.length()), currOffset));
                }
                break;
            }
            ch = iter.next();
        }
        
        return bounds;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
    
    public String toHTML() {
        JavaSourceConversionOptions options = JavaSourceConversionOptions.getDefault();
        options.getStyleTable().put(JavaSourceType.KEYWORD, new JavaSourceStyleEntry(RGB.BLUE, true, false));
        options.getStyleTable().put(JavaSourceType.STRING, new JavaSourceStyleEntry(new RGB(206, 133, 0)));
        options.getStyleTable().put(JavaSourceType.LINE_NUMBERS, new JavaSourceStyleEntry(RGB.BLACK));
        options.getStyleTable().put(JavaSourceType.NUM_CONSTANT, new JavaSourceStyleEntry(RGB.BLACK));
        options.getStyleTable().put(JavaSourceType.CODE_TYPE, new JavaSourceStyleEntry(RGB.BLUE));
        return Java2Html.convertToHtml(text, options);
    }    
}
