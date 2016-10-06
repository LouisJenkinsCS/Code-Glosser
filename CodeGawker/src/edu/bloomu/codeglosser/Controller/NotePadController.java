/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser.Controller;

import edu.bloomu.codeglosser.Model.NotePadModel;
import edu.bloomu.codeglosser.View.NotePadView;

/**
 *
 * @author Louis
 */
public class NotePadController {
    private final NotePadView view;
    private final NotePadModel model;
    
    public NotePadController() {
        view = new NotePadView();
        model = new NotePadModel();
    }
    
    public void setModelText(String txt) {
        model.setText(txt);
        view.setText(model.toHTML());
    }

    public NotePadView getView() {
        return view;
    }

    public NotePadModel getModel() {
        return model;
    }
    
    
}
