/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.bloomu.codeglosser.Controller;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import edu.bloomu.codeglosser.Events.FileChangeEvent;
import edu.bloomu.codeglosser.Model.NotePadModel;
import edu.bloomu.codeglosser.Utils.DocumentHelper;
import edu.bloomu.codeglosser.Utils.HTMLGenerator;
import edu.bloomu.codeglosser.View.NotePadView;
import io.reactivex.Observable;
import java.awt.Desktop;
import java.io.BufferedOutputStream;
import edu.bloomu.codeglosser.Utils.Bounds;
import edu.bloomu.codeglosser.Events.MarkupColorChangeEvent;
import edu.bloomu.codeglosser.Events.NoteSelectedChangeEvent;
import edu.bloomu.codeglosser.Model.Note;
import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.logging.Logger;
import javax.swing.text.Document;
import org.openide.util.Exceptions;

/**
 *
 * @author Louis
 */
public class NotePadController {

    private static final Logger LOG = Logger.getLogger(NotePadController.class.getName());
    
    
    private final NotePadView view;
    private final NotePadModel model;
    private NoteManager manager;
    private EventBus bus;
    
    public NotePadController(NotePadView v) {
        view = v;
        model = new NotePadModel();
        
        view.onPreviewHTML()
                .subscribe((ignored) -> {
                    String html = HTMLGenerator.generate(model.getTitle(), model.getText());
                    try {
                        File f = new File("tmp.html");
                        f.createNewFile();
                        BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(f));
                        stream.write(html.getBytes());
                        stream.flush();
                        stream.close();
                        Desktop.getDesktop().browse(f.toURI());
                    } catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                });
        
        view.onCreateSelection()
                .doOnNext((b) -> LOG.info("onShowSelection: " + b.toString()))
                .doOnNext(view::addMarkup)
                .map((b) -> manager.createNote(b))
                .subscribe((n) -> bus.post(NoteSelectedChangeEvent.of(n)));
        
        view.onDeleteSelection()
                .doOnNext((b) -> LOG.info("onDeleteSelection: " + b.toString()))
                .subscribe((b) -> {
                    view.removeMarkup(b);
                    manager.deleteNote(b);
                });
        
        view.onShowSelection()
                .doOnNext((b) -> LOG.info("onShowSelection: " + b.toString()))
                .map((b) -> manager.getNote(b))
                .subscribe((opt) -> opt.ifPresent((n) -> bus.post(NoteSelectedChangeEvent.of(n))));
    }
    
    public void setModelDocument(Document doc) {
        model.setText(DocumentHelper.getText(doc));
        model.setTitle(DocumentHelper.getDocumentName(doc));
        view.setText(model.toHTML());
    }
    
    @Subscribe
    public void highlightColorChange(MarkupColorChangeEvent e) {
        view.setMarkupColor(e.getBounds(), e.getColor());
    }
    
    @Subscribe
    public void handleFileChange(FileChangeEvent event) {
        manager = NoteManager.getInstance(event.getFileName());
    }
    
    public void setEventBus(EventBus bus) {
        this.bus = bus;
    }

    public NotePadView getView() {
        return view;
    }

    public NotePadModel getModel() {
        return model;
    }
    
    public String getText() {
        return model.getText();
    }
    
    public String getTitle() {
        return model.getTitle();
    }
}
