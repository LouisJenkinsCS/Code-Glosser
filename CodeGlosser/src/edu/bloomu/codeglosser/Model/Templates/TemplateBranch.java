/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser.Model.Templates;

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
    
    private static final Logger LOG = Logger.getLogger(TemplateBranch.class.getName());
    
    private final HashMap<String, TreeViewNode> childrenMap = new HashMap<>();
    private final String title;
    
    public TemplateBranch(JSONObject data) {
        LOG.info("Parsing Branch: " + data.toJSONString());
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
