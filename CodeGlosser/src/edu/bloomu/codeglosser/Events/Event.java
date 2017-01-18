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
    
    /*
        The identifying bits
    */
    public static final int MARKUP_VIEW = 0x01;
    public static final int MARKUP_CONTROLLER = 0x02;
    public static final int MARKUP_PROPERTIES = 0x03;
    public static final int PROPERTY_SELECTOR = 0x04;
    public static final int PROPERTY_TEMPLATES = 0x05;
    public static final int PROPERTY_FILES = 0x06;
    public static final int PROPERTY_ATTRIBUTES = 0x07;
    
    private static String tagToString(int tag) {
        switch (tag) {
            case MARKUP_VIEW:
                return "MarkupView";
            case MARKUP_CONTROLLER:
                return "MarkupController";
            case MARKUP_PROPERTIES:
                return "MarkupProperties";
            case PROPERTY_SELECTOR:
                return "PropertySelector";
            case PROPERTY_TEMPLATES:
                return "PropertyTemplates";
            case PROPERTY_FILES:
                return "PropertyFiles";
            case PROPERTY_ATTRIBUTES:
                return "PropertyAttributes";
            default:
                return "Unknown(" + tag + ")";
        }
    }   
    
    /*
        Helper bit shifts and masks to determine what is what.
    */
    public static final int SENDER_MASK = 0xFF;
    
    public static final int RECIPIENT_SHIFT = 8;
    public static final int RECIPIENT_MASK = 0xFF00;
    
    public static final int CUSTOM_SHIFT = 16;
    public static final int CUSTOM_MASK = 0xFFFF0000;
    
    
    
    // Special flags; The lowest 8 bits are used to represent WHO the sender is,
    // the next 8 bits are used to represent WHO the intended recipients are.
    // The next 16 bits can be used for other purposes.
    public final int tag;
    
    // The data that is associated with this event. It is an opaque reference, and
    // it is up to the sender and recipient to determine what type it is or how it
    // should be represented.
    public final Object data;
    
    /**
     * A static helper method for encoding events to be passed around.
     * @param from Who are we?
     * @param to Who should see this?
     * @param customTag What event tag is being sent?
     * @param data What is the event data?
     * @return Event encoded.
     */
    public static Event of(int from, int to, int customTag, Object data) {
        return new Event(from | (to << RECIPIENT_SHIFT) | (customTag << CUSTOM_SHIFT), data);
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
    
    private Event(int tag, Object data) {
        this.tag = tag;
        this.data = data;
    }
    
    /**
     * Get the set sender bits only.
     * @return Sender.
     */
    public int getSender() {
        return this.tag & SENDER_MASK;
    }
    
    /**
     * Get the set recipient bits only. 
     * @return Recipient.
     */
    public int getRecipient() {
        return (this.tag & RECIPIENT_MASK) >> RECIPIENT_SHIFT;
    }
    
    /**
     * Get the set custom bits only.
     * @return Custom.
     */
    public int getCustom() {
        return (this.tag & CUSTOM_MASK) >> CUSTOM_SHIFT;
    }

    @Override
    public String toString() {
        return "Event: {Sender: " + tagToString(getSender()) + ", Recipient: " + tagToString(getRecipient()) + ", Custom: " + getCustom() + "}";
    }
    
    public String toString(Function<Event, String> asString) {
        return "Event: {Sender: " + tagToString(getSender()) + ", Recipient: " + tagToString(getRecipient()) + ", Custom: " + getCustom() + "}";
    }
}
