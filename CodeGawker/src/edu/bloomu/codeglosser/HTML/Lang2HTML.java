/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser.HTML;

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
    
    public String translate(String code) {
        StringBuffer buf = new StringBuffer();
        StringCharacterIterator it = new StringCharacterIterator(code.trim());
        for(char c = it.current(); c != CharacterIterator.DONE; c = it.next()) {
            switch (c) {
                case '>':
                    buf.append("&gt;");
                    break;
                case '<':
                    buf.append("&lt;");
                    break;
                case ' ':
                    buf.append("&nbsp;");
                    break;
                case '&':
                    buf.append("&amp;");
                    break;
                case '\"':
                    buf.append("&quot;");
                    break;
                case '\'':
                    buf.append("&apos;");
                    break;
                case '\t':
                    buf.append("&nbsp;&nbsp;&nbsp;&nbsp;");
                    break;
                case '\r':
                    if (it.next() != '\n')
                        it.previous();
                case '\n':
                    buf.append(getNewLineStandin());
                    break;
                default: {
                    if (!Character.isAlphabetic(c)) {
                        buf.append(c);
                        break;
                    }
                    StringBuilder tmp = new StringBuilder().append(c);
                    char ch = it.next();
                    while (Character.isAlphabetic(ch)) {
                        tmp.append(ch);
                        ch = it.next();
                    }
                    it.previous();
                    
                    String str = tmp.toString();
                    DisplayConfig conf = configMap.get(str);
                    if (conf != null) {
                        LOG.info("Found config for " + str);
                        buf.append("<font color=\"#");
                        buf.append(String.format("%06X",conf.getColor().getRGB() & 0xFFFFFF));
                        buf.append("\">");
                        
                        if (conf.getFont() == HTMLFont.BOLD) {
                            buf.append("<b>");
                        }
                    }
                    buf.append(str);
                    if (conf != null) {
                        buf.append("</b></font>");
                    }
                }
                    
            }
        }
        return buf.toString();
    }
    private static final Logger LOG = Logger.getLogger(Lang2HTML.class.getName());
    
    
}
