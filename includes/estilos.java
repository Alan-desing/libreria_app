package includes;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.JTextComponent;

public class estilos {

    // colores
    public static final Color COLOR_FONDO       = Color.decode("#FFF4D8");
    public static final Color COLOR_BARRA       = Color.decode("#ECBA73");
    public static final Color COLOR_TITULO      = Color.decode("#7B6C6C");
    public static final Color COLOR_BOTON       = Color.decode("#798D48");
    public static final Color COLOR_BOTON_HOVER = Color.decode("#7C8A65");

    // Bordes y radios
    public static final Color COLOR_BORDE_CREMA = new Color(0xE6,0xD9,0xBF);
    public static final Color COLOR_BORDE_SUAVE = new Color(0,0,0,35); // borde gris muy suave
    public static final int   RADIO             = 12;

    //stock
    public static final Color BADGE_OK_BG     = new Color(0xE6,0xF6,0xEA);
    public static final Color BADGE_OK_BORDER = new Color(0xC6,0xE9,0xD0);
    public static final Color BADGE_OK_FG     = new Color(0x22,0x6B,0x3D);

    public static final Color BADGE_NO_BG     = new Color(0xFF,0xEC,0xEB);
    public static final Color BADGE_NO_BORDER = new Color(0xF2,0xB6,0xB1);
    public static final Color BADGE_NO_FG     = new Color(0xB9,0x4A,0x48);

    public static final Color BADGE_WARN_BG     = new Color(0xFF,0xF5,0xE6);
    public static final Color BADGE_WARN_BORDER = new Color(0xF1,0xD5,0xA3);
    public static final Color BADGE_WARN_FG     = new Color(0x9A,0x68,0x1A);

    // Fuentes
    public static final Font FUENTE_TITULO = new Font("SansSerif", Font.BOLD, 40);
    public static final Font FUENTE_TEXTO  = new Font("SansSerif", Font.PLAIN, 20);
    public static final Font FUENTE_INPUT  = new Font("SansSerif", Font.PLAIN, 16);
    public static final Font FUENTE_BOTON  = new Font("SansSerif", Font.BOLD, 14);

    
    public static void initLookAndFeel() {
        try {
            Class<?> lafClass = Class.forName("com.formdev.flatlaf.FlatLightLaf");
            LookAndFeel laf = (LookAndFeel) lafClass.getDeclaredConstructor().newInstance();
            UIManager.setLookAndFeel(laf);
        } catch (Exception ignore) {}
        UIManager.put("Panel.background", COLOR_FONDO);
    }

    // Botón grande 
    public static JButton botonRedondeado(String texto) {
        JButton btn = new JButton(texto) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color fill = getModel().isRollover() ? COLOR_BOTON_HOVER : COLOR_BOTON;
                if (getModel().isPressed()) fill = fill.darker();
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), RADIO, RADIO);
                g2.dispose();
                super.paintComponent(g);
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

    //  Botón  (FILTRAR)
    public static JButton botonBlanco(String texto){
        JButton b = new JButton(texto);
        b.setFocusPainted(false);
        b.setFont(FUENTE_BOTON);
        b.setBackground(Color.WHITE);
        b.setOpaque(true);
        b.setBorder(new CompoundBorder(
                new LineBorder(COLOR_BORDE_CREMA, 1, true),
                new EmptyBorder(8, 14, 8, 14)
        ));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { b.setBackground(new Color(0xFF,0xFE,0xF9)); }
            @Override public void mouseExited (java.awt.event.MouseEvent e) { b.setBackground(Color.WHITE); }
        });
        return b;
    }

    // Botón  (Editar)
    public static JButton botonSm(String texto){
        JButton b = new JButton(texto);
        b.setFocusPainted(false);
        b.setFont(new Font("SansSerif", Font.BOLD, 12));
        b.setForeground(Color.WHITE);
        b.setBackground(COLOR_BOTON);
        b.setOpaque(true);
        b.setBorder(new CompoundBorder(
                new LineBorder(COLOR_BORDE_SUAVE, 1, true),
                new EmptyBorder(6, 10, 6, 10)
        ));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { b.setBackground(COLOR_BOTON_HOVER); }
            @Override public void mouseExited (java.awt.event.MouseEvent e) { b.setBackground(COLOR_BOTON); }
        });
        return b;
    }

    // Botón  (Eliminar)
    public static JButton botonSmDanger(String texto){
        JButton b = new JButton(texto);
        b.setFocusPainted(false);
        b.setFont(new Font("SansSerif", Font.BOLD, 12));
        b.setForeground(Color.WHITE);
        b.setBackground(new Color(0xB9,0x4A,0x48));
        b.setOpaque(true);
        b.setBorder(new CompoundBorder(
                new LineBorder(COLOR_BORDE_SUAVE, 1, true),
                new EmptyBorder(6, 10, 6, 10)
        ));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { b.setBackground(new Color(0xA1,0x3F,0x3D)); }
            @Override public void mouseExited (java.awt.event.MouseEvent e) { b.setBackground(new Color(0xB9,0x4A,0x48)); }
        });
        return b;
    }

    
    public static <T> void estilizarCombo(JComboBox<T> combo){
        combo.setFont(FUENTE_INPUT);
        combo.setBackground(Color.WHITE);
        combo.setBorder(new CompoundBorder(
                new LineBorder(COLOR_BORDE_CREMA, 1, true),
                new EmptyBorder(4, 8, 4, 8)
        ));
    }

    //  redondeo del texto
    public static void estilizarCampo(JTextComponent c) {
        c.setFont(FUENTE_INPUT);
        Border line   = new LineBorder(new Color(0xD9,0xD9,0xD9), 1, true);
        Border margin = new EmptyBorder(10, 14, 10, 14);
        c.setBorder(new CompoundBorder(line, margin));
        c.setBackground(Color.WHITE);
    }

    public static JButton botonSmBlanco(String txt){
    JButton b = new JButton(txt);
    b.setFocusPainted(false);
    b.setFont(new Font("Arial", Font.PLAIN, 14));
    b.setBackground(Color.WHITE); // fondo blanco
    b.setForeground(new Color(0x444444)); // texto gris oscuro
    b.setBorder(new CompoundBorder(
        new LineBorder(COLOR_BORDE_CREMA, 1, true), // borde crema
        new EmptyBorder(8, 14, 8, 14)               // padding interno
    ));
    b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    return b;
}

}
