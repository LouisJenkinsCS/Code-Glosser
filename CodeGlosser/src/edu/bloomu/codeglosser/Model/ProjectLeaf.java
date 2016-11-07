/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser.Model;

import java.io.File;
import java.util.logging.Logger;

/**
 *
 * @author Louis
 */
public class ProjectLeaf implements TreeViewLeaf {

    private static final Logger LOG = Logger.getLogger(ProjectLeaf.class.getName());
    
    private File file;
    
    ProjectLeaf(File file) {
        this.file = file;
        
        LOG.info("Leaf: " + file.getName());
        if (!file.isFile()) {
            LOG.severe("Not a valid file: " + file.getName());
            throw new RuntimeException("Not a valid file: " + file.getName());
        }
    }

    @Override
    public String toString() {
        return this.file.getName();
    }
    
    public File getFile() {
        return file;
    }
}
