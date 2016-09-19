package edu.bloomu.codeglosser;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import javax.swing.JScrollPane;
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
    private static final char SYM = '\u2691'; // flag

    public GlossableTopComponent(Document doc) {
        GlossedDocument gDoc = new GlossedDocument(doc);
        setDisplayName(gDoc.getDocumentName() + ".html");
        setLayout(new BorderLayout());        
        gTextArea = new GlossableTextArea(gDoc);
        gTextArea.setPreferredSize(new Dimension(this.getBounds().width, this.getBounds().height));
        JScrollPane scrollPane = new JScrollPane(gTextArea);
        scrollPane.setPreferredSize(new Dimension(this.getBounds().width, this.getBounds().height));
        add(scrollPane, BorderLayout.CENTER);
    }
}
