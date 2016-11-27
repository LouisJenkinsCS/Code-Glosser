/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser.Utils;

import com.google.common.io.ByteStreams;
import edu.bloomu.codeglosser.Model.Markup;
import edu.bloomu.codeglosser.Session.MarkupManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.apache.logging.log4j.core.util.IOUtils;
import org.apache.logging.log4j.util.Strings;
import org.openide.util.Exceptions;



/**
 *
 * @author Louis
 */
public class HTMLGenerator {

    private static final Logger LOG = Logger.getLogger(HTMLGenerator.class.getName());
    
    public static String generate(String code) {
        return "";
    }
    
    public static String relativeFileName(File f) {
        return new File(".").toURI().relativize(f.toURI()).getPath();
    }
    
    public static void generateDirectory(File dir, ZipOutputStream stream) throws IOException {
        // Directories must end with a slash.
        String name = relativeFileName(dir);
        name = name.endsWith("/") ? name : name + "/";
        stream.putNextEntry(new ZipEntry(name));
        
        for (final File f : dir.listFiles()) {
            if (f.isDirectory()) {
                generateDirectory(f, stream);
            } else if (f.getName().toLowerCase().endsWith(".java")){
                generateFile(f, stream);
            }
        }
    }
    
    public static void generateFile(File file, ZipOutputStream ostream) {
        try {
            // Copy contents into zip file (We replace carriage returns for standard newlines)
            String title = relativeFileName(file);
            String code = new String(Files.readAllBytes(file.toPath())).replace("\r\n", "\n");
            String result = generate(title, code, MarkupManager.getInstance(title).getAllNotes());
            ostream.putNextEntry(new ZipEntry(title + ".html"));
            ostream.write(result.getBytes());
            ostream.closeEntry();
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
    
    public static void generateAll() {
        try(ZipOutputStream stream = new ZipOutputStream(new FileOutputStream(new File("exported.zip")))) {
            generateDirectory(new File("."), stream);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
    
    public static String generate(String title, String code, List<Markup> notes) {
        StringBuilder builder = new StringBuilder();
//        code = code.replaceAll("&", "&amp;");
//        code = code.replaceAll("<", "&lt;");
//        code = code.replaceAll(">", "&gt;");
        builder
                .append("<html>")
                .append("    <head>")
                .append("        <link rel=\"stylesheet\" href=\"src/edu/bloomu/codeglosser/HTML/styles/atom-one-light.css\">")
                .append("        <script src=\"src/edu/bloomu/codeglosser/HTML/highlight.pack.js\"></script>")
                .append("        <script>hljs.initHighlightingOnLoad();</script>")
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
                .append(title)
                .append("</title>")
                .append("        <meta charset=\"UTF-8\">")
                .append("    </head>")
                .append("    <body>")
                .append("        <pre>")
                .append("<code>")
                .append(generateMarkups(notes, code))
                .append("</code>")
                .append("</pre>")
                .append("    </body>")
                .append("</html>");
        
        return builder.toString();
    }
    
    private static String generateMarkups(List<Markup> notes, String code) {
        // Ascending order
        notes.sort((n1, n2) -> n1.getRange().compareTo(n2.getRange()));
        StringBuilder finalCode = new StringBuilder();
        
        int offset = 0;
        
        for (Markup n : notes) {
            for (Bounds b : n.getOffsets()) {
                // Add all characters up to next markup...
                finalCode.append(code.substring(offset, b.getStart() - 1));

                // Inject the code for the markup.
                String color = String.format("#%02x%02x%02x",
                        n.getHighlightColor().getRed(), n.getHighlightColor().getGreen(), n.getHighlightColor().getBlue());
                finalCode.append("<span class=\"note\" msg=\"").append(n.getMsg()).append("\">");

                // Read in the text in between markups...
                finalCode.append(code.substring(b.getStart() - 1, b.getEnd()));

                // Inject the end tag for the markup.
                finalCode.append("</span>");

                // Update offset for next run.
                offset = b.getEnd();
                LOG.info("Output Note: " + n.getId() + " with message: " + n.getMsg() + " with new offset: " + offset);
            }
        }
        
        if (offset < code.length()) {
            finalCode.append(code.substring(offset));
        }
        
        return finalCode.toString();
    }
}
