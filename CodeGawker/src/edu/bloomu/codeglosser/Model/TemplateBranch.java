/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser.Model;

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
    
    public static final String KEY_BODY = "body";
    public static final String KEY_CATEGORY = "category";
    
    private static final Logger LOG = Logger.getLogger(TemplateBranch.class.getName());
    
    private final HashMap<String, TreeViewNode> childrenMap = new HashMap<>();
    private final String title;
    
    public TemplateBranch(JSONObject data) {
        LOG.info("Parsing Branch: " + data.toJSONString());
        this.title = (String) data.get(KEY_TITLE);
        
        JSONArray arr = (JSONArray) data.get(KEY_BODY);
        TreeViewNode[] children = new TreeViewNode[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            JSONObject obj = (JSONObject) arr.get(i);
            // Is another branch...
            if (obj.containsKey(KEY_CATEGORY)) {
                children[i] = new TemplateBranch(obj);
            } else {
                children[i] = new TemplateLeaf((JSONObject) arr.get(i));
            }
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
