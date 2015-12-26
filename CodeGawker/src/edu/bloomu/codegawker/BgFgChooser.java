package edu.bloomu.codegawker;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.ButtonGroup;
import javax.swing.JColorChooser;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.event.ChangeEvent;

/**
 * This class extends JColorChooser to present options for specifying background and 
 * foreground colors within a single dialog.
 *
 * @author Drue Coles
 */
final public class BgFgChooser extends JColorChooser {

    private final JRadioButton bgRadioButton = new JRadioButton("Background");
    private final JRadioButton fgRadioButton = new JRadioButton("Foreground");
    private PreviewPanel previewPanel; 
    private Color bgColor;
    private Color fgColor;

    /**
     * Constructs a color chooser with two radio buttons (for background and foreground),
     * a swatch chooser, and a custom preview panel.
     * @param bgColor initial background color
     * @param fgColor initial foreground color 
     */
    public BgFgChooser(Color bgColor, Color fgColor) {
        this.bgColor = bgColor;
        this.fgColor = fgColor;
        init();        
    }
    
    /**
     * Helper method for constructor. This code was factored out of the constructor to
     * avoid having overrideable method calls during construction.
     */
    private void init() {
        // Panel with two radio buttons allowing the user to specify whether the 
        // background or foreground color is being selected.
        JPanel featureSelectionPanel = new JPanel();
        final ButtonGroup group = new ButtonGroup();
        group.add(bgRadioButton);
        group.add(fgRadioButton);
        bgRadioButton.setSelected(true);
        fgRadioButton.setSelected(false);
        featureSelectionPanel.add(bgRadioButton);
        featureSelectionPanel.add(fgRadioButton);
        this.add(featureSelectionPanel, BorderLayout.NORTH);
        
        // Normally, a JColorChooser presents itself as a tabbed pane enabling a color to
        // be selected in various ways. Here, however, the presentation is simplified by
        // provided a chooser panel for swatches only (other chooser panels are removed).
        for (AbstractColorChooserPanel p : getChooserPanels()) {
            if (!p.getDisplayName().equals("Swatches")) {
                removeChooserPanel(p);
            }
        }
        
        // Use class at end of this file to provide custom preview panel
        previewPanel = new PreviewPanel(this.bgColor, this.fgColor);
        this.add(previewPanel, BorderLayout.SOUTH);
       
        // Define action when user clicks on a swatch
        getSelectionModel().addChangeListener((ChangeEvent e) -> {
            Color c = getColor();
            if (bgRadioButton.isSelected()) {
                previewPanel.setBackgroundColor(c);
                this.bgColor = c;
            }
            if (fgRadioButton.isSelected()) {
                previewPanel.setForegroundColor(c);
                this.fgColor = c;
            }
        });
    }
    

    public Color getBackgroundColor() {
        return bgColor;
    }

    public Color getForegroundColor() {
        return fgColor;
    }
}

/**
 * Preview panel for a color chooser that enables the user to specify a background color
 * and a foreground color. 
 *
 * @author Drue Coles
 */
class PreviewPanel extends JPanel {
    
    // Sample text is two lines of generic Java source code.
    private static final String TEXT = "int sum = num1 + num2;\nSystem.out.println(sum);";
    
    private static final int FONT_SIZE = 18;
    private static final Font FONT = new Font(Font.MONOSPACED, Font.BOLD, FONT_SIZE);
    private final JTextArea textArea = new JTextArea(TEXT);

    /**
     * Sets up a text area inside a panel with insets, and that within a panel with an
     * etched titled border.
     * @param bg the initial background color
     * @param fg the initial foreground color
     */
    public PreviewPanel(Color bg, Color fg) {
        super(new GridLayout(0, 1));
        init(bg, fg);
    }
    
    private void init(Color bg, Color fg) {
        JPanel innerPanel = new JPanel();
        innerPanel.setLayout(new GridLayout(1, 1));
        innerPanel.setBorder(new EmptyBorder(5, 5, 10, 5));
        innerPanel.add(textArea);
        add(innerPanel);
        textArea.setFont(FONT); 
        textArea.setBackground(bg);
        textArea.setForeground(fg);
        setBorder(new TitledBorder(new EtchedBorder(), "Preview"));
    }

    public void setForegroundColor(Color c) {
        textArea.setForeground(c);
        textArea.repaint();
    }

    public void setBackgroundColor(Color c) {
        textArea.setBackground(c);
        textArea.repaint();
    }
}
