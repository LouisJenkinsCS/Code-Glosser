/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser.Events;

import edu.bloomu.codeglosser.Utils.HTMLGenerator;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 *
 * @author Louis
 */
public class FileChangeEvent {
    private final File file;
    
    public static FileChangeEvent of(File file) {
        return new FileChangeEvent(file);
    }
    
    public FileChangeEvent(File file) {
        this.file = file;
    }

    public String getFileName() {
        return HTMLGenerator.relativeFileName(this.file);
    }
    
    public String getFileContents() throws IOException {
        return new String(Files.readAllBytes(file.toPath())).replace("\r\n", "\n");
    }
}
