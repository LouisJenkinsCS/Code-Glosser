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
package edu.bloomu.codeglosser.Model.Templates;

import edu.bloomu.codeglosser.Globals;
import edu.bloomu.codeglosser.Model.TreeViewBranch;
import edu.bloomu.codeglosser.Model.TreeViewNode;
import java.util.HashMap;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 *
 * @author Louis
 */
public class TemplateBranch implements TreeViewBranch {
    
    protected static final String KEY_BODY = "body";
    protected static final String KEY_CATEGORY = "category";
    protected static final String KEY_TITLE = "title";
    
    private static final Logger LOG = Globals.LOGGER;
    
    private final HashMap<String, TreeViewNode> childrenMap = new HashMap<>();
    private final String title;
    
    public TemplateBranch(JSONObject data) {
        LOG.fine("Parsing Branch: " + data.toJSONString());
        this.title = (String) data.get(KEY_TITLE);
        
        JSONArray arr = (JSONArray) data.get(KEY_BODY);
        TreeViewNode[] children = new TreeViewNode[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            children[i] = TemplateNodeFactory.getTemplateNode((JSONObject) arr.get(i));
        }
        
        childrenMap.putAll(
                Stream.of(children)
                .collect(Collectors
                        .toMap(TreeViewNode::toString, Function.identity())
                )
        );
    }

    @Override
    public TreeViewNode[] getChildren() {
        return childrenMap.values().toArray(new TreeViewNode[childrenMap.size()]);
    }

    @Override
    public String toString() {
        return title;
    }
    
    
}
