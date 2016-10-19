/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser.View;

import edu.bloomu.codeglosser.Model.Note;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

/**
 *
 * @author Louis
 */
public class propertyNoteName extends javax.swing.JPanel implements ObservableProperty<Note> {
    
    private PublishSubject<Note> onNoteSelection = PublishSubject.create();
    private static final Logger LOG = Logger.getLogger(propertyNoteName.class.getName());
    
    // Kept track of to prevent invoking 'setSelectedItem' and triggering the ActionListener (which would cause an infinite loop)
    private Note currentNote;
    
    
    /**
     * Creates new form propertyNoteName
     */
    public propertyNoteName() {
        initComponents();
        clear();
        noteSelector.addActionListener(e -> {
            JComboBox<Note> src = (JComboBox<Note>) e.getSource();
            Note n = (Note) src.getSelectedItem();
            if (n != null) {
                currentNote = n;
                onNoteSelection.onNext(n);
            }
        });
    }
    
    public void update(Note... notes) {
        Note n = currentNote;
        clear();
        Stream.of(notes).forEach(noteSelector::addItem);
        if (!Stream.of(notes).anyMatch(n::equals)) {
            noteSelector.setSelectedItem(Note.DEFAULT);
        }
    }
    
    public void clear() {
        noteSelector.removeAllItems();
        currentNote = Note.DEFAULT;
        noteSelector.addItem(currentNote);
    }
    
    public void setSelectedNote(Note n) {
        if (n != currentNote)
            noteSelector.setSelectedItem(n);
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

    @Override
    public Observable<Note> observe() {
        return onNoteSelection;
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<Note> noteSelector;
    // End of variables declaration//GEN-END:variables
}
