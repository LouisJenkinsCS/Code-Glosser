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

import edu.bloomu.codeglosser.Globals;
import edu.bloomu.codeglosser.Utils.FileUtils;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

/**
 *
 * @author Louis
 */
public class Lang2HTML {
    
    private static final Logger LOG = Logger.getLogger(Lang2HTML.class.getName());
    
    private WebView wv;

    public Lang2HTML() {
        
    }

    public Observable<String> translate(final String code) {
        PublishSubject<String> pageLoaded = PublishSubject.create();
        String ext = FileUtils.getExtension(Globals.CURRENT_FILE);
        Platform.runLater(() -> {
            wv = new WebView();
            WebEngine eng = wv.getEngine();

            StackPane root = new StackPane();
            root.getChildren().add(wv);

            Scene scene = new Scene(root, 300, 250);

            eng.getLoadWorker().stateProperty().addListener((ov,oldState,newState) -> {
                if (newState == Worker.State.SUCCEEDED) {
                    String newCode = (String) eng.executeScript("document.getElementById('code_segment').innerHTML");
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
                .append(code.replace(">", "&gt").replace("<", "&lt"))
                .append("</code>")
                .append("</pre>")
                .append("    </body>")
                .append("</html>");
        
        return builder.toString();
    } 
}
