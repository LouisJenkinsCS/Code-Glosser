/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser.Utils;

/**
 *
 * @author Louis
 */
public class Bounds implements Comparable<Bounds> {
    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(Bounds.class.getName());
    private int start;
    private int end;
    
    public static Bounds of(int start, int end) {
        return new Bounds(start, end);
    }
    
    public Bounds(int start, int end) {
        this.start = start;
        this.end = end;
    }
    
    public boolean contains(Bounds other) {
        return other.start >= this.start && other.end <= this.end;
    }
    
    public boolean collidesWith(Bounds other) {
        return (other.start <= this.end && other.end >= this.start)
                || (this.start <= other.end && this.end >= other.start);
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    @Override
    public int compareTo(Bounds other) {
        return Integer.compare(this.start, other.start);
    }

    @Override
    public String toString() {
        return "Bounds{" + "start=" + start + ", end=" + end + '}';
    }
    
    
}
