/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser.Model.Templates;

import edu.bloomu.codeglosser.Model.TreeViewLeaf;
import java.awt.Color;
import java.util.logging.Logger;
import org.json.simple.JSONObject;

/**
 *
 * @author Louis
 */
public class TemplateLeaf implements TreeViewLeaf {
    
    
    private static final Logger LOG = Logger.getLogger(TemplateLeaf.class.getName());
    
    private final MarkupTemplate template;

    public TemplateLeaf(MarkupTemplate template) {
        LOG.info("Parsing Leaf:" + template);
        this.template = template;
    }

    public MarkupTemplate getTemplate() {
        return template;
    }

    @Override
    public String toString() {
        return template.toString();
    }
}
