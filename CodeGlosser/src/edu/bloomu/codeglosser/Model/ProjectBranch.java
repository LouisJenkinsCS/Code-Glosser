/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser.Model;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 *
 * @author Louis
 */
public class ProjectBranch implements TreeViewBranch {

    private static final Logger LOG = Logger.getLogger(ProjectBranch.class.getName());
    
    private TreeViewNode[] children;
    private final String dirName;
    
    public ProjectBranch(File directory) {
        this.dirName = directory.getName();
        LOG.info("Branch: " + directory.getName());
        if (!directory.isDirectory()) {
            LOG.severe("Not a valid directory: " + directory.getName());
            throw new RuntimeException("Not a valid directory: " + directory.getName());
        }
        
        ArrayList<TreeViewNode> children = new ArrayList<>();
        for (final File f : directory.listFiles()) {
            if (f.isDirectory()) {
                children.add(new ProjectBranch(f));
            } else {
                children.add(new ProjectLeaf(f));
            }
        }
        
        this.children = children.toArray(new TreeViewNode[children.size()]);
    }

    @Override
    public String toString() {
        return this.dirName;
    }
    
    
    @Override
    public TreeViewNode[] getChildren() {
        return this.children;
    }
    
}
