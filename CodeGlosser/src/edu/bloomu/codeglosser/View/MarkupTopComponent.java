/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser.View;

import com.google.common.eventbus.EventBus;
import edu.bloomu.codeglosser.Session.MarkupManager;
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
    private final EventBus bus = new EventBus();
    

    public MarkupTopComponent(File fileOrProject) {
        setDisplayName("Markup Window");
        setLayout(new BorderLayout());
        LOG.info("URI: " + fileOrProject.toURI());
        MarkupManager.setURIPrefix(fileOrProject.toURI());
        MarkupView v = new MarkupView(fileOrProject, bus);
        add(v, BorderLayout.CENTER);
    }
}
