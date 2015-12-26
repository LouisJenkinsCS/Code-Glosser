package edu.bloomu.codegawker;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.*;

/**
 * This class is used to set up an options Panel for Code Gawker via menu selection
 * Tools->Options->Miscellaneous.
 * 
 * @author Drue Coles
 */
@OptionsPanelController.SubRegistration(
        displayName = "#AdvancedOption_DisplayName_CodeGawker",
        keywords = "#AdvancedOption_Keywords_CodeGawker",
        keywordsCategory = "Advanced/CodeMagnifier"
)
@org.openide.util.NbBundle.Messages({"AdvancedOption_DisplayName_CodeGawker=Code Gawker", 
    "AdvancedOption_Keywords_CodeGawker=gawker"})
public final class CodeGawkerOptionsPanelController extends OptionsPanelController {

    private CodeGawkerOptionsPanel panel;
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private boolean changed;

    @Override
    public void update() {
        getPanel().load();
        changed = false;
    }

    @Override
    public void applyChanges() {
        SwingUtilities.invokeLater(() -> {
            getPanel().store();
            changed = false;
        });
    }

    @Override
    public void cancel() {
        // need not do anything special, if no changes have been persisted yet
    }

    @Override
    public boolean isValid() {
        return getPanel().valid();
    }

    @Override
    public boolean isChanged() {
        return changed;
    }

    @Override
    public HelpCtx getHelpCtx() {
        return null; // new HelpCtx("...ID") if you have a help set
    }

    @Override
    public JComponent getComponent(Lookup masterLookup) {
        return getPanel();
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l) {
        pcs.addPropertyChangeListener(l);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l) {
        pcs.removePropertyChangeListener(l);
    }

    private CodeGawkerOptionsPanel getPanel() {
        if (panel == null) {
            panel = new CodeGawkerOptionsPanel(this);
        }
        return panel;
    }

    void changed() {
        if (!changed) {
            changed = true;
            pcs.firePropertyChange(OptionsPanelController.PROP_CHANGED, false, true);
        }
        pcs.firePropertyChange(OptionsPanelController.PROP_VALID, null, null);
    }
}
