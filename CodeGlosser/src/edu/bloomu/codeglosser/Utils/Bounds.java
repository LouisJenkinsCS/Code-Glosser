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
package edu.bloomu.codeglosser.Utils;

import org.json.simple.JSONObject;

/**
 *
 * @author Louis
 * 
 * Utility object that keeps track of the boundary of a markup. Each Bounds has both
 * a start and end, and also has the ability to determine if another markup collides with
 * it.
 */
public class Bounds implements Comparable<Bounds> {
    
    private static final String START = "start";
    private static final String END = "end";
    
    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(Bounds.class.getName());
    
    private int start;
    private int end;
    
    public static Bounds of(int start, int end) {
        return new Bounds(start, end);
    }
    
    public static Bounds deserialize(JSONObject obj) {
        int start = (int) (long) obj.get(START);
        int end = (int) (long) obj.get(END);
        
        return Bounds.of(start, end);
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
    
    public JSONObject serialize() {
        JSONObject obj = new JSONObject();
        obj.put(START, start);
        obj.put(END, end);
        
        return obj;
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
