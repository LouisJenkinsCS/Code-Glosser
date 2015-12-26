package edu.bloomu.codegawker;

import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.JFrame;

/**
 * This class packs a code fragment into a frame. Resizing the frame causes the font to
 * be adjusted for optimal fit.
 * 
 * @author Drue Coles
 */
public class CodeFragmentViewer extends JFrame {

    public CodeFragmentViewer(String name, String code) {
        init(name, code);        
    } 
    
    /**
     * Helper method for constructor. This code was factored out of the constructor to
     * avoid having overrideable method calls during construction.
     */
    private void init(String name, String code) {
        setTitle(name);
        final CodeFragment textArea = new CodeFragment(code, this);
        add(textArea);        
        pack();
        setResizable(true);
        setVisible(true);        
        
        // I want the resize listener to be invoked only when the resizing of this
        // frame is complete. According to the documentation, this option is not
        // guaranteed to be supported on all systems, so I should insert code here
        // to check that it is supported, and if not the frame should be non-resizeable.
        Toolkit.getDefaultToolkit().setDynamicLayout( false );

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {    
                textArea.resetFont();
                pack();              
            }
        });
    }
}
