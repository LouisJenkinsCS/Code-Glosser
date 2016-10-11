/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser.Utils;


/**
 *
 * @author Louis
 */
public class HTMLGenerator {
    public static String generate(String title, String code) {
        StringBuilder builder = new StringBuilder();
        builder
                .append("<html>\n")
                .append("    <head>\n")
                .append("        <link rel=\"stylesheet\" href=\"src/edu/bloomu/codeglosser/HTML/styles/default.css\">\n")
                .append("        <script src=\"src/edu/bloomu/codeglosser/HTML/highlight.pack.js\"></script>\n")
                .append("        <script>hljs.initHighlightingOnLoad();</script>\n")
                .append("        <title>")
                .append(title)
                .append("</title>\n")
                .append("        <meta charset=\"UTF-8\">\n")
                .append("    </head>\n")
                .append("    <body>\n")
                .append("        <pre>            \n")
                .append("            <code class=\"java\">\n")
                .append(code)
                .append("</code>\n")
                .append("        </pre>\n")
                .append("    </body>\n")
                .append("</html>");
        
        return builder.toString();
    }
    
}
