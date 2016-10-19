package edu.bloomu.codeglosser;

import com.google.common.eventbus.EventBus;
import edu.bloomu.codeglosser.Controller.NoteManager;
import edu.bloomu.codeglosser.Controller.NotePadController;
import edu.bloomu.codeglosser.Events.FileChangeEvent;
import edu.bloomu.codeglosser.Utils.DocumentHelper;
import edu.bloomu.codeglosser.View.MarkupView;
import edu.bloomu.codeglosser.View.NotePropertiesView;
import edu.bloomu.codeglosser.View.NotePadView;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.io.File;
import java.net.URL;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import org.openide.windows.TopComponent;

/**
 *
 * @author Drue Coles
 */
@TopComponent.Description(
        preferredID = "GlossTopComponent",
        //iconBase="SET/PATH/TO/ICON/HERE", 
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "glossEditor", openAtStartup = false)
public class GlossableTopComponent extends TopComponent {
    
    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(GlossableTopComponent.class.getName());

    private static final char SYM = '\u2691'; // flag
    private final EventBus bus = new EventBus();
    

    public GlossableTopComponent(Document doc) {
        doc.putProperty(DefaultEditorKit.EndOfLineStringProperty, "\r\n");
        setDisplayName(DocumentHelper.getDocumentName(doc) + ".html");
        setLayout(new BorderLayout());      
        MarkupView v = new MarkupView();
        add(v, BorderLayout.CENTER);
        NoteManager.getInstance(DocumentHelper.getDocumentName(doc));
        v.setDocument(doc);
        v.setEventBus(bus);
        bus.post(FileChangeEvent.of(DocumentHelper.getDocumentName(doc)));
    }
}
