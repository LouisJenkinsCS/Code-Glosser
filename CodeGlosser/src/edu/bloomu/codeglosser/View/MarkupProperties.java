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
package edu.bloomu.codeglosser.View;

import edu.bloomu.codeglosser.Controller.MarkupController;
import edu.bloomu.codeglosser.Events.Event;
import edu.bloomu.codeglosser.Events.EventEngine;
import edu.bloomu.codeglosser.Events.EventHandler;
import edu.bloomu.codeglosser.Model.Markup;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import java.awt.Color;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 *
 * @author Louis Jenkins
 * 
 * The parent of all properties, which manages PropertiesFile, PropertiesTemplate,
 * PropertiesAttributes, and PropertiesSelector. Due to technical difficulties
 * with NetBeans, we reserve space for all attributes and add them manually. 
 */
public class MarkupProperties extends javax.swing.JPanel implements EventHandler {
    
    // MarkupController
    public static final int FILE_SELECTED = 0x1;
    public static final int APPLY_TEMPLATE = 0x2;
    public static final int SELECTED_ID = 0x3;
    
    // PropertySelector
    public static final int CLEAR_SELECTION = 0x1;
    public static final int NEW_SELECTION = 0x2;
    public static final int SET_SELECTION = 0x3;
    public static final int RESTORE_SELECTIONS = 0x4;
    
    // PropertyAttributes
    public static final int CLEAR_ATTRIBUTES = 0x1;
    public static final int SET_ATTRIBUTES = 0x2;
    
    private static final Logger LOG = Logger.getLogger(MarkupProperties.class.getName());
    
    private final EventEngine engine = new EventEngine(this, Event.MARKUP_PROPERTIES);

    public MarkupProperties() {
        // Initialize components
        initComponents();
        initChildren();
    }
    
    @Override
    public Observable<Event> handleEvent(Event e) {
        switch (e.getSender()) {
            case Event.MARKUP_CONTROLLER:
                switch (e.getCustom()) {
                    case MarkupController.NEW_MARKUP:
                        return newMarkup((Markup) e.data);
                    case MarkupController.DISPLAY_MARKUP:
                        return displayMarkup((Markup) e.data);
                    case MarkupController.RESTORE_MARKUPS:
                        return restoreMarkups((List<Markup>) e.data);
                    default:
                        throw new RuntimeException("Bad Custom Tag from MarkupController!");
                }
            case Event.PROPERTIES_ATTRIBUTES:
                switch (e.getCustom()) {
                    case PropertyAttributes.TEXT_CHANGE:
                        return textChange((String) e.data);
                    default:
                        throw new RuntimeException("Bad Custom Tag from PropertyAttributes!");
                }
            case Event.PROPERTIES_FILES:
                switch (e.getCustom()) {
                    case PropertyFiles.FILE_SELECTED:
                        return fileSelected((Path) e.data);
                    default:
                        throw new RuntimeException("Bad Custom Tag from PropertyFiles!");
                }
            case Event.PROPERTIES_SELECTOR:
                switch (e.getCustom()) {
                    case PropertySelector.SELECTED_ID:
                        return selectedId((String) e.data);
                    default:
                        throw new RuntimeException("Bad Custom Tag from PropertySelector!");
                }
            case Event.PROPERTIES_TEMPLATES:
                switch (e.getCustom()) {
                    case PropertyTemplates.APPLY_TEMPLATE:
                        return applyTemplate((Markup) e.data);
                }
            default:
                throw new RuntimeException("Bad Sender!");
        }
    }

    @Override
    public EventEngine getEventEngine() {
        return engine;
    }
    
    private void initChildren() {
        engine.register(propertyFiles.getEventEngine());
        engine.register(propertyAttributes.getEventEngine());
        engine.register(propertySelector.getEventEngine());
        engine.register(propertyTemplates.getEventEngine());
    }
    
    private Observable<Event> displayMarkup(Markup markup) {
        LOG.info("Handling displaying Markup: " + markup);
        
        return Observable.just(
                Event.of(Event.MARKUP_PROPERTIES, Event.PROPERTIES_ATTRIBUTES, SET_ATTRIBUTES, markup),
                Event.of(Event.MARKUP_PROPERTIES, Event.PROPERTIES_SELECTOR, SET_SELECTION, markup.getId())
        );
    }
    
    private Observable<Event> selectedId(String id) {
        LOG.info("Propagating event for markup selection: " + id);
        
        return Observable.just(Event.of(Event.MARKUP_PROPERTIES, Event.MARKUP_CONTROLLER, SELECTED_ID, id));
    }
    
    /**
     * Forwards a partially filled template to the MarkupController. Is used for any changes
     * to the current markup.
     * @param template Partially filled Markup
     * @return Observable to emit event.
     */
    private Observable<Event> applyTemplate(Markup template) {
        LOG.info("Propagating event for applying template: " + template);
        
        // Make the change as a template
        return Observable.just(
                Event.of(Event.MARKUP_PROPERTIES, Event.MARKUP_CONTROLLER, APPLY_TEMPLATE, template),
                Event.of(Event.MARKUP_PROPERTIES, Event.PROPERTIES_ATTRIBUTES, SET_ATTRIBUTES, template)
        );
    }
    
    private Observable<Event> textChange(String text) {
        return applyTemplate(Markup.template(text));
    }
    
    private Observable<Event> colorChange(Color c) {
        return applyTemplate(Markup.template(c));
    }
    
    /**
     * Notifies the Controller of file, selector to clear it's adapter, and attributes to clear to a default state.
     * @param fileName File path selected.
     * @return 
     */
    private Observable<Event> fileSelected(Path filePath) {
        LOG.info("Propagating event for file selection: " + filePath);
        
        // Notify controller, selector, and attributes.
        return Observable.just(
                Event.of(Event.MARKUP_PROPERTIES, Event.MARKUP_CONTROLLER, FILE_SELECTED, filePath),
                Event.of(Event.MARKUP_PROPERTIES, Event.PROPERTIES_SELECTOR, CLEAR_SELECTION, null),
                Event.of(Event.MARKUP_PROPERTIES, Event.PROPERTIES_ATTRIBUTES, CLEAR_ATTRIBUTES, null)                
        );
    }
    
    /**
     * Updates the PropertyAttributes and PropertySelector components of new Markup.
     * @param markup Markup
     * @return 
     */
    private Observable<Event> newMarkup(Markup markup) {
        LOG.info("Propagating event for new markup: " + markup);
        
        // Notify attributes and selector
        return Observable.just(
                Event.of(Event.MARKUP_PROPERTIES, Event.PROPERTIES_ATTRIBUTES, SET_ATTRIBUTES, markup),
                Event.of(Event.MARKUP_PROPERTIES, Event.PROPERTIES_SELECTOR, NEW_SELECTION, markup)
        );
    }
    
    private Observable<Event> restoreMarkups(List<Markup> markups) {
        LOG.info("Restoring Markups for :" + markups);
        
        return Observable.just(Event.of(Event.MARKUP_PROPERTIES, Event.PROPERTIES_SELECTOR, RESTORE_SELECTIONS, 
                markups
                    .stream()
                    .map(Markup::getId)
                    .collect(Collectors.toList()))
        );
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jInternalFrame1 = new javax.swing.JInternalFrame();
        propertySelector = new edu.bloomu.codeglosser.View.PropertySelector();
        propertyAttributes = new edu.bloomu.codeglosser.View.PropertyAttributes();
        tabbedTreeView = new javax.swing.JTabbedPane();
        propertyFiles = new edu.bloomu.codeglosser.View.PropertyFiles();
        propertyTemplates = new edu.bloomu.codeglosser.View.PropertyTemplates();

        jInternalFrame1.setVisible(true);

        tabbedTreeView.addTab("Files", propertyFiles); // NOI18N
        tabbedTreeView.addTab("Templates", propertyTemplates); // NOI18N

        javax.swing.GroupLayout jInternalFrame1Layout = new javax.swing.GroupLayout(jInternalFrame1.getContentPane());
        jInternalFrame1.getContentPane().setLayout(jInternalFrame1Layout);
        jInternalFrame1Layout.setHorizontalGroup(
            jInternalFrame1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jInternalFrame1Layout.createSequentialGroup()
                .addComponent(propertySelector, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
            .addComponent(propertyAttributes, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jInternalFrame1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tabbedTreeView, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addContainerGap())
        );
        jInternalFrame1Layout.setVerticalGroup(
            jInternalFrame1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jInternalFrame1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(propertySelector, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(propertyAttributes, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(tabbedTreeView, javax.swing.GroupLayout.DEFAULT_SIZE, 383, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jInternalFrame1, javax.swing.GroupLayout.Alignment.TRAILING)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jInternalFrame1)
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JInternalFrame jInternalFrame1;
    private edu.bloomu.codeglosser.View.PropertyAttributes propertyAttributes;
    private edu.bloomu.codeglosser.View.PropertyFiles propertyFiles;
    private edu.bloomu.codeglosser.View.PropertySelector propertySelector;
    private edu.bloomu.codeglosser.View.PropertyTemplates propertyTemplates;
    private javax.swing.JTabbedPane tabbedTreeView;
    // End of variables declaration//GEN-END:variables
}
