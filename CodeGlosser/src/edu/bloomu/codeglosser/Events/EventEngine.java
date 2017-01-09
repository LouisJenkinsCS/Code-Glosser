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

import io.reactivex.subjects.PublishSubject;
import java.util.logging.Logger;

/**
 *
 * @author Louis Jenkins
 */
public class EventEngine {
    
    private final int id;
    private final EventHandler handler;
    
    private static final Logger LOG = Logger.getLogger(EventEngine.class.getName());
    
    
    PublishSubject<Event> outgoingEvent = PublishSubject.create();
    PublishSubject<Event> ingoingEvent = PublishSubject.create();
    
    public EventEngine(EventHandler handler, int id) {
        this.handler = handler;
        this.id = id;
        
        // Handle receiving events
        ingoingEvent
                // Only accept events if they are addressed to use
                .filter(e -> e.getRecipient() == id)
                .doOnNext(e -> LOG.info("Received event: " + e))
                // Convert it to the implementor's Observable
                .flatMap(handler::handleEvent)
                // If it emits any events, send them as outgoing.
                .subscribe(outgoingEvent::onNext);
    }
    
    /**
     * Broadcasts the event to all subscribers. This is generally used begin propagation
     * of events.
     * @param to Recipient
     * @param custom Custom
     * @param data Data
     */
    public void broadcast(int to, int custom, Object data) {
        outgoingEvent.onNext(Event.of(id, to, custom, data));
    }
    
    /**
     * Registers another engine with our own so that both receive and send to each other.
     * @param engine Engine to register.
     */
    public void register(EventEngine engine) {
        engine.outgoingEvent.subscribe(this.ingoingEvent::onNext);
        this.outgoingEvent.subscribe(engine.ingoingEvent::onNext);
    } 
    
    
}
