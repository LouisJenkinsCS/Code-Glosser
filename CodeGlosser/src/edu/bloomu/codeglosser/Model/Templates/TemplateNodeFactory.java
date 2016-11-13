/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser.Model.Templates;

import edu.bloomu.codeglosser.Model.TreeViewNode;
import org.json.simple.JSONObject;

/**
 *
 * @author Louis
 */
public class TemplateNodeFactory {
    public static TreeViewNode getTemplateNode(JSONObject obj) {
        TreeViewNode node;
        
        // Bootstrap the recursive process: Categories get recursively resolved
        if (obj.containsKey(TemplateBranch.KEY_CATEGORY)) {
            node = new TemplateBranch(obj);
        } else {
            MarkupTemplate template = new MarkupTemplate();
            template.deserialize(obj);
            node = new TemplateLeaf(template);
        }
        
        return node;
    }
}
