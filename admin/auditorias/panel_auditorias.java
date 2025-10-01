package admin.auditorias;

import javax.swing.*;
import java.awt.*;

public class panel_auditorias extends JPanel {
    public panel_auditorias() {
        setLayout(new BorderLayout());
        add(new JLabel("Panel de Auditorías", SwingConstants.CENTER), BorderLayout.CENTER);
    }
}
