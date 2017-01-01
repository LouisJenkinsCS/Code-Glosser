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
//                case '&':
//                    buf.append("&amp;");
//                    break;
//                case '\"':
//                    buf.append("&quot;");
//                    break;
//                case '\'':
//                    buf.append("&apos;");
//                    break;
                case '\t':
                    buf.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
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
                        LOG.fine("Found config for " + str);
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
        
        LOG.info(HTMLGenerator.generate(code));
        return buf.toString();
    }
    private static final Logger LOG = Logger.getLogger(Lang2HTML.class.getName());
    
    
}
