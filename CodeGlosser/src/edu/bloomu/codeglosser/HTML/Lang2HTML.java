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
package edu.bloomu.codeglosser.HTML;

import edu.bloomu.codeglosser.Utils.HTMLGenerator;
import java.awt.Color;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 *
 * @author Louis
 */
public abstract class Lang2HTML {
    
    private HashMap<String, DisplayConfig> configMap = new HashMap<>();
    
    enum ParseState {
        NORMAL,
        STRING,
        CHAR,
        SINGLE_LINE_COMMENT,
        MULTI_LINE_COMMENT
    }
    
    private ParseState state = ParseState.NORMAL;
    
    private Syntax syntax;
    
    protected abstract String[] getKeywordList();
    
    protected DisplayConfig getKeywordFont() {
        return new DisplayConfig(Color.BLUE, HTMLFont.PLAIN);
    }
    
    protected abstract String[] getPrimitiveTypeList();
    
    protected DisplayConfig getPrimitiveTypeColor() {
        return new DisplayConfig(Color.BLUE, HTMLFont.PLAIN);
    }
    
    protected String getNewLineStandin() {
        return "<br>";
    }

    public Lang2HTML() {
        Stream.of(getKeywordList())
                .forEach((keyword) -> configMap.put(keyword, getKeywordFont()));
        Stream.of(getPrimitiveTypeList())
                .forEach((primitiveType) -> configMap.put(primitiveType, getPrimitiveTypeColor()));
    }
    
    private String parseToken(StringCharacterIterator iterator) {
        char ch = iterator.current();
        switch (ch) {
            // Spaces are translated into HTML entities
            case ' ': {
                return "&nbsp;";  
            }
            // Tabs are represented as a sequence of HTML entity spaces
            case '\t': {
                return "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
            }
            // Based on the architecture, newline can be either \r, \r\n, or just \n
            case '\r': {
                    if (iterator.next() != '\n')
                        iterator.previous();
            }
            // Handle newline character as HTML Entity
            case '\n': {
                return getNewLineStandin();
            }
            // Convert to HTML entities
            case '>': {
                return "&gt;";
            }
            // Convert to HTML entities
            case '<': {
                return "&lt;";
            }
            case '&': {
                return "&amp";
            }
            // The non-general cases which depend on which mode we are in
            default: {
                // Handle processing based on state.
                switch (state) {
                    case NORMAL: {
                        if (Character.isDigit(ch)) {
                            StringBuilder tmp = new StringBuilder().append(ch);
                            char c = iterator.next();
                            
                            // Check for hex
                            if (ch == '0' && c == 'x') {
                                tmp.append(c);
                                c = iterator.next();
                                while(Character.isDigit(c) || Character.isAlphabetic(c)) {
                                    tmp.append(c);
                                    c = iterator.next();
                                }
                                iterator.previous();
                                return syntax.numberToHTMLTag(tmp.toString());
                            } else {
                                while (Character.isDigit(c) || c == '.') {
                                    tmp.append(c);
                                    c = iterator.next(); 
                                }
                                
                                iterator.previous();
                                return syntax.numberToHTMLTag(tmp.toString());
                            }
                        }
                        
                        switch (ch) {
                            // Methods
                            case '.': {
                                char c = iterator.next();
                                // Check if there is a word following this.
                                 if (!Character.isAlphabetic(c)) {
                                    break;
                                }

                                StringBuilder tmp = new StringBuilder(".").append(c);
                                c = iterator.next();
                                while (Character.isAlphabetic(c) || Character.isDigit(c) || c == '_') {
                                    tmp.append(c);
                                    c = iterator.next();
                                }
                                
                                iterator.previous();
                                return syntax.methodToHTMLTag(tmp.toString());
                            }
                            // Look for start of comment
                            case '/': {                                
                                // Next character determines if it is and what type
                                switch (iterator.next()) {
                                    // Single line...
                                    case '/': {
                                        state = ParseState.SINGLE_LINE_COMMENT;
                                        return "//";
                                    }
                                    // Multi line...
                                    case '*': {
                                        state = ParseState.MULTI_LINE_COMMENT;
                                        return "/*";
                                    }
                                    // Put back.
                                    default: {
                                        iterator.previous();
                                        return "/";
                                    }
                                }
                            }
                            // Character
                            case '\'': {
                                state = ParseState.CHAR;
                                return "'";
                            }
                            // String
                            case '\"': {
                                state = ParseState.STRING;
                                return "\"";
                            }
                            // Look for potential keywords
                            default: {
                                if (!Character.isAlphabetic(ch)) {
                                    return "" + ch;
                                }

                                StringBuilder tmp = new StringBuilder().append(ch);
                                char c = iterator.next();
                                while (Character.isAlphabetic(c) || Character.isDigit(c) || c == '_') {
                                    tmp.append(c);
                                    c = iterator.next();
                                }

                                iterator.previous();
                                return syntax.wordToHTMLTag(tmp.toString());
                            }
                        }
                    }
                    
                    case STRING: {
                        switch (ch) {
                            // Escape
                            case '\\':
                                char c = iterator.next();
                                if (c != CharacterIterator.DONE) {
                                    return "" + ch + c;
                                }
                            // Other characters
                            default:
                                return "" + ch;
                        }
                    }
                    
                    case CHAR: {
                        switch (ch) {
                            // Escape
                            case '\\':
                                char c = iterator.next();
                                if (c != CharacterIterator.DONE) {
                                    return "" + ch + c;
                                }
                            // Other characters
                            default:
                                return "" + ch;
                        }
                    }

                    case MULTI_LINE_COMMENT: {
                        switch (ch) {
                            case '*': {
                                if (iterator.next() == '/') {
                                    return "*/";
                                } else {
                                    iterator.previous();
                                    return "*";
                                }
                            }
                        }
                    }
                    case SINGLE_LINE_COMMENT: {
                        return "" + ch;
                    }
                }
            }
        }
        
        throw new RuntimeException("Did not handle character: " + ch);
    }
    
    public String translate(String code) {
        syntax = new Syntax();
        boolean start = false;
        StringBuffer buf = new StringBuffer();
        StringBuffer tmp = new StringBuffer();
        StringCharacterIterator it = new StringCharacterIterator(code.trim());
        for(char c = it.current(); c != CharacterIterator.DONE; c = it.next()) {
            String token = parseToken(it);
            
            
            // Handle comments by buffering them.
            switch (state) {
                case NORMAL:
                    buf.append(token);
                    break;
                case SINGLE_LINE_COMMENT:
                    tmp.append(token);
                    if (token.equals(getNewLineStandin())) {
                        state = ParseState.NORMAL;
                        buf.append(syntax.commentToHTMLTag(tmp.toString()));
                        tmp.delete(0, tmp.length());
                        tmp.trimToSize();
                    }
                    
                    break;
                case MULTI_LINE_COMMENT:
                    tmp.append(token);
                    if (token.equals("*/")) {
                        state = ParseState.NORMAL;
                        buf.append(syntax.commentToHTMLTag(tmp.toString()));
                        tmp.delete(0, tmp.length());
                        tmp.trimToSize();
                    }
                    
                    break;
                case STRING:
                    tmp.append(token);
                    if (token.equals("\"") && start) {
                        state = ParseState.NORMAL;
                        buf.append(syntax.stringToHTMLTag(tmp.toString()));
                        tmp.delete(0, tmp.length());
                        tmp.trimToSize();
                        start = false;
                    } else {
                        start = true;
                    }
                    
                    break;
                case CHAR:
                    tmp.append(token);
                    if (token.equals("\'") && start) {
                        state = ParseState.NORMAL;
                        buf.append(syntax.stringToHTMLTag(tmp.toString()));
                        tmp.delete(0, tmp.length());
                        tmp.trimToSize();
                        start = false;
                    } else {
                        start = true;
                    }
                    
                    break;
            }
        }
        
        LOG.info(HTMLGenerator.generate(code));
        return buf.toString();
    }
    private static final Logger LOG = Logger.getLogger(Lang2HTML.class.getName());
    
    
}
