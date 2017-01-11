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

import io.reactivex.Scheduler;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import javax.swing.Timer;

/**
 *
 * @author Louis Jenkins
 * 
 * An implementation of an Rx Scheduler that ensures that tasks are executed on the
 * Swing UI Thread.
 */
public class SwingScheduler extends Scheduler {

    private static final Logger LOG = Logger.getLogger(SwingScheduler.class.getName());

    private static final SwingScheduler INSTANCE = new SwingScheduler();
    
    public static SwingScheduler getInstance() {
        return INSTANCE;
    }
    
    @Override
    public Worker createWorker() {
        return new Worker() {
            
            private boolean isDisposed = false;
            
            @Override
            public Disposable schedule(Runnable run, long delay, TimeUnit unit) {
                LOG.info("Inside of Scheduler");
                int timeout = Math.min(Math.max(0, (int) unit.toMillis(delay)), Integer.MAX_VALUE);
                isDisposed = false;
                Timer timer = new Timer(timeout, e -> {
                    if (!isDisposed) {
                        run.run();
                    }
                });
                timer.setRepeats(false);
                timer.start();
                
                return Disposables.fromAction(() -> isDisposed = true);
            }
            
            @Override
            public void dispose() {
                LOG.info("Disposing...");
                isDisposed = true;
            }

            @Override
            public boolean isDisposed() {
                LOG.info("IsDisposed called...");
                return isDisposed;
            }
        };
    }
    
}
