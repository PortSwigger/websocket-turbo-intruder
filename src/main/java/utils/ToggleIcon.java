package utils;

import javax.swing.*;
import java.awt.*;

public class ToggleIcon implements Icon {
    private final int width;
    private final int height;

    private final Color hoverOnOnColor = new Color(55, 98, 153);
    private final Color hoverOffOnColor = new Color(53, 80, 114);
    private final Color hoverOnOffColor = Color.GRAY;
    private final Color hoverOffOffColor = Color.DARK_GRAY;

    public ToggleIcon(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        AbstractButton b = (AbstractButton) c;
        ButtonModel m = b.getModel();
        boolean selected = m.isSelected();
        boolean rollover = m.isRollover();
        Color trackColor = Color.GRAY;
        if (b.isEnabled() && !rollover) trackColor = hoverOffOffColor;
        if (b.isEnabled() && rollover) trackColor = hoverOnOffColor;
        if (selected && rollover) trackColor = hoverOnOnColor;
        if (selected && !rollover) trackColor = hoverOffOnColor;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.translate(x, y);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(trackColor);
        g2.fillRoundRect(0, 0, width, height, height, height);

        g2.setColor(Color.LIGHT_GRAY);
        int knobSize = height - 4;
        int knobX = selected
                ? width - knobSize - 2
                : 2;
        g2.fillOval(knobX, 2, knobSize, knobSize);

        g2.dispose();
    }

    @Override
    public int getIconWidth() {
        return width;
    }

    @Override
    public int getIconHeight() {
        return height;
    }
}