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
import edu.bloomu.codeglosser.Events.EventBus;
import edu.bloomu.codeglosser.Model.Markup;
import io.reactivex.Observable;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.JComboBox;
import javax.swing.SwingUtilities;
import edu.bloomu.codeglosser.Events.EventProcessor;
import edu.bloomu.codeglosser.Globals;

/**
 *
 * @author Louis
 */
public class PropertySelector extends javax.swing.JPanel implements EventProcessor {
    
    // MarkupProperties
    public static final int SELECTED_ID = 0x1;
    
    private static final Logger LOG = Globals.LOGGER;
    
    private final EventBus engine = new EventBus(this, Event.PROPERTY_SELECTOR);
    
    public PropertySelector() {
        initComponents();
        initListeners();        
    }
    
    private void initListeners() {
        // Setup listener for id selection
        noteSelector.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                JComboBox<String> src = (JComboBox<String>) e.getSource();
                String id = (String) src.getSelectedItem();
                engine.broadcast(Event.MARKUP_PROPERTIES, SELECTED_ID, id);
            }
        });
    }
    
    @Override
    public Observable<Event> process(Event e) {
        switch (e.getSender()) {
            case Event.MARKUP_PROPERTIES:
                switch (e.getCustom()) {
                    case MarkupProperties.NEW_SELECTION:
                        return newSelection((Markup) e.data);
                    case MarkupProperties.CLEAR_SELECTION:
                        return clearSelection();
                    case MarkupProperties.SET_SELECTION:
                        return setSelection((String) e.data);
                    case MarkupProperties.REMOVE_SELECTION:
                        return removeSelection();
                    case MarkupProperties.RESTORE_SELECTIONS:
                        return restoreSelections((List<String>) e.data);
                    default:
                        throw new RuntimeException("Bad Custom Tag from MarkupProperties!");
                }
            default:
                throw new RuntimeException("Bad Sender!");
        }
    }

    @Override
    public EventBus getEventEngine() {
        return engine;
    }
    
    private Observable<Event> setSelection(String id) {
        SwingUtilities.invokeLater(() -> {
            if (!noteSelector.getSelectedItem().equals(id)) {
                noteSelector.setSelectedItem(id);
            }
        });
        
        return Observable.empty();
    }
    
    public void setSelectedNote(String id) {
        SwingUtilities.invokeLater(() -> noteSelector.setSelectedItem(id));
    }

    private Observable<Event> clearSelection() {
        LOG.fine("Handling event for clearing markup selection...");
        
        SwingUtilities.invokeLater(() -> {
            noteSelector.removeAllItems();
            noteSelector.addItem(Markup.DEFAULT.getId());
        });
        
        return Observable.empty();
    }
    
    private Observable<Event> newSelection(Markup markup) {
        LOG.fine("Handling event for creating a new markup selection: " + markup);
        
        return Observable
                .just(markup)
                .map(Markup::getId)
                // We need to collect all identifiers into a list so we can sort them
                // with the new id. This is necessary because there currently is no
                // JComboBox Model that does this for us.
                .flatMap(id -> {
                    ArrayList<String> idList = new ArrayList<>();
                    
                    // Obtain all identifiers (including one to add)
                    for (int i = 0; i < noteSelector.getItemCount(); i++) {
                        idList.add(noteSelector.getItemAt(i));
                    }
                    idList.add(id);
                    
                    // Now it becomes the new Observable
                    return Observable.fromIterable(idList);
                })
                // Sort in descending order
                .sorted((id1, id2) -> id2.compareTo(id1))
                // Remove all items first and then add them again as they are out of order
                .doOnNext(id -> SwingUtilities.invokeLater(() -> {
                        noteSelector.removeItem(id);
                        noteSelector.addItem(id);
                    }
                ))
                .flatMap(Event::empty);
    }
    
    private Observable<Event> restoreSelections(List<String> selections) {
        LOG.fine("Restoring Selections: " + selections);
        
        selections
                .stream()
                .sorted()
                .forEach(id -> SwingUtilities.invokeLater(() -> noteSelector.addItem(id)));
        
        return Observable.empty();
    }
    
    private Observable<Event> removeSelection() {
        SwingUtilities.invokeLater(() -> {
                noteSelector.removeItem(noteSelector.getSelectedItem());
                noteSelector.setSelectedItem(Markup.DEFAULT);
            }
        );
        
        return Observable.empty();
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        noteSelector = new javax.swing.JComboBox<>();

        noteSelector.setFont(new java.awt.Font("Times New Roman", 1, 18)); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(noteSelector, javax.swing.GroupLayout.Alignment.TRAILING, 0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(noteSelector)
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<java.lang.String> noteSelector;
    // End of variables declaration//GEN-END:variables
}
