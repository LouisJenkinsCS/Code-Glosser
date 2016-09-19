package edu.bloomu.codeglosser;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.MatteBorder;
import org.openide.windows.TopComponent;

/**
 *
 * @author Drue Coles
 */
public class EditorPanel extends JPanel {    
    private static final Font defaultFont = new Font(Font.SERIF, Font.PLAIN, 24);
    private final JScrollPane scrollPane;
    private final JTextArea textArea;
    private final JButton saveButton;
    private final JButton cancelButton;

    public EditorPanel() {      
        setLayout(new BorderLayout());
        setBorder(new MatteBorder(3, 3, 3, 3, Color.GRAY));
        textArea = new JTextArea(20, 80);
        textArea.setBackground(new Color(220, 220, 220));
        textArea.setFont(defaultFont);
        textArea.setCaretPosition(0);
        
        scrollPane = new JScrollPane(textArea);
        add(scrollPane, BorderLayout.CENTER);

        saveButton = new JButton("Save");
        cancelButton = new JButton("Cancel");
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(new MatteBorder(3, 3, 3, 3, Color.GRAY));
        buttonPanel.setBackground(new Color(220, 220, 220));
        JPanel savePanel = new JPanel();
        JPanel cancelPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(1, 2));
        buttonPanel.add(savePanel);
        buttonPanel.add(cancelPanel);
        savePanel.add(saveButton);
        cancelPanel.add(cancelButton);
        savePanel.setBackground(new Color(220, 220, 220));
        cancelPanel.setBackground(new Color(220, 220, 220));
        add(buttonPanel, BorderLayout.SOUTH);

        cancelButton.addActionListener((ActionEvent e) -> {
            TopComponent parent = (TopComponent) getParent();            
            parent.close();
        });
        
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                CommentEditorTopComponent parent = (CommentEditorTopComponent) getParent();  
                parent.saveGlossedDocument();
                parent.close();
            }
        });
        
    }
    
    public String getComment() {
        return textArea.getText();
    }
}
