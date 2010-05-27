package platform.client.form.cell;

import platform.client.logics.ClientCellView;
import platform.client.form.ClientForm;

import javax.swing.*;
import java.awt.*;

public abstract class CellView extends JPanel {

    private final JLabel label;
    private final CellTable table;

    protected abstract ClientCellView getKey();

    int getID() { return getKey().getID() + getKey().getShiftID(); }

    @Override
    public int hashCode() {
        return getID();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CellView && ((CellView) o).getID() == this.getID();
    }

    public CellView() {

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        label = new JLabel();
        label.setBorder(BorderFactory.createEmptyBorder(0,4,0,4));
        add(label);

        table = new CellTable() {

            protected boolean cellValueChanged(Object value) {
                return CellView.this.cellValueChanged(value);
            }

            public boolean isDataChanging() {
                return CellView.this.isDataChanging();
            }

            public ClientCellView getCellView(int col) {
                return getKey();
            }

            public ClientForm getForm() {
                return CellView.this.getForm();
            }
        };
        table.setBorder(BorderFactory.createLineBorder(Color.gray));

        add(table);

    }

    protected abstract boolean cellValueChanged(Object value);
    protected abstract boolean isDataChanging();
    protected abstract ClientForm getForm();

    @Override
    public boolean requestFocusInWindow() {
        return table.requestFocusInWindow();
    }

    void keyChanged(ClientCellView key) {

        label.setText(key.getFullCaption());
        table.keyChanged(key);

        table.setFocusable(key.focusable);
        table.setCellSelectionEnabled(key.focusable);
    }

    void setValue(Object ivalue) {
        table.setValue(ivalue);
    }

    public void startEditing() {
        table.editCellAt(0, 0);

        Component tableEditor = table.getEditorComponent();
        if (tableEditor != null) {

            // устанавливаем следущий компонент фокуса на текущий
            if (tableEditor instanceof JComponent) {
                ((JComponent)tableEditor).setNextFocusableComponent(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner());
            }
            
            tableEditor.requestFocusInWindow();
        }
    }
}