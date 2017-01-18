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
package edu.bloomu.codeglosser.Model;

import edu.bloomu.codeglosser.Globals;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 *
 * @author Louis
 */
public class ProjectBranch implements TreeViewBranch {

    private static final Logger LOG = Globals.LOGGER;
    
    private TreeViewNode[] children;
    private final String dirName;
    
    public ProjectBranch(File directory) {
        this.dirName = directory.getName();
        LOG.fine("Branch: " + directory.getName());
        if (!directory.isDirectory()) {
            LOG.severe("Not a valid directory: " + directory.getName());
            throw new RuntimeException("Not a valid directory: " + directory.getName());
        }
        
        ArrayList<TreeViewNode> children = new ArrayList<>();
        // Filter non-Java files
        for (final File f : directory.listFiles()) {
            if (f.isDirectory()) {
                ProjectBranch dir = new ProjectBranch(f);
                if (dir.getChildren().length > 0) {
                    children.add(dir);
                }
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
