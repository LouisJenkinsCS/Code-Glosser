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
import edu.bloomu.codeglosser.Model.Markup;
import edu.bloomu.codeglosser.Utils.SwingScheduler;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import java.awt.Color;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.swing.JColorChooser;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 *
 * @author Louis Jenkins
 */
public class PropertyAttributes extends javax.swing.JPanel {

    private static final Logger LOG = Logger.getLogger(PropertyAttributes.class.getName());
    
    public static final int COLOR_CHANGE = 0x1;
    public static final int TEXT_CHANGE = 0x2;
    
    // Determines how many milliseconds of time must pass between the last user input
    // before automatically saving it. This is very useful in both performance and user
    // experience.
    private static final long TEXT_CHANGE_DEBOUNCE = 500;
    
    private Color color = Color.YELLOW;
    private String message = "";
    
    // Event Multiplexer
    private final PublishSubject<Event> event = PublishSubject.create();

    public PropertyAttributes() {
        // Initialize GUI components
        initComponents();
        initListeners();
        setColor(Color.YELLOW);
        
        // Handle receiving events
        event
                .filter(this::eventForUs)
                .doOnNext(ignored -> LOG.info("Processing Event..."))
                .flatMap(e -> {
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
                })
                .subscribe(event::onNext);
    }
    
    private void initListeners() {
        PublishSubject<String> textChange = PublishSubject.create();
        
        // Handle any text change events
        textChange
                // We throttle text change events from the user to relieve backpressure.
                // As well, all internal work is kept off the UI Thread and on a CPU-Bound one.
                .debounce(TEXT_CHANGE_DEBOUNCE, TimeUnit.MILLISECONDS, Schedulers.computation())
                .doOnNext(ignored -> LOG.info("Processing Text Change event"))
                // We only proceed if the message != text, because 'setText' can trigger this
                .filter(text -> !text.equals(message))
                .map(text -> Event.of(Event.PROPERTIES_ATTRIBUTES, Event.MARKUP_PROPERTIES, TEXT_CHANGE, Markup.template(text)))
                // Event handling and broadcasting are done on the UI Thread for simplicity
                .observeOn(SwingScheduler.getInstance())
                .subscribe(event::onNext);
        
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
        textLabel = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        noteMsg = new javax.swing.JTextArea();

        labelColor.setBackground(new java.awt.Color(255, 255, 102));
        org.openide.awt.Mnemonics.setLocalizedText(labelColor, org.openide.util.NbBundle.getMessage(PropertyAttributes.class, "PropertyAttributes.labelColor.text")); // NOI18N
        labelColor.setOpaque(true);
        labelColor.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                labelColorMousePressed(evt);
            }
        });

        labelName.setFont(new java.awt.Font("Tahoma", 1, 13)); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(labelName, org.openide.util.NbBundle.getMessage(PropertyAttributes.class, "PropertyAttributes.labelName.text")); // NOI18N
        labelName.setToolTipText(org.openide.util.NbBundle.getMessage(PropertyAttributes.class, "PropertyAttributes.labelName.toolTipText")); // NOI18N

        labelRGB.setFont(new java.awt.Font("Tahoma", 1, 13)); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(labelRGB, org.openide.util.NbBundle.getMessage(PropertyAttributes.class, "PropertyAttributes.labelRGB.text")); // NOI18N

        textLabel.setFont(new java.awt.Font("Tahoma", 1, 13)); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(textLabel, org.openide.util.NbBundle.getMessage(PropertyAttributes.class, "PropertyAttributes.textLabel.text")); // NOI18N

        noteMsg.setColumns(20);
        noteMsg.setLineWrap(true);
        noteMsg.setRows(5);
        noteMsg.setText(org.openide.util.NbBundle.getMessage(PropertyAttributes.class, "PropertyAttributes.noteMsg.text")); // NOI18N
        noteMsg.setToolTipText(org.openide.util.NbBundle.getMessage(PropertyAttributes.class, "PropertyAttributes.noteMsg.toolTipText")); // NOI18N
        noteMsg.setWrapStyleWord(true);
        jScrollPane1.setViewportView(noteMsg);

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
                        .addComponent(textLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane1)))
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
                        .addComponent(labelColor, javax.swing.GroupLayout.DEFAULT_SIZE, 19, Short.MAX_VALUE)
                        .addGap(18, 18, 18)))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(textLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 96, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jScrollPane1))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void labelColorMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_labelColorMousePressed
        Color c = JColorChooser.showDialog(null, "Highlighter Color...", color);
        if (c != null) {
            setColor(c);
            sendEventToParent(COLOR_CHANGE, c);
        }
    }//GEN-LAST:event_labelColorMousePressed
    
    private Observable<Event> setAttributes(Markup markup) {
        LOG.info("Setting attributes: " + markup);
        setColor(markup.getHighlightColor());
        setMessage(markup.getMsg());
        
        return Observable.empty();
    }
    
    private Observable<Event> clearAttributes() {
        LOG.info("Clearing attributes...");
        
        setColor(Color.YELLOW);
        setMessage("");
        
        return Observable.empty();
    }
    
    /**
     * Registers the following observable as an event source. This must be called
     * to receive events from other components.
     * @param source 
     */
    public void addEventSource(Observable<Event> source) {
        source.filter(this::eventForUs).subscribe(event::onNext);
    }
    
    /**
     * Returns our own Subject as an event source for listeners. This must be used
     * to receive events from this component.
     * @return Our event source
     */
    public Observable<Event> getEventSource() {
        return event;
    }
    
    public Color getColor() {
        return labelColor.getBackground();
    }
    
    public void setColor(Color c) {
       color = c;
       labelColor.setBackground(c);
       labelColor.setForeground(c);
       labelRGB.setText("(" + c.getRed() + "," + c.getGreen() + "," + c.getBlue() + ")");
    }
    
    public void setMessage(String msg) {
        message = msg;
        noteMsg.setText(msg);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel labelColor;
    private javax.swing.JLabel labelName;
    private javax.swing.JLabel labelRGB;
    private javax.swing.JTextArea noteMsg;
    private javax.swing.JLabel textLabel;
    // End of variables declaration//GEN-END:variables

    private void sendEventToParent(int eventTag, Object data) {
        event.onNext(Event.of(Event.PROPERTIES_ATTRIBUTES, Event.MARKUP_PROPERTIES, eventTag, data));
    }
    
    private boolean eventForUs(Event e) {
        return e.getSender() !=  Event.PROPERTIES_ATTRIBUTES && e.getRecipient() == Event.PROPERTIES_ATTRIBUTES;
    }
}
