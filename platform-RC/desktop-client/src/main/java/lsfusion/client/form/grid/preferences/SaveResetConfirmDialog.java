package lsfusion.client.form.grid.preferences;

import lsfusion.base.Callback;
import lsfusion.client.Main;
import lsfusion.interop.KeyStrokes;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

import static lsfusion.client.ClientResourceBundle.getString;

public class SaveResetConfirmDialog extends JDialog {
    public boolean forAll = false;
    public boolean complete = false;
    private final boolean save;

    public SaveResetConfirmDialog(boolean save) {
        super(Main.frame, getString(save ? "form.grid.preferences.saving" : "form.grid.preferences.resetting"), true);
        this.save = save;

        ActionListener escListener = new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                setVisible(false);
            }
        };
        KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        getRootPane().registerKeyboardAction(escListener, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    public void show(final Callback callback) {
        JComponent contents;
        setResizable(false);

        if (!Main.configurationAccessAllowed) {
            contents = new JLabel(getString(save ? "form.grid.preferences.sure.to.save" : "form.grid.preferences.sure.to.reset"));
        } else {
            JPanel radios = new JPanel();
            radios.setLayout(new BoxLayout(radios, BoxLayout.Y_AXIS));
            radios.add(new JLabel((getString(save ? "form.grid.preferences.save" : "form.grid.preferences.reset")) + ":"));
            radios.add(Box.createVerticalStrut(6));

            final JRadioButton currentUserRB = new JRadioButton(getString("form.grid.preferences.for.user"), true);
            currentUserRB.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        forAll = false;
                        complete = false;
                    }
                }
            });
            currentUserRB.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (KeyStrokes.isEnterEvent(e)) {
                        okPressed(callback);    
                    }
                }
            });
            final JRadioButton allUsersRB = new JRadioButton(getString("form.grid.preferences.for.all.users"));
            allUsersRB.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        forAll = true;
                        complete = false;
                    }
                }
            });
            allUsersRB.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (KeyStrokes.isEnterEvent(e)) {
                        okPressed(callback);
                    }
                }
            });
            
            ButtonGroup radioGroup = new ButtonGroup();
            radioGroup.add(currentUserRB);
            radioGroup.add(allUsersRB);
            
            radios.add(currentUserRB);
            radios.add(allUsersRB);

            if (!save) {
                final JRadioButton allUsersCompleteRB = new JRadioButton(getString("form.grid.preferences.for.all.users.complete"));
                allUsersCompleteRB.addItemListener(new ItemListener() {
                    @Override
                    public void itemStateChanged(ItemEvent e) {
                        if (e.getStateChange() == ItemEvent.SELECTED) {
                            forAll = true;
                            complete = true;
                        }
                    }
                });
                allUsersCompleteRB.addKeyListener(new KeyAdapter() {
                    @Override
                    public void keyPressed(KeyEvent e) {
                        if (KeyStrokes.isEnterEvent(e)) {
                            okPressed(callback);
                        }
                    }
                });
                
                radioGroup.add(allUsersCompleteRB);
                radios.add(allUsersCompleteRB);
            }

            contents = radios;
        }
        
        contents.setBorder(new EmptyBorder(5, 10, 0, 10));

        JButton okButton = new JButton(getString(Main.configurationAccessAllowed ? "dialog.ok" : "dialog.yes"));
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                okPressed(callback);
            }
        });
        JButton cancelButton = new JButton(getString(Main.configurationAccessAllowed ? "dialog.cancel" : "dialog.no"));
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });
        JPanel buttons = new JPanel();
        buttons.add(okButton);
        buttons.add(cancelButton);

        setLayout(new BorderLayout());
        add(contents, BorderLayout.NORTH);
        add(buttons, BorderLayout.EAST);

        pack();
        setLocationRelativeTo(getParent());
        setVisible(true);
    }

    private void okPressed(Callback callback) {
        setVisible(false);
        callback.done(null);
    }
}
