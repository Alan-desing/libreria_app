package admin.ventas;

import javax.swing.*;
import java.awt.*;

public class panel_ventas extends JPanel {
    public panel_ventas() {
        setLayout(new BorderLayout());
        add(new JLabel("Panel de Ventas", SwingConstants.CENTER), BorderLayout.CENTER);
    }
}
