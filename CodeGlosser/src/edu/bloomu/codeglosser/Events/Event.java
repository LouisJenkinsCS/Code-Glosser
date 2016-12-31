package edu.bloomu.codeglosser.Events;

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
    public static final int MARKUP_VIEW = 1 << 0;
    public static final int MARKUP_CONTROLLER = 1 << 1;
    public static final int MARKUP_PROPERTIES = 1 << 2;
    public static final int PROPERTIES_SELECTOR = 1 << 3;
    public static final int PROPERTIES_TEMPLATES = 1 << 4;
    public static final int PROPERTIES_FILES = 1 << 5;
    public static final int PROPERTIES_ATTRIBUTES = 1 << 6;
    
    
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
}
