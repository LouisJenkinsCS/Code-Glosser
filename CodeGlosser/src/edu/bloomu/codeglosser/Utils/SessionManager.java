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
import edu.bloomu.codeglosser.Model.Markup;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author Louis Jenkins
 * 
 * A simple session manager, that preserves all session data for a given file.
 */
public class SessionManager {

    private static final Logger LOG = Logger.getLogger(SessionManager.class.getName());
    
    private static final String FILE_NAME = "session.json";
    private static final String MARKUPS = "Markups";
    
    /**
     * Initialize by creating the session data object ahead of time.
     */
    public static void init() {
       LOG.info("Initializing session data...");
       
       Observable
               .just(Globals.PROJECT_FOLDER + "\\" + FILE_NAME)
               .observeOn(Schedulers.io())
               .map(Paths::get)
               // Only initialize if file does not exist
               .filter(path -> !Files.exists(path))
               // Create empty file
               .subscribe(path -> {
                   FileWriter writer = new FileWriter(path.toFile());
                   writer.write("{}");
                   writer.close();
                });
    }
    
    /**
     * Saves all markups.
     * @param markups Markups.
     */
    public static void saveSession(List<Markup> markups) {
        LOG.info("Saving session data...");
        
        // Fist time loading file
        if (Globals.CURRENT_FILE == null) {
            return;
        }
        
        String key = Globals.PROJECT_FOLDER.relativize(Globals.CURRENT_FILE).toString();
        Observable
                .fromIterable(markups)
                .observeOn(Schedulers.computation())
                .map(Markup::serialize)
                .buffer(Integer.MAX_VALUE)
                .map(serializedMarkup -> {
                    JSONArray arr = new JSONArray();
                    arr.addAll(serializedMarkup);
                    return arr;
                })
                .observeOn(Schedulers.io())
                // Write to file
                .subscribe(SessionManager::writeData);
    }
    
    public static boolean sessionExists(Path path) {
        LOG.info("Determining if session exists: " + path);
        
        String key = Globals.PROJECT_FOLDER.relativize(path).toString();
        return getJSONContents()
                .any(json -> json.containsKey(key))
                .blockingGet();
    }
    
    public static Observable<JSONObject> getJSONContents() {
        Path dataPath = Paths.get(Globals.PROJECT_FOLDER + "\\" + FILE_NAME);
        
        return Observable
                .just(dataPath)
                .flatMap(FileUtils::getContents)
                .map(json -> (JSONObject) new JSONParser().parse(json));
    }
    
    public static List<Markup> loadSession(Path path) {
        LOG.info("Loading session data...");
        
        String key = Globals.PROJECT_FOLDER.relativize(path).toString();
        List<Markup> markups = (List<Markup>) getJSONContents()
                .filter(json -> json.containsKey(key))
                .map(json -> (JSONArray) json.get(key))
                .flatMap(Observable::fromIterable)
                .map(data -> Markup.deserialize((JSONObject) data))
                .toList()
                .blockingGet();
        
        if (markups == null) {
            markups = Collections.EMPTY_LIST;
        }
        
        LOG.info("Received Markups: " + markups);
        
        return markups;
    }
    
    private static void writeData(JSONArray data) {
        LOG.info("Writing session data to: " + Globals.PROJECT_FOLDER + "\\" + FILE_NAME);
        
        String key = Globals.PROJECT_FOLDER.relativize(Globals.CURRENT_FILE).toString();
        Observable
                .just(Globals.PROJECT_FOLDER + "\\" + FILE_NAME)
                .observeOn(Schedulers.io())
                .map(Paths::get)
                .map(Files::readAllLines)
                .observeOn(Schedulers.computation())
                .map(list -> list.stream().collect(Collectors.joining("\n")))
                .doOnNext(json -> LOG.info("Contents: " + json))
                .map(json -> (JSONObject) new JSONParser().parse(json))
                .doOnNext(obj -> ((JSONObject) obj).put(key, data))
                .observeOn(Schedulers.io())
                .doOnNext(json -> LOG.info("Writing data: " + json.toJSONString()))
                .subscribe(allData -> {
                    FileWriter writer = new FileWriter(new File(Globals.PROJECT_FOLDER + "\\" + FILE_NAME));
                    writer.write(allData.toJSONString());
                    writer.close();
                });
    }
    
}
