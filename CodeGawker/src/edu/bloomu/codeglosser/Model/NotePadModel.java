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
import edu.bloomu.codeglosser.Utils.DocumentHelper;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 *
 * @author Louis
 * 
 * Model for the the NotePad view.
 */
public class NotePadModel {
    
    private static final Logger LOG = Logger.getLogger(NotePadModel.class.getName());
    private String text = "";

    public NotePadModel() {
        LOG.finest("Initialized...");
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
    public Collection<Bounds> segmentRange(int start, int end) throws InvalidTextSelectionException {
        if (start == end) {
            LOG.warning("Nothing was selected...");
            throw new InvalidTextSelectionException();
        }
        
        LOG.fine("Offsets: " + start + " to " + end);
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
        
        Collection<Bounds> bounds = new ArrayList<>();
        // Find everything up to newline character.
        int offset = start;
        while (true) {
            char ch = iter.current();
            // Consume everything up to a white space, and look-ahead to see if
            // there are other characters left; if there are no other letters then
            // this line is finished.
            while (!Character.isSpaceChar(ch) && ch != CharacterIterator.DONE) {
                ch = iter.next();
                offset++;
            }
            
            // Error: Reached EOF
            if (ch == CharacterIterator.DONE) {
                LOG.warning("While scanning for white space, found EOF...");
                throw new InvalidTextSelectionException();
            }
            // Terminate: Reached 'end'
            else if (start >= end) {
                bounds.add(Bounds.of(start, offset));
                break;
            }
            
            // Don't want to update official offset as we want to discard end
            // white spaces...
            int tmpOffset = offset;
            // Check if there is another character on this line...
            while (Character.isSpaceChar(ch) && ch != CharacterIterator.DONE && ch != '\r' || ch != '\n') {
                ch = iter.next();
                tmpOffset++;
            }
            
            // Error: Reached EOF
            if (ch == CharacterIterator.DONE) {
                LOG.warning("While scanning for new line, found EOF...");
                throw new InvalidTextSelectionException();
            }
            // Terminate: Reached 'end'
            else if (start >= end) {
                bounds.add(Bounds.of(start, offset));
                break;
            }
            
            // Handle current state based on if we come across newline or not.
            switch (ch) {
                // Windows: Carriage Returns are two characters, '\r\n'
                case '\r':
                    // Consume '\r'
                    iter.next();
                    tmpOffset++;
                case '\n':
                    bounds.add(Bounds.of(start, offset));
                    // Consume '\n'
                    iter.next();
                    tmpOffset++;
                    // Update start position for new line...
                    start = tmpOffset;
                default:
                    // Restore offset
                    offset = tmpOffset;
            }
        }
        
        return bounds;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
    
    public String toHTML() {
        JavaSourceConversionOptions options = JavaSourceConversionOptions.getDefault();
        options.getStyleTable().put(JavaSourceType.KEYWORD, new JavaSourceStyleEntry(RGB.BLUE, true, false));
        options.getStyleTable().put(JavaSourceType.STRING, new JavaSourceStyleEntry(new RGB(206, 133, 0)));
        options.getStyleTable().put(JavaSourceType.LINE_NUMBERS, new JavaSourceStyleEntry(RGB.BLACK));
        options.getStyleTable().put(JavaSourceType.NUM_CONSTANT, new JavaSourceStyleEntry(RGB.BLACK));
        options.getStyleTable().put(JavaSourceType.CODE_TYPE, new JavaSourceStyleEntry(RGB.BLUE));
        return Java2Html.convertToHtmlPage(text, options);
    }
    
}
