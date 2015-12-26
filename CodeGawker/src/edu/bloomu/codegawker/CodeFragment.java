package edu.bloomu.codegawker;

import java.awt.Color;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import org.openide.util.Exceptions;

/**
 * This class is used to display a fragment of source code, with options (via pop-up menu)
 * to set colors, change the font size, save as a text file, and send the fragment to the
 * local printer.
 *
 * @author Drue Coles
 */
public class CodeFragment extends JTextArea {

    private static final int INIT_FONT_SIZE = 32;
    private static final int MIN_FONT_SIZE = 12;
    private static final int MAX_FONT_SIZE = 64;
    public static final Insets INSETS = new Insets(0, 5, 1, 5);
    private static final int PADDING = INSETS.top + INSETS.bottom;

    // This field is needed so that menu item listeners can invoke the setAlwaysOnTop 
    // method of the containing frame. They do so through a method of this class.
    private final JFrame parent;

    private int rows; // number of lines of code in the fragment
    private int cols; // greatest number of characters in a line

    public CodeFragment(String code, JFrame parent) {
        this.parent = parent;
        init(code);

    }

    /**
     * Helper method for constructor. This code was factored out of the constructor to
     * avoid having overrideable method calls during construction.     
     * @param code 
     */
    private void init(String code) {
        // find maximum number of columns
        String[] temp = code.split("\n");
        rows = temp.length;
        setRows(rows);
        int numCols = temp[0].length();
        for (int i = 1; i < temp.length; i++) {
            if (temp[i].length() > numCols) {
                numCols = temp[i].length();
            }
        }
        cols = numCols;
        setColumns(cols);

        setFont(new Font(Font.MONOSPACED, Font.BOLD, INIT_FONT_SIZE));
        setMargin(INSETS);
        setText(code);
        setEditable(false);
        addMouseListener(new PopupListener(this));
    }

    /**
     * Used to configure the frame containing this code fragment so that it is (or is not)
     * always on top of other windows.
     *
     * @param onTop
     */
    public void setAlwaysOnTop(boolean onTop) {
        parent.setAlwaysOnTop(onTop);
    }

    /**
     * Resets the font to the optimal size for the current dimensions of this component.
     */
    public void resetFont() {
        Font currentFont = getFont();
        int fontHeight = currentFont.getSize();
        int componentHeight = getRootPane().getHeight();
        if (fontHeight * rows + PADDING < componentHeight) { // current font too small
            setFont(getBestLargerFont(currentFont, componentHeight));
        } else { // current font too big
            setFont(getBestSmallerFont(currentFont, componentHeight));
        }
        repaint();
    }

    /**
     * Helper method used by resetFont.
     *
     * @param font the current font
     * @param componentHeight the height in pixels of this component
     * @return the largest font smaller than this one that fits in the component
     */
    private Font getBestSmallerFont(Font font, int componentHeight) {
        int fHeight = getFontMetrics(font).getHeight();
        int size = font.getSize();
        while (fHeight * rows + PADDING > componentHeight && size > MIN_FONT_SIZE) {
            size--;
            font = new Font(Font.MONOSPACED, Font.BOLD, size);
            fHeight = getFontMetrics(font).getHeight();
        }
        return font;
    }

    /**
     * Helper method used by resetFont.
     *
     * @param font the current font
     * @param componentHeight the height in pixels of this component
     * @return the smallest font larger than this one that fits in the component
     */
    private Font getBestLargerFont(Font font, int componentHeight) {
        int fHeight = getFontMetrics(font).getHeight();
        int size = font.getSize();
        while (fHeight * rows + PADDING < componentHeight && size < MAX_FONT_SIZE) {
            size++;
            font = new Font(Font.MONOSPACED, Font.BOLD, size);
            fHeight = getFontMetrics(font).getHeight();
        }
        return font;
    }
}

/**
 * Custom mouse listener for a code fragment's pop-up menu.
 *
 * @author Drue Coles
 */
class PopupListener extends MouseAdapter {

    // This class stores a reference to the code fragment that has registered this 
    // listener for two reasons: (1) so that the setAlwaysOnTop method of the code 
    // fragment can be invoked from here and (2) the getText method can be invoked
    // for the save option. 
    JPopupMenu popup;
    CodeFragment codeFragment;

    PopupListener(CodeFragment textArea) {
        this.codeFragment = textArea;

        popup = new JPopupMenu();
        JMenuItem colorsMenuItem = new JMenuItem("Colors");
        JMenuItem saveMenuItem = new JMenuItem("Save");
        JMenuItem printMenuItem = new JMenuItem("Print");
        JMenu alwaysOnTopSubmenu = new JMenu("Always On Top");

        popup.add(colorsMenuItem);
        popup.addSeparator();
        popup.add(saveMenuItem);
        popup.add(printMenuItem);
        popup.addSeparator();
        popup.add(alwaysOnTopSubmenu);

        // Set up the sub-menu items
        JMenuItem yesMenuItem = new JMenuItem("Yes");
        JMenuItem noMenuItem = new JMenuItem("No");
        alwaysOnTopSubmenu.add(yesMenuItem);
        alwaysOnTopSubmenu.add(noMenuItem);

        // change background or foreground colors of code fragment
        colorsMenuItem.addActionListener((ActionEvent e) -> {
            Color bg = textArea.getBackground();
            Color fg = textArea.getForeground();
            final BgFgChooser chooser = new BgFgChooser(bg, fg);
            ActionListener okListener = (ActionEvent e1) -> {
                textArea.setBackground(chooser.getBackgroundColor());
                textArea.setForeground(chooser.getForegroundColor());
                textArea.repaint();
            };

            String title = "Choose Background and Foreground Colors";
            JDialog dialog = JColorChooser.createDialog(null, title, true, chooser,
                    okListener, null);
            dialog.setResizable(false);
            dialog.setVisible(true);
            dialog.setModalityType(Dialog.ModalityType.MODELESS);
        });

        // Print code fragment.
        printMenuItem.addActionListener((ActionEvent e) -> {
            try {
                codeFragment.print();
            } catch (PrinterException ex) {
                String msg = "Printer exception: " + ex.toString();
                JOptionPane.showMessageDialog(null, msg);
            }
        });

        // Save code fragment as text file.
        saveMenuItem.addActionListener((ActionEvent e) -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
            int result = fileChooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                String filename = fileChooser.getSelectedFile().getAbsolutePath();
                Path path = FileSystems.getDefault().getPath(filename);
                String code = codeFragment.getText();
                byte data[] = code.getBytes();
                try {
                    Files.write(path, data);
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        });

        // Make containing frame stay on top of all other windows
        yesMenuItem.addActionListener((ActionEvent e) -> {
            textArea.setAlwaysOnTop(true);
        });

        // Let containing frame be covered by other windows
        noMenuItem.addActionListener((ActionEvent e) -> {
            textArea.setAlwaysOnTop(false);
        });
    }

    @Override
    public void mousePressed(MouseEvent e) {
        maybeShowPopup(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        maybeShowPopup(e);
    }

    private void maybeShowPopup(MouseEvent e) {
        if (e.isPopupTrigger()) {
            popup.show(e.getComponent(), e.getX(), e.getY());
        }
    }
}
