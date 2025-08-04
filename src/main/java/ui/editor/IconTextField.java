package ui.editor;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;

public class IconTextField extends JTextField {
    private static final int ICON_SIZE    = 16;
    private static final int ICON_PADDING = 2;

    private final JButton iconButton;
    private final String  placeholder;

    public IconTextField(String placeholder) {
        super();
        this.placeholder = placeholder;
        setColumns(20);
        setLayout(new BorderLayout());
        setOpaque(true);

        iconButton = new JButton() {
            @Override
            protected void paintComponent(Graphics g0) {
                super.paintComponent(g0);
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
//                g.setColor(getForeground());

                int w = getWidth(), h = getHeight();
                int pad = ICON_PADDING;
                int size = ICON_SIZE;
                int y   = (h - size) / 2;
                int x   = pad;

                if (IconTextField.this.getText().isEmpty()) {
                    int ovalX = x + 4;
                    int ovalY = y;
                    int ovalSize = size - 6;
                    g.setStroke(new BasicStroke(2f));
                    g.drawOval(ovalX, ovalY, ovalSize, ovalSize);
                    g.drawLine(ovalX, ovalY + ovalSize, ovalX - 5, ovalY + ovalSize + 5);
                } else {
                    int inset = 4;
                    int bx = pad + inset;
                    int by = pad + inset;
                    int ex = w - pad - inset;
                    int ey = h - pad - inset;
                    g.drawLine(bx, by, ex, ey);
                    g.drawLine(bx, ey, ex, by);
                }
                g.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                int total = ICON_SIZE + ICON_PADDING * 2;
                return new Dimension(total, total);
            }
        };
        iconButton.setBorderPainted(false);
        iconButton.setContentAreaFilled(false);
        iconButton.setFocusable(false);
        iconButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        iconButton.addActionListener(e -> {
            if (!getText().isEmpty()) {
                setText("");
            } else {
                requestFocusInWindow();
            }
        });
        add(iconButton, BorderLayout.EAST);

        getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { update(); }
            public void removeUpdate(DocumentEvent e)  { update(); }
            public void changedUpdate(DocumentEvent e) { update(); }
            private void update() {
                iconButton.repaint();
                repaint();
            }
        });

        addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { repaint(); }
            @Override public void focusLost(FocusEvent e)   { repaint(); }
        });
    }

    @Override
    public Insets getInsets() {
        Insets i = super.getInsets();
        int padRight = ICON_SIZE + ICON_PADDING * 2;
        return new Insets(i.top, i.left, i.bottom, i.right + padRight);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);

        if (getText().isEmpty() && !isFocusOwner()) {
            Graphics2D g = (Graphics2D)g0.create();

            FontMetrics fm = g.getFontMetrics();
            int x = getInsets().left;
            int y = (getHeight() - fm.getHeight())/2 + fm.getAscent();
            g.drawString(placeholder, x, y);
            g.dispose();
        }
    }
}