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

import edu.bloomu.codeglosser.Globals;
import edu.bloomu.codeglosser.main;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 *
 * @author Louis Jenkins
 */
public class FileUtils {

    private static final Logger LOG = Logger.getLogger(FileUtils.class.getName());
    
    /**
     * Obtain the contents of the given file located at the path, if it exists. 
     * If the file does not exist, an empty Observable is returned. The scheduler
     * is returned as CPU Bound, and so the caller may need to restore to a
     * previous one.
     * @param path Path to file
     * @return Contents or empty
     */
    public static Observable<String> getContents(Path path) {
        if (!path.toFile().exists()) {
            return Observable.empty();
        }
        
        return Observable
                .just(path)
                .observeOn(Schedulers.io())
                .map(Files::readAllLines)
                .observeOn(Schedulers.computation())
                .map(list -> list.stream().collect(Collectors.joining("\n")));         
    }
    
    public static String getExtension(Path path) {
        String fileName = path.toString();
        int offset = fileName.indexOf('.');
        LOG.info("Extension: " + fileName.substring(offset + 1));
        return fileName.substring(offset + 1);
    }
    
    public static String readAll(String fileName) {
        InputStream is = main.class.getResourceAsStream(fileName);
        if (is == null) {
            throw new RuntimeException("Filename: " + fileName + " was not found...");
        }
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        StringBuilder contents = new StringBuilder();
        return br.lines().collect(Collectors.joining("\n"));
    }

    public static File temporaryFile(String fileName, String contents) throws IOException  {
        File f = new File(fileName);
        f.createNewFile();
        BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(f));
        stream.write(contents.getBytes());
        stream.flush();
        stream.close();
        
        return f;
    }
}
