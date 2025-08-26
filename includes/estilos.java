package includes;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.JTextComponent;

public class estilos {

    // 🎨 Colores
    public static final Color COLOR_FONDO  = Color.decode("#FFF4D8");
    public static final Color COLOR_BARRA  = Color.decode("#ECBA73");
    public static final Color COLOR_TITULO = Color.decode("#7B6C6C");
    public static final Color COLOR_BOTON  = Color.decode("#798D48");
    public static final Color COLOR_BOTON_HOVER = Color.decode("#7C8A65"); // oliva más oscuro (web)

    // 📝 Fuentes
    public static final Font FUENTE_TITULO = new Font("SansSerif", Font.BOLD, 40);
    public static final Font FUENTE_TEXTO  = new Font("SansSerif", Font.PLAIN, 20);
    public static final Font FUENTE_INPUT  = new Font("SansSerif", Font.PLAIN, 16);
    public static final Font FUENTE_BOTON  = new Font("SansSerif", Font.BOLD, 14);

    // (Opcional) FlatLaf
    public static void initLookAndFeel() {
        try {
            Class<?> lafClass = Class.forName("com.formdev.flatlaf.FlatLightLaf");
            LookAndFeel laf = (LookAndFeel) lafClass.getDeclaredConstructor().newInstance();
            UIManager.setLookAndFeel(laf);
        } catch (Exception ignore) {}
        UIManager.put("Panel.background", COLOR_FONDO);
    }

    // 🔘 Botón redondeado (con hover de la paleta)
    public static JButton botonRedondeado(String texto) {
        JButton btn = new JButton(texto) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color fill = getModel().isRollover() ? COLOR_BOTON_HOVER : COLOR_BOTON;
                if (getModel().isPressed()) fill = fill.darker();
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
                super.paintComponent(g); // dibuja el texto
            }
        };
        btn.setFocusPainted(false);
        btn.setForeground(Color.WHITE);
        btn.setFont(FUENTE_BOTON);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setBorder(new EmptyBorder(10, 24, 10, 24));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ✏️ Estilo base para campos de texto (bordes redondeados y padding)
    public static void estilizarCampo(JTextComponent c) {
        c.setFont(FUENTE_INPUT);
        Border line   = new LineBorder(new Color(0xD9,0xD9,0xD9), 1, true); // redondeado
        Border margin = new EmptyBorder(10, 14, 10, 14);
        c.setBorder(new CompoundBorder(line, margin));
        c.setBackground(Color.WHITE);
    }
}
