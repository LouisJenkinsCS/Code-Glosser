/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser.Model;

import edu.bloomu.codeglosser.Exceptions.InvalidTextSelectionException;
import edu.bloomu.codeglosser.HTML.Java2HTML;
import edu.bloomu.codeglosser.Utils.Bounds;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;
import org.apache.logging.log4j.util.Strings;

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
    public Bounds[] segmentRange(Bounds range) throws InvalidTextSelectionException {
        int start = range.getStart();
        start = start == 0 ? start : start - 1;
        int end = range.getEnd() - 1;
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
                    currOffset++;
                case '\n':
                    String orig = text.substring(start, currOffset);
                    String trimmed = orig.trim();
                    LOG.info("Chunk: " + trimmed);
                    if (trimmed.length() == 0) {
                        break;
                    }
                    int startOffset = 0;
                    while (orig.charAt(startOffset) != trimmed.charAt(0))
                        startOffset++;
                    
                    int endOffset = 0;
                    while (orig.charAt(orig.length() - (endOffset + 1)) != trimmed.charAt(trimmed.length()-1))
                        endOffset++;
                    
                    bounds.add(Bounds.of(start + startOffset + 1, (currOffset - (endOffset + 1)) + 2));
                    start = currOffset + 1;
                    break;
                case CharacterIterator.DONE:
                    LOG.severe("EOF before finish... Text.length() = " + text.length() + ", bounds: " + range.toString());
                    LOG.severe("Remaining String: " + text.substring(start, end));
                    throw new InvalidTextSelectionException();
            }
            currOffset++;
            if (currOffset >= end) {
                if (start != end) {
                    String orig = text.substring(start, currOffset);
                    String trimmed = orig.trim();
                    if("".equals(trimmed))
                        break;
                    
                    LOG.info("End: " + trimmed);
                    
                    int startOffset = 0;
                    while (trimmed.length() != 0 && orig.charAt(startOffset) != trimmed.charAt(0))
                        startOffset++;
                    
                    int endOffset = 0;
                    while (trimmed.length() != 0 && orig.charAt(orig.length() - (endOffset + 1)) != trimmed.charAt(trimmed.length()-1))
                        endOffset++;
                    
                    bounds.add(Bounds.of(start + startOffset + 1, (currOffset - (endOffset + 1)) + 2));
                }
                break;
            }
            ch = iter.next();
        }
        
        return bounds.toArray(new Bounds[bounds.size()]);
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
        return new Java2HTML().translate(text);
    }
    
    public Bounds getLineBounds(int offset) {
        int lineStart = offset;
        int lineEnd = lineStart;

        // Check if we are at the beginning of the line
        char ch = '\0';
        if (lineStart != 0) {
            while (ch != '\n' && lineStart >= 0) {
                ch = text.charAt(lineStart);
                lineStart--;
            }
            lineStart++;
        }

        ch = '\0';
        while (ch != '\n' && lineEnd < text.length()) {
            ch = text.charAt(lineEnd);
            lineEnd++;
        }
        lineEnd--;

        return Bounds.of(lineStart + 1, lineEnd + 1);
    }
}
