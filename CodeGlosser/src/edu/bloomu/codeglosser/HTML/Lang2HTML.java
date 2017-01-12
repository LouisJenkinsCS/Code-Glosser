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
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JTextPane;

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
    
    WebView wv;
    
    public Observable<String> translate(final String code) {
        PublishSubject<String> pageLoaded = PublishSubject.create();
        Platform.runLater(() -> {
            String html = "<title>Test</title>"
                    + "<meta charset=\"UTF-8\">"
                    + "</head>"
                    + "<body>"
                    + "<pre><code>" + code.replace(">", "&gt").replace("<", "&lt") + "</code>" + "</pre>" 
                    + "</body>" + "</html>";
            wv = new WebView();
            WebEngine eng = wv.getEngine();

    //        btn.setText("Say 'Hello World'");
    //        btn.setOnAction(new EventHandler<ActionEvent>() {
    //            
    //            @Override
    //            public void handle(ActionEvent event) {
    //                System.out.println("Hello World!");
    //            }
    //        });

            StackPane root = new StackPane();
            root.getChildren().add(wv);

            Scene scene = new Scene(root, 300, 250);

            ContextMenu menu = new ContextMenu();
            MenuItem item = new MenuItem("Create");
            item.setOnAction(e -> {
                String newCode = generateHTML2((String) eng.executeScript("document.getElementById('code_segment').innerHTML"));
                System.out.println(newCode);
                final JDialog dialog = new JDialog();
                dialog.setModal(true);
                JTextPane pane = new JTextPane();
                pane.setText(newCode);
                dialog.add(pane);
                dialog.setVisible(true);
            });
            menu.getItems().add(item);

            wv.setContextMenuEnabled(false);
            wv.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.SECONDARY) {
                    menu.show(wv, e.getSceneX(), e.getScreenY());
                }
            });


            eng.getLoadWorker().stateProperty().addListener((ov,oldState,newState) -> {

                switch (newState) {
                    case SCHEDULED:
                        System.out.println("state: scheduled");
                        break;
                    case RUNNING:
                        System.out.println("state: running");
                        break;
                    case SUCCEEDED:
                        System.out.println("state: succeeded");
                        break;
                    default:
                        System.out.println("state: " + newState);
                        break;
                }
                if (newState == Worker.State.SUCCEEDED) {
                    String newCode = (String) eng.executeScript("document.getElementById('code_segment').innerHTML");
                    pageLoaded.onNext(newCode);
                }
            });

            eng.loadContent(generateHTML(code, true));
        });
//        syntax = new Syntax();
//        boolean start = false;
//        StringBuffer buf = new StringBuffer();
//        StringBuffer tmp = new StringBuffer();
//        StringCharacterIterator it = new StringCharacterIterator(code.trim());
//        for(char c = it.current(); c != CharacterIterator.DONE; c = it.next()) {
//            String token = parseToken(it);
//            
//            
//            // Handle comments by buffering them.
//            switch (state) {
//                case NORMAL:
//                    buf.append(token);
//                    break;
//                case SINGLE_LINE_COMMENT:
//                    tmp.append(token);
//                    if (token.equals(getNewLineStandin())) {
//                        state = ParseState.NORMAL;
//                        buf.append(syntax.commentToHTMLTag(tmp.toString()));
//                        tmp.delete(0, tmp.length());
//                        tmp.trimToSize();
//                    }
//                    
//                    break;
//                case MULTI_LINE_COMMENT:
//                    tmp.append(token);
//                    if (token.equals("*/")) {
//                        state = ParseState.NORMAL;
//                        buf.append(syntax.commentToHTMLTag(tmp.toString()));
//                        tmp.delete(0, tmp.length());
//                        tmp.trimToSize();
//                    }
//                    
//                    break;
//                case STRING:
//                    tmp.append(token);
//                    if (token.equals("\"") && start) {
//                        state = ParseState.NORMAL;
//                        buf.append(syntax.stringToHTMLTag(tmp.toString()));
//                        tmp.delete(0, tmp.length());
//                        tmp.trimToSize();
//                        start = false;
//                    } else {
//                        start = true;
//                    }
//                    
//                    break;
//                case CHAR:
//                    tmp.append(token);
//                    if (token.equals("\'") && start) {
//                        state = ParseState.NORMAL;
//                        buf.append(syntax.stringToHTMLTag(tmp.toString()));
//                        tmp.delete(0, tmp.length());
//                        tmp.trimToSize();
//                        start = false;
//                    } else {
//                        start = true;
//                    }
//                    
//                    break;
//            }
//        }
//        
//        LOG.info(HTMLGenerator.generate(code));
        return pageLoaded;
    }
    
    public static String generateHTML2(String code) {
        StringBuilder builder;
        builder = new StringBuilder()
                .append("<html><head><style>.hljs {\n" +
"  display: block;\n" +
"  overflow-x: auto;\n" +
"  padding: 0.5em;\n" +
"  color: #383a42;\n" +
"  background: #fafafa;\n" +
"}\n" +
"\n" +
".hljs-comment,\n" +
".hljs-quote {\n" +
"  color: #a0a1a7;\n" +
"  font-style: italic;\n" +
"}\n" +
"\n" +
".hljs-doctag,\n" +
".hljs-keyword,\n" +
".hljs-formula {\n" +
"  color: #a626a4;\n" +
"}\n" +
"\n" +
".hljs-section,\n" +
".hljs-name,\n" +
".hljs-selector-tag,\n" +
".hljs-deletion,\n" +
".hljs-subst {\n" +
"  color: #e45649;\n" +
"}\n" +
"\n" +
".hljs-literal {\n" +
"  color: #0184bb;\n" +
"}\n" +
"\n" +
".hljs-string,\n" +
".hljs-regexp,\n" +
".hljs-addition,\n" +
".hljs-attribute,\n" +
".hljs-meta-string {\n" +
"  color: #50a14f;\n" +
"}\n" +
"\n" +
".hljs-built_in,\n" +
".hljs-class .hljs-title {\n" +
"  color: #c18401;\n" +
"}\n" +
"\n" +
".hljs-attr,\n" +
".hljs-variable,\n" +
".hljs-template-variable,\n" +
".hljs-type,\n" +
".hljs-selector-class,\n" +
".hljs-selector-attr,\n" +
".hljs-selector-pseudo,\n" +
".hljs-number {\n" +
"  color: #986801;\n" +
"}\n" +
"\n" +
".hljs-symbol,\n" +
".hljs-bullet,\n" +
".hljs-link,\n" +
".hljs-meta,\n" +
".hljs-selector-id,\n" +
".hljs-title {\n" +
"  color: #4078f2;\n" +
"}\n" +
"\n" +
".hljs-emphasis {\n" +
"  font-style: italic;\n" +
"}\n" +
"\n" +
".hljs-strong {\n" +
"  font-weight: bold;\n" +
"}\n" +
"\n" +
".hljs-link {\n" +
"  text-decoration: underline;\n" +
"}\n" +
"</style>")
                
                .append("        <title>")
                .append("Test")
                .append("</title>")
                .append("        <meta charset=\"UTF-8\">")
                .append("    </head>")
                .append("    <body>")
                .append("        <pre>")
                .append("<code>")
                .append(code)
                .append("</code>")
                .append("</pre>")
                .append("    </body>")
                .append("</html>");
        
        return builder.toString();
    }
    
    public static String generateHTML(String code, boolean script) {
        StringBuilder builder;
        builder = new StringBuilder()
                .append("<html>")
                .append("    <head>")
                .append("        <link rel=\"stylesheet\" href=\"https://cdnjs.cloudflare.com/ajax/libs/highlight.js/9.7.0/styles/atom-one-light.min.css\"></script>")
                .append(!script ? "" : "        <script src=\"https://cdnjs.cloudflare.com/ajax/libs/highlight.js/9.7.0/highlight.min.js\"></script>")
                .append(!script ? "" : "        <script>hljs.initHighlightingOnLoad();</script>")
                .append("<style>.note {\n")
                .append("  display: inline;\n" )
                .append("  position: relative;\n" )
                .append("  background-color: yellow;\n" )
                .append("}\n" )
                .append("\n" )
                .append(".note:hover:after {\n" )
                .append("  background: #333;\n" )
                .append("  background: rgba(0,0,0,.8);\n" )
                .append("  border-radius: 5px;\n" )
                .append("  bottom: 26px;\n" )
                .append("  white-space: pre-wrap;\n" )
                .append("  color: #fff;\n" )
                .append("  content: attr(msg);\n" )
                .append("  left: 20%;\n" )
                .append("  padding: 5px 15px;\n" )
                .append("  position: absolute;\n" )
                .append("  z-index: 98;\n" )
                .append("  width:500px;\n" )
                .append("  display:block;\n" )
                .append("  word-wrap: normal;\n" )
                .append("}\n" )
                .append("\n" )
                .append(".note:hover:before{\n" )
                .append("  border:solid;\n" )
                .append("  border-color: #333 transparent;\n" )
                .append("  border-width: 6px 6px 0 6px;\n" )
                .append("  bottom: 20px;\n" )
                .append("  content: \"\";\n" )
                .append("  left: 50%;\n" )
                .append("  position: absolute;\n" )
                .append("  display:block;\n" )
                .append("  z-index: 99;\n" )
                .append("}</style>")
                .append("        <title>")
                .append("Test")
                .append("</title>")
                .append("        <meta charset=\"UTF-8\">")
                .append("    </head>")
                .append("    <body contenteditable=\"false\">")
                .append("        <pre>")
                .append("<code id=\"code_segment\">")
                .append(code.replace(">", "&gt").replace("<", "&lt"))
                .append("</code>")
                .append("</pre>")
                .append("    </body>")
                .append("</html>");
        
        return builder.toString();
    }
    private static final Logger LOG = Logger.getLogger(Lang2HTML.class.getName());
    
    
}
