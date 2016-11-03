/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser.Model;

/**
 *
 * @author Louis
 */
public interface TreeViewBranch extends TreeViewNode {
    TreeViewNode[] getChildren();
}
