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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.stream.Collectors;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author Louis Jenkins
 * 
 * Handles parsing the *_syntax.json files that was converted from GeSHi's implementation
 * of the syntax highlighter.
 */
public class Syntax {
    
    private final static String STRINGS_KEY = "STRINGS";
    private final static String NUMBERS_KEY = "NUMBERS";
    private final static String METHODS_KEY = "METHODS";
    private final static String COMMENTS_KEY = "COMMENTS";
    private final static String KEYWORD_KEY = "KEYWORDS";
    private final static String STYLES_KEY = "STYLES";
    
    
    private final HashMap<String, String> keyword = new HashMap<>();
    
    private final String commentsStyle;
    private final String stringsStyle;
    private final String numbersStyle;
    private final String methodsStyle;
    
    /**
     * Converts the given word to an HTML tag. If there is no style attribute set
     * for the word, it will return it unaltered.
     * @param word Word to convert to HTML tag.
     * @return HTML Tag or word if no present mapping.
     */
    public String wordToHTMLTag(String word) {
        String tag = word;
        if (keyword.containsKey(word)) {
            tag = "<span style=\"" + keyword.get(word) + "\">" + word + "</span>";
        }
        
        return tag;
    }
    
    public String stringToHTMLTag(String string) {
        return "<span style=\"" + stringsStyle + "\">" + string + "</span>";
    }
    
    public String numberToHTMLTag(String number) {
        return "<span style=\"" + numbersStyle + "\">" + number + "</span>";
    }
    
    public String methodToHTMLTag(String method) {
        return "<span style=\"" + methodsStyle + "\">" + method + "</span>";
    }
    
    public String commentToHTMLTag(String comment) {
        return "<span style=\"" + commentsStyle + "\">" + comment + "</span>";
    }
    
    public Syntax() {
        // Load .java language file.
        Path path = Paths.get("src", "edu", "bloomu", "codeglosser", "html", "syntax", "java_syntax.json");
        File file = path.toFile();
        if (!file.exists()) {
            throw new RuntimeException("java_syntax.json did not exist!");
        }
        
        try {
            // Parse...
            JSONObject obj = (JSONObject) new JSONParser().parse(
                    Files.readAllLines(path)
                            .stream()
                            .collect(Collectors.joining("\n"))
            );
            
            // All keywords are further mapped to numbers which add separate styles
            JSONObject keywordMapping = (JSONObject) obj.get(KEYWORD_KEY);
            JSONObject stylesMapping = (JSONObject) obj.get(STYLES_KEY);
            JSONObject keywordStyles = (JSONObject) stylesMapping.get(KEYWORD_KEY);
            JSONObject commentStyles = (JSONObject) stylesMapping.get(COMMENTS_KEY);
            JSONArray stringsStyles = (JSONArray) stylesMapping.get(STRINGS_KEY);
            JSONArray numbersStyles = (JSONArray) stylesMapping.get(NUMBERS_KEY);
            JSONObject methodsStyles = (JSONObject) stylesMapping.get(METHODS_KEY);
            
            
            // Create styling mappings
            commentsStyle = (String) commentStyles.get("1");
            stringsStyle = (String) stringsStyles.get(0);
            numbersStyle = (String) numbersStyles.get(0);
            methodsStyle = (String) methodsStyles.get("1");
            
            
            // Create keyword styling mappings
            for (int i = 1; ; i++) {
                String key = Integer.toString(i);
                if (!keywordMapping.containsKey(key)) {
                    break;
                }
                
                JSONArray mapping = (JSONArray) keywordMapping.get(key);
                String style = (String) keywordStyles.get(key);
                
                
                mapping.stream().forEach(word -> keyword.put((String) word, style));
            }
        } catch (IOException ex) {
            throw new RuntimeException("java_syntax.json did not exist!");
        } catch (ParseException ex) {
            throw new RuntimeException("java_syntax.json could not be properly parsed... " + ex.getMessage());
        }
    }
}
