package edu.bloomu.codeglosser;

import edu.bloomu.codeglosser.Controller.NoteManager;
import edu.bloomu.codeglosser.Utils.DocumentHelper;
import edu.bloomu.codeglosser.View.NoteDescriptorPane;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
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

    private final GlossableTextArea gTextArea;
    private final NoteDescriptorPane nDescrPane;
    private static final char SYM = '\u2691'; // flag

    public GlossableTopComponent(Document doc) {
        nDescrPane = new NoteDescriptorPane();
        setDisplayName(DocumentHelper.getDocumentName(doc) + ".html");
        setLayout(new BorderLayout());        
        gTextArea = new GlossableTextArea(doc);
        JScrollPane scrollPane = new JScrollPane(gTextArea);
        JScrollPane spane = new JScrollPane(nDescrPane);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPane, spane);
        split.setOneTouchExpandable(true);
        split.setDividerLocation(.5);
        split.setResizeWeight(.5);
        add(split, BorderLayout.CENTER);
        // Initialize NoteManager...
        NoteManager.setNoteView(nDescrPane);
        NoteManager.setNotepadView(gTextArea);
        NoteManager manager = NoteManager.getInstance(DocumentHelper.getDocumentName(doc));
        gTextArea.setController(manager);
        nDescrPane.setController(manager);
    }
}
