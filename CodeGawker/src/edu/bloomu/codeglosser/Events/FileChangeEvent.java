/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser.Events;

/**
 *
 * @author Louis
 */
public class FileChangeEvent {
    private String fileName;
    
    public static FileChangeEvent of(String fileName) {
        return new FileChangeEvent(fileName);
    }
    
    public FileChangeEvent(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }
}
