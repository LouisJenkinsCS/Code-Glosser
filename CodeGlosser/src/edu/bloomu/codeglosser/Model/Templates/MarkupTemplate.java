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
package edu.bloomu.codeglosser.Model.Templates;

import edu.bloomu.codeglosser.Session.SessionObject;
import edu.bloomu.codeglosser.Utils.ColorUtils;
import java.awt.Color;
import org.apache.logging.log4j.util.Strings;
import org.json.simple.JSONObject;

/**
 *
 * @author Louis
 */
public class MarkupTemplate implements SessionObject {
    
    public static final String KEY_PREFIX = "prefix";
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_TITLE = "title";
    public static final String KEY_COLOR = "color";
    
    private String prefix;
    private String message;
    private String title;
    private Color color;
    
    private boolean initialized;
    
    public MarkupTemplate() {
        this.prefix = this.message = this.title = Strings.EMPTY;
        this.color = Color.YELLOW;
        this.initialized = false;
    }

    @Override
    public String getId() {
        return this.title;
    }

    @Override
    public JSONObject serialize() {
        if (!this.initialized) {
            throw new IllegalStateException("Attempt to serialize a non-initalized template!");
        }
        
        JSONObject obj = new JSONObject();
        obj.put(KEY_PREFIX, prefix);
        obj.put(KEY_TITLE, title);
        obj.put(KEY_MESSAGE, message);
        obj.put(KEY_COLOR, ColorUtils.asString(color));
        
        return obj;
    }

    @Override
    public void deserialize(JSONObject obj) {
        this.prefix = (String) obj.get(KEY_PREFIX);
        this.title = (String) obj.get(KEY_TITLE);
        this.message = (String) obj.get(KEY_MESSAGE);
        if (obj.containsKey(KEY_COLOR)) {
            this.color = Color.decode((String) obj.get(KEY_COLOR));
        } else { 
            this.color = Color.YELLOW;
        }
        
        this.initialized = true;
    }
    
    public String getPrefix() {
        return prefix;
    }

    public String getMessage() {
        return message;
    }

    public String getTitle() {
        return title;
    }

    public Color getColor() {
        return color;
    }

    @Override
    public String toString() {
        return title;
    }
    
    
}
