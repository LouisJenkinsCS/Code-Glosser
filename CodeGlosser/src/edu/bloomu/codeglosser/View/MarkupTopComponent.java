/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser.View;

import edu.bloomu.codeglosser.Controller.MarkupController;
import edu.bloomu.codeglosser.Globals;
import java.awt.BorderLayout;
import java.io.File;
import java.util.logging.Logger;
import org.openide.windows.TopComponent;

/**
 *
 * @author Louis
 */
public class MarkupTopComponent extends TopComponent {

    private static final Logger LOG = Logger.getLogger(MarkupTopComponent.class.getName());
    
    private static final char SYM = '\u2691'; // flag
    

    public MarkupTopComponent(File project) {
        LOG.info("Initializing for project: " + project.getName());
        setDisplayName("Select Project File...");
        setLayout(new BorderLayout());
        
        // Setup global data
        Globals.initGlobals();
        Globals.PROJECT_FOLDER = project.toPath();
        Globals.URI_PREFIX = project.toURI();
        
        // Create the main components
        MarkupController controller = new MarkupController();
        MarkupView view = new MarkupView();
        MarkupProperties properties = new MarkupProperties();
        
        // Connect the MarkupController to the MarkupView and MarkupProperty
        controller.addEventSource(view.getEventSource());
        controller.addEventSource(properties.getEventSource());
        view.addEventSource(controller.getEventSource());
        properties.addEventSource(controller.getEventSource());
        
        add(view, BorderLayout.CENTER);
        add(properties, BorderLayout.LINE_END);
    }
}
