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
import edu.bloomu.codeglosser.Utils.SwingScheduler;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import java.awt.Color;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.swing.JColorChooser;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import edu.bloomu.codeglosser.Events.EventProcessor;
import edu.bloomu.codeglosser.Globals;

/**
 *
 * @author Louis Jenkins
 */
public class PropertyAttributes extends javax.swing.JPanel implements EventProcessor {

    private static final Logger LOG = Globals.LOGGER;
    
    public static final int COLOR_CHANGE = 0x1;
    public static final int TEXT_CHANGE = 0x2;
    
    // Determines how many milliseconds of time must pass between the last user input
    // before automatically saving it. This is very useful in both performance and user
    // experience.
    private static final long TEXT_CHANGE_DEBOUNCE = 500;
    
    private Color color = Color.YELLOW;
    private String message = "";
    
    private final EventBus engine = new EventBus(this, Event.PROPERTY_ATTRIBUTES);

    public PropertyAttributes() {
        // Initialize GUI components
        initComponents();
        noteMsg.setLineWrap(true);
        noteMsg.setWrapStyleWord(true);
        labelName.setText("Color");
        textLabel.setText("Message");
        initListeners();
        setColor(Color.YELLOW);     
    }
    
    @Override
    public Observable<Event> process(Event e) {
        switch (e.getSender()) {
            case Event.MARKUP_PROPERTIES:
                switch (e.getCustom()) {
                    case MarkupProperties.SET_ATTRIBUTES:
                        return setAttributes((Markup) e.data);
                    case MarkupProperties.CLEAR_ATTRIBUTES:
                        return clearAttributes();
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
    
    private void initListeners() {
        PublishSubject<String> textChange = PublishSubject.create();
        
        // Handle any text change events
        textChange
                // We throttle text change events from the user to relieve backpressure.
                // As well, all internal work is kept off the UI Thread and on a CPU-Bound one.
                .debounce(TEXT_CHANGE_DEBOUNCE, TimeUnit.MILLISECONDS, Schedulers.computation())
                // We only proceed if the message != text, because 'setText' can trigger this
                .filter(text -> !text.equals(message))
                .subscribe(text -> engine.broadcast(Event.MARKUP_PROPERTIES, TEXT_CHANGE, text));
                
                
        
        noteMsg.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                textChange.onNext(noteMsg.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                textChange.onNext(noteMsg.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                textChange.onNext(noteMsg.getText());
            }
            
        });
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        labelColor = new javax.swing.JLabel();
        labelName = new javax.swing.JLabel();
        labelRGB = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        noteMsg = new javax.swing.JTextArea();
        textLabel = new javax.swing.JLabel();

        labelColor.setBackground(new java.awt.Color(255, 255, 102));
        labelColor.setOpaque(true);
        labelColor.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                labelColorMousePressed(evt);
            }
        });

        labelName.setFont(new java.awt.Font("Tahoma", 1, 13)); // NOI18N

        labelRGB.setFont(new java.awt.Font("Tahoma", 1, 13)); // NOI18N

        noteMsg.setColumns(20);
        noteMsg.setRows(5);
        jScrollPane1.setViewportView(noteMsg);

        textLabel.setFont(new java.awt.Font("sansserif", 1, 12)); // NOI18N
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("edu/bloomu/codeglosser/View/Bundle"); // NOI18N
        textLabel.setToolTipText(bundle.getString("PropertyAttributes.textLabel.toolTipText")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(labelName, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(labelRGB, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(labelColor, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(textLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 60, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 244, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(labelName, javax.swing.GroupLayout.DEFAULT_SIZE, 50, Short.MAX_VALUE)
                            .addComponent(labelRGB, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(19, 19, 19)
                        .addComponent(labelColor, javax.swing.GroupLayout.DEFAULT_SIZE, 37, Short.MAX_VALUE)))
                .addGap(0, 0, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(textLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void labelColorMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_labelColorMousePressed
        Color c = JColorChooser.showDialog(null, "Highlighter Color...", color);
        if (c != null) {
            setColor(c);
            engine.broadcast(Event.MARKUP_PROPERTIES, COLOR_CHANGE, c);
        }
    }//GEN-LAST:event_labelColorMousePressed
    
    private Observable<Event> setAttributes(Markup markup) {
        LOG.fine("Setting attributes: " + markup);
        
        setColor(markup.getHighlightColor());
        setMessage(markup.getMsg());
        
        return Observable.empty();
    }
    
    private Observable<Event> clearAttributes() {
        LOG.fine("Clearing attributes...");
        
        setColor(Color.YELLOW);
        setMessage("");
        
        return Observable.empty();
    }
    
    public Color getColor() {
        return labelColor.getBackground();
    }
    
    public void setColor(Color c) {
        if (c != null) {
            color = c;
            
            SwingUtilities.invokeLater(() -> {
                labelColor.setBackground(c);
                labelColor.setForeground(c);
                labelRGB.setText("(" + c.getRed() + "," + c.getGreen() + "," + c.getBlue() + ")");
            });
        }
    }
    
    public void setMessage(String msg) {
        if (msg != null && !msg.equals(message)) {
            message = msg;
            SwingUtilities.invokeLater(() -> noteMsg.setText(msg));
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel labelColor;
    private javax.swing.JLabel labelName;
    private javax.swing.JLabel labelRGB;
    private javax.swing.JTextArea noteMsg;
    private javax.swing.JLabel textLabel;
    // End of variables declaration//GEN-END:variables
}
