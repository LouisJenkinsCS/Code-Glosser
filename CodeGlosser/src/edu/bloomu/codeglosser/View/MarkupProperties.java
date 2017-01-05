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

import edu.bloomu.codeglosser.Events.Event;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import java.nio.file.Path;
import java.util.logging.Logger;

/**
 *
 * @author Louis Jenkins
 * 
 * The parent of all properties, which manages PropertiesFile, PropertiesTemplate,
 * PropertiesAttributes, and PropertiesSelector. Due to technical difficulties
 * with NetBeans, we reserve space for all attributes and add them manually. 
 */
public class MarkupProperties extends javax.swing.JPanel {
    
    public static final int FILE_SELECTED = 1 << 0;
    
    public static final int CLEAR_SELECTION = 1 << 0;

    public static final int CLEAR_ATTRIBUTES = 1 << 0;
    
    private static final Logger LOG = Logger.getLogger(MarkupProperties.class.getName());
    
    // Event Multiplexer
    private final PublishSubject<Event> event = PublishSubject.create();

    public MarkupProperties() {
        // Initialize components
        initComponents();
        initChildren();
        
        // Handle receiving events
        event
                .filter(this::eventForUs)
                .flatMap(e -> {
                    switch (e.getSender()) {
                        case Event.MARKUP_CONTROLLER:
                            switch (e.getCustom()) {
                                default:
                                    throw new RuntimeException("Bad Custom Tag!");
                            }
                        case Event.PROPERTIES_FILES:
                            switch (e.getCustom()) {
                                case PropertyFiles.FILE_SELECTED:
                                    return fileSelected((Path) e.data);
                            }
                        default:
                            throw new RuntimeException("Bad Sender!");
                    }
                })
                .subscribe(event::onNext);
                
        
    }
    
    private void initChildren() {
        propertyFiles.addEventSource(event);
        propertyFiles.getEventSource().subscribe(event::onNext);
    }
    
    /**
     * Predicate to determine if the event sent was meant for us.
     *
     * @param e Event
     * @return If meant for us
     */
    private boolean eventForUs(Event e) {
        return (e.getRecipient() == Event.MARKUP_PROPERTIES);
    }
    
    /**
     * Registers the following observable as an event source. This must be called
     * to receive events from other components.
     * @param source 
     */
    public void addEventSource(Observable<Event> source) {
        source.subscribe(event::onNext);
    }
    
    /**
     * Returns our own Subject as an event source for listeners. This must be used
     * to receive events from this component.
     * @return Our event source
     */
    public Observable<Event> getEventSource() {
        return event;
    }
    
    private void sendEventToFiles(int eventTag, Object data) {
        event.onNext(Event.of(Event.MARKUP_PROPERTIES, Event.PROPERTIES_FILES, eventTag, data));
    }
    
    private void sendEventToTemplates(int eventTag, Object data) {
        event.onNext(Event.of(Event.MARKUP_PROPERTIES, Event.PROPERTIES_TEMPLATES, eventTag, data));
    }
    
    private void sendEventToAttributes(int eventTag, Object data) {
        event.onNext(Event.of(Event.MARKUP_PROPERTIES, Event.PROPERTIES_ATTRIBUTES, eventTag, data));
    }
    
    private void sendEventToSelector(int eventTag, Object data) {
        event.onNext(Event.of(Event.MARKUP_PROPERTIES, Event.PROPERTIES_SELECTOR, eventTag, data));
    }
    
    private void sendEventToController(int eventTag, Object data) {
        event.onNext(Event.of(Event.MARKUP_PROPERTIES, Event.MARKUP_CONTROLLER, eventTag, data));
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
                Event.of(Event.MARKUP_PROPERTIES, Event.PROPERTIES_SELECTOR, CLEAR_SELECTION, null),
                Event.of(Event.MARKUP_PROPERTIES, Event.PROPERTIES_ATTRIBUTES, CLEAR_ATTRIBUTES, null),
                Event.of(Event.MARKUP_PROPERTIES, Event.MARKUP_CONTROLLER, FILE_SELECTED, filePath)
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
        propertySelector = new edu.bloomu.codeglosser.View.propertyNoteName();
        propertyAttributes = new edu.bloomu.codeglosser.View.PropertyAttributes();
        tabbedTreeView = new javax.swing.JTabbedPane();
        propertyFiles = new edu.bloomu.codeglosser.View.PropertyFiles();
        propertyTemplates = new edu.bloomu.codeglosser.View.PropertyTreeView();

        jInternalFrame1.setVisible(true);

        tabbedTreeView.addTab(org.openide.util.NbBundle.getMessage(MarkupProperties.class, "MarkupProperties.propertyFiles.TabConstraints.tabTitle"), propertyFiles); // NOI18N
        tabbedTreeView.addTab(org.openide.util.NbBundle.getMessage(MarkupProperties.class, "MarkupProperties.propertyTemplates.TabConstraints.tabTitle"), propertyTemplates); // NOI18N

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
    private edu.bloomu.codeglosser.View.propertyNoteName propertySelector;
    private edu.bloomu.codeglosser.View.PropertyTreeView propertyTemplates;
    private javax.swing.JTabbedPane tabbedTreeView;
    // End of variables declaration//GEN-END:variables
}
