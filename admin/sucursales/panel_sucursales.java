package admin.sucursales;

import includes.estilos;
import javax.swing.*;
import java.awt.*;

public class panel_sucursales extends JPanel {

    public panel_sucursales() {
        setLayout(new BorderLayout());
        setBackground(estilos.COLOR_FONDO);

        JLabel h1 = new JLabel("Sucursales");
        h1.setFont(estilos.FUENTE_TITULO.deriveFont(Font.BOLD, 28f));
        h1.setForeground(estilos.COLOR_TITULO);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(24, 32, 8, 32));
        header.add(h1, BorderLayout.WEST);

        add(header, BorderLayout.NORTH);
        add(new JLabel("Contenido sucursales…", SwingConstants.CENTER), BorderLayout.CENTER);
    }
}
