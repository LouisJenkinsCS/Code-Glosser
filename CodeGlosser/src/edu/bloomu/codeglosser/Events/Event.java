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

import io.reactivex.Observable;
import java.util.function.Function;

/**
 *
 * @author Louis
 * 
 * Events that each component emits to notify each other of an action being performed.
 */
public class Event {
    
    // The possible components in this event-driven system must be one of these.
    // The strings themselves are used as descriptors which help with event sourcing
    // and general logging.
    public static final String MARKUP_VIEW = "MarkupView";
    public static final String MARKUP_CONTROLLER = "MarkupController";
    public static final String MARKUP_PROPERTIES = "MarkupProperties";
    public static final String PROPERTY_SELECTOR = "PropertySelector";
    public static final String PROPERTY_TEMPLATES = "PropertyTemplates";
    public static final String PROPERTY_FILES = "PropertyFiles";
    public static final String PROPERTY_ATTRIBUTES = "PropertyAttributes"; 
    
    
    // Fields to determine WHO sent it, WHO this is for, and WHAT this event is.
    public final String sender;
    public final String recipient;
    public final String descriptor;
    
    // The data that is associated with this event. The actual type and how it is
    // used is up to the sender and intended recipient to determine. 
    public final Object data;
    
    /**
     * Create an instance of an Event. This is used over the constructor in case
     * we make some further changed and/or optimizations.
     * @param from Who are we?
     * @param to Who should see this?
     * @param what What kind of event is this?
     * @param data What is the event data?
     * @return Event encoded.
     */
    public static Event of(String from, String to, String what, Object data) {
        return new Event(from, to, what, data);
    }
    
    /**
     * Ignores the passed parameter in favor of returning an empty Observable. This
     * is needed for when handling events that do not need propagation without needing
     * to interrupt the control flow with explicit checks.
     * @param ignored
     * @return 
     */
    public static Observable<Event> empty(Object ignored) {
        return Observable.empty();
    }
    
    private Event(String sender, String recipient, String descriptor, Object data) {
        this.sender = sender;
        this.recipient = recipient;
        this.descriptor = descriptor;
        this.data = data;
    }

    @Override
    public String toString() {
        return "Event: {Sender: " + sender + ", Recipient: " + recipient + ", Descriptor: \"" + descriptor + "\"}";
    }
    
    public String toString(Function<Event, String> asString) {
        return "Event: {Sender: " + sender + ", Recipient: " + recipient + ", Descriptor: \"" + descriptor + "\"}";
    }
}
