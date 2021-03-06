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
package edu.bloomu.codeglosser.Events;

import edu.bloomu.codeglosser.Globals;
import io.reactivex.subjects.PublishSubject;
import java.util.Arrays;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.JOptionPane;

/**
 *
 * @author Louis Jenkins
 */
public class EventBus {
    
    private static final Logger LOG = Globals.LOGGER;
    
    
    private final String id;
    
    private Function<Event, String> stringifyEvent;
    
    
    private final PublishSubject<Event> outgoingEvent = PublishSubject.create();
    private final PublishSubject<Event> ingoingEvent = PublishSubject.create();
    
    public EventBus(EventProcessor processor, String id) {
        this.id = id;
        
        // Handle receiving events
        ingoingEvent
                // Only accept events if they are addressed to use
                .filter(e -> e.recipient.equals(id))
                // Log any and all events
                .doOnNext(e -> LOG.info(e.toString()))
                // Defer processing. The processor is free to go between schedulers
                // all they like, and may return zero or more Events.
                .flatMap(processor::process)
                // All events received are processed by the background worker thread.
                .subscribeOn(Globals.WORKER_THREAD)
                // If it emits any events, send them as outgoing. Any errors will cause
                // a runtime exception and termination! In the future, there will be 
                // a way to recover from errors that are recoverable, but that will be
                // implemented based on need.
                .subscribe(outgoingEvent::onNext, this::onError);
    }
    
    private void onError(Throwable ex) {
        // Dump stack frame
        LOG.log(Level.SEVERE, "Error while processing event: {0}", ex.getMessage());
        LOG.severe(Stream
                .of(ex.getStackTrace())
                .map(Object::toString)
                .collect(Collectors.joining("\n")));
        
        // User informed of critical error before exiting.
        JOptionPane.showMessageDialog(null, "Error while processing an event!!!\n"
                + "A stacktrace has been written to the log file, 'log.txt'.\n"
                + "Please Submit this to the developer: LouisJenkinsCS@hotmail.com\n"
                + "Error Message: " + ex.getMessage(), "Critical Error", JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }
    
    public void setStringification(Function<Event, String> toString) {
        this.stringifyEvent = toString;
    }
    
    /**
     * Broadcasts the event to all subscribers. This is generally used begin propagation
     * of events.
     * @param to Recipient
     * @param custom Custom
     * @param data Data
     */
    public void broadcast(String to, String descr, Object data) {
        outgoingEvent.onNext(Event.of(id, to, descr, data));
    }
    
    /**
     * Registers another engine with our own so that both receive and send to each other.
     * @param engine Engine to register.
     */
    public void register(EventBus engine) {
        engine.outgoingEvent.subscribe(this.ingoingEvent::onNext);
        this.outgoingEvent.subscribe(engine.ingoingEvent::onNext);
    } 
    
    
}
