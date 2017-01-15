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
package edu.bloomu.codeglosser;

import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;

/**
 *
 * @author Louis Jenkins
 */
public final class Globals {
    
    // Scheduler for our background worker thread. The worker thread handles all
    // IO and CPU Bound processing.
    public static final Scheduler WORKER_THREAD = Schedulers.from(Executors.newSingleThreadScheduledExecutor());
    
    // The project folder selected by the user.
    public static Path PROJECT_FOLDER = null;
    
    // The URI used for obtaining the relative path to the file. This URI is used
    // for generating the file tree as well as saving session data.
    public static URI URI_PREFIX = null;
    
    // The location of the templates file
    public static Path TEMPLATE_FILE = Paths.get("templates.json");
    
    // The path to current file
    public static Path CURRENT_FILE = null;
    
    /**
     * Initializes all globals to a default state. Used so that in the case of a
     * memory leak, the previous values are not accidentally used.
     */
    public static void initGlobals() {
        PROJECT_FOLDER = null;
        URI_PREFIX = null;
        CURRENT_FILE = null;
    }
}
