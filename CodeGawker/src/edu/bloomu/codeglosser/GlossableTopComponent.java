package edu.bloomu.codeglosser;

import edu.bloomu.codeglosser.Controller.NoteManager;
import edu.bloomu.codeglosser.Controller.NotePadController;
import edu.bloomu.codeglosser.Utils.DocumentHelper;
import edu.bloomu.codeglosser.View.NoteDescriptorPane;
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

    private final NotePadController nPad;
    private final NoteDescriptorPane nDescrPane;
    private static final char SYM = '\u2691'; // flag
    

    public GlossableTopComponent(Document doc) {
        doc.putProperty(DefaultEditorKit.EndOfLineStringProperty, "\r\n");
        nDescrPane = new NoteDescriptorPane();
        setDisplayName(DocumentHelper.getDocumentName(doc) + ".html");
        setLayout(new BorderLayout());        
        nPad = new NotePadController();
        JScrollPane scrollPane = new JScrollPane(nPad.getView());
        JScrollPane spane = new JScrollPane(nDescrPane);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPane, spane);
        split.setOneTouchExpandable(true);
        split.setDividerLocation(.5);
        split.setResizeWeight(.5);
        add(split, BorderLayout.CENTER);
        // Initialize NoteManager...
        NoteManager.setNoteView(nDescrPane);
        NoteManager.setNotepadView(nPad.getView());
        nPad.setModelDocument(doc);
        NoteManager manager = NoteManager.getInstance(DocumentHelper.getDocumentName(doc));
//        nPad.setController(manager);
        nDescrPane.setController(manager);
    }
}
