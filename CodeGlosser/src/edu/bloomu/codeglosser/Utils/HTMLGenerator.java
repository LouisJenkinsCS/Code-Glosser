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
package edu.bloomu.codeglosser.Utils;

import edu.bloomu.codeglosser.Globals;
import edu.bloomu.codeglosser.Model.Markup;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.json.simple.JSONObject;



/**
 *
 * @author Louis
 */
public class HTMLGenerator {

    private static final Logger LOG = Logger.getLogger(HTMLGenerator.class.getName());
    
    public static String generate(String code) {
        return "";
    }
    
    private static WebView webview;
    
    public static Observable<String> toHighlighted(final String code) {
        PublishSubject<String> pageLoaded = PublishSubject.create();
        // Execute the library and retrieve its fully syntax highlighted web page
        // in a JavaFX background thread. The WebView is headless, so the user does
        // not notice it load.
        Platform.runLater(() -> {
            webview = new WebView();
            WebEngine eng = webview.getEngine();
            
            // Necessary to ensure that the WebView is initialized properly.
            StackPane root = new StackPane();
            root.getChildren().add(webview);
            Scene scene = new Scene(root, 300, 250);

            eng.getLoadWorker().stateProperty().addListener((ov,oldState,newState) -> {
                if (newState == Worker.State.SUCCEEDED) {
                    String newCode = (String) eng.executeScript("document.getElementById('code_segment').innerHTML");
                    eng.getLoadWorker().cancel();
                    pageLoaded.onNext(newCode);
                }
            });
            
            String html = generateHTML(code);
            try {
                File file = FileUtils.temporaryFile("html.html", html);
                String css = FileUtils.readAll("HTML/styles.css");
                FileUtils.temporaryFile("styles.css", css);
                String js = FileUtils.readAll("HTML/highlight.pack.js");
                FileUtils.temporaryFile("script.js", js);
                eng.load(file.toURI().toURL().toString());
            } catch (IOException ex) {
                throw new RuntimeException("Error while attempting to create temporary file!");
            }
        });
        
        return pageLoaded;
    }
    
    public static String generateHTML(String code) {
        StringBuilder builder;
        builder = new StringBuilder()
                .append("<html><head><link rel=\"stylesheet\" href=\"styles.css\"></style>")
                .append("<script src=\"script.js\"></script>")
                .append("<script>")
                .append("hljs.initHighlightingOnLoad();")
                .append("</script>")
                .append("        <title>")
                .append("Test")
                .append("</title>")
                .append("        <meta charset=\"UTF-8\">")
                .append("    </head>")
                .append("    <body contenteditable=\"false\">")
                .append("<pre>")
                .append("<code id=\"code_segment\" class=\"" + FileUtils.getExtension(Globals.CURRENT_FILE) + "\">")
                .append(code)
                .append("</code>")
                .append("</pre>")
                .append("    </body>")
                .append("</html>");
        
        return builder.toString();
    }
    
    private static String fileToString(File file) {
        return Globals.PROJECT_FOLDER.relativize(file.toPath()).toString();
    }
    
    public static Observable<String> syntaxHighlight(String code, String title, List<Markup> markups) {
        return Observable
                .just(code)
                .map(code_ -> code_.replace("&", "&amp;").replace(">", "&gt;").replace("<", "&lt;"))
                .map(safeCode -> toMarkedUp(safeCode, markups))
                .flatMap(markedUpCode -> toHighlighted(markedUpCode))
                .map(highlightedCode -> toPresentation(highlightedCode, title));
    }
    
    /**
     * Converts the syntax highlighted code to it's final presentation form. This involves
     * inserting the required header for the visible markups, as well as inserting the required
     * CSS needed to render it.
     * @param html
     * @return 
     */
    private static String toPresentation(String html, String title) {
        String stylesCss = FileUtils.readAll("HTML/styles.css");
        String markupCss = FileUtils.readAll("HTML/markup.css");
        String presentationHTML = "<html><head><title>" + title + "</title>"
                + "<style>" + stylesCss + "</style><style>" + markupCss + "</style>"
                + "<meta charset=\"UTF-8\"></head><body>" + "<pre><code>" 
                + html + "</code></pre></body></html>";
        return presentationHTML;
    }
    
    public static boolean isValidDirectory(File dir, JSONObject data) {
        for (File f : dir.listFiles()) {
            if (f.isDirectory() && isValidDirectory(f, data)) {
                return true;
            } else if (data.containsKey(fileToString(f))) {
                return true;
            }
        }
        return false;
    }
    
    public static void generateDirectory(File dir, JSONObject data, ZipOutputStream stream) throws IOException {
        LOG.info("Archiving Directory: " + dir);
        
        // Only generate if directory contains a file that is marked up.
        if (!isValidDirectory(dir, data)) {
            LOG.info("Not a valid directory...");
            return;
        }
        
        // Directories must end with a slash.
        String name = fileToString(dir);
        name = name.endsWith("/") ? name : name + "/";
        stream.putNextEntry(new ZipEntry(name));
        
        for (final File f : dir.listFiles()) {
            if (f.isDirectory()) {
                generateDirectory(f, data,  stream);
            } else if (data.containsKey(fileToString(f))){
                generateFile(f, data, stream);
            }
        }
    }
    
    public static void generateFile(File file, JSONObject data, ZipOutputStream ostream) {
        try {
            LOG.info("Archiving file: " + file);
            // Copy contents into zip file (We replace carriage returns for standard newlines)
            String title = fileToString(file);
            Globals.CURRENT_FILE = file.toPath();
            
            String code = new String(Files.readAllBytes(file.toPath())).replace("\r\n", "\n");
            String result = syntaxHighlight(code, title, SessionManager.loadSession(file.toPath())).blockingFirst();
            ostream.putNextEntry(new ZipEntry(title + ".html"));
            ostream.write(result.getBytes());
            ostream.closeEntry();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    private static void generateAll(JSONObject data) {
        String zipFileName = Globals.PROJECT_FOLDER + "\\" + "exported.zip";
        LOG.info(zipFileName);
        try(ZipOutputStream stream = new ZipOutputStream(new FileOutputStream(new File(zipFileName)))) {
            LOG.info("Created zip archive...");
            File dir = Globals.PROJECT_FOLDER.toFile();
            for (final File f : dir.listFiles()) {
                if (f.isDirectory()) {
                    generateDirectory(f, data, stream);
                } else if (data.containsKey(fileToString(f))){
                    generateFile(f, data, stream);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    public static void generateAll() {
        SessionManager
                .getJSONContents()
                .subscribe(HTMLGenerator::generateAll);
    }
    
    private static String toMarkedUp(String code, List<Markup> notes) {
        // Ascending order
        notes.sort((n1, n2) -> n1.getRange().compareTo(n2.getRange()));
        StringBuilder finalCode = new StringBuilder();
        StringCharacterIterator iter = new StringCharacterIterator(code);
        int offset = 0;
        char c = iter.current();
        Queue<Markup> queue = new ArrayDeque<>(notes);
        
        while (!queue.isEmpty()) {
            Markup currentMarkup = queue.remove();
            
            // Add the requested offsets.
            for (Bounds bounds : currentMarkup.getOffsets()) {
                // Add all characters before the markup. We must keep track of any
                // special HTML entities, as it will need to be taken into account.
                while (offset != bounds.getStart() - 1) {
                    offset++;
                    
                    // Check if character begins with '&', which may be beginning of HTML entity
                    if (c == '&') {
                        // If we do not have an HTML entity we need to go back to this...
                        int idx = iter.getIndex();
                        String str = "" + c + iter.next() + iter.next() + iter.next();
                        
                        // Check if it is '<' or '>' replacement
                        if ("&lt;".equals(str) || "&gt;".equals(str)) {
                            finalCode.append(str);
                        } else {
                            // Check for '&' replacement
                            str += iter.next();
                            if ("&amp;".equals(str)) {
                                finalCode.append(str);
                            } else {
                                // At this point, it is not an HTML entity we are looking for. Roll back.
                                iter.setIndex(idx);
                            }
                        }
                    } else {
                        finalCode.append(c);
                    }
                    
                    c = iter.next();
                }
                
                // At this point, we are at the boundary where we need to inject the markup.
                // We need to insert the HTML tags, as well as also keeping track of certain HTML entities.
                String color = String.format("#%02x%02x%02x",
                        currentMarkup.getHighlightColor().getRed(), 
                        currentMarkup.getHighlightColor().getGreen(), 
                        currentMarkup.getHighlightColor().getBlue()
                );
                finalCode
                        .append("<span class=\"note\" msg=\"")
                        .append(currentMarkup.getMsg())
                        .append("\">");
                
                while (offset != bounds.getEnd() - 1) {
                    offset++;
                    
                    // Check if character begins with '&', which may be beginning of HTML entity
                    if (c == '&') {
                        // If we do not have an HTML entity we need to go back to this...
                        int idx = iter.getIndex();
                        String str = "" + c + iter.next() + iter.next() + iter.next();
                        
                        // Check if it is '<' or '>' replacement
                        if ("&lt;".equals(str) || "&gt;".equals(str)) {
                            finalCode.append(str);
                        } else {
                            // Check for '&' replacement
                            str += iter.next();
                            if ("&amp;".equals(str)) {
                                finalCode.append(str);
                            } else {
                                // At this point, it is not an HTML entity we are looking for. Roll back.
                                iter.setIndex(idx);
                            }
                        }
                    } else {
                        finalCode.append(c);
                    }
                    c = iter.next();
                }
                
                // Inject the end tag for the markup.
                finalCode.append("</span>");
            }
        }
        
        // Append rest of code
        for (c = iter.current(); c != CharacterIterator.DONE; c = iter.next()) {
            finalCode.append(c);
        }
        
        return finalCode.toString();
    }
}
