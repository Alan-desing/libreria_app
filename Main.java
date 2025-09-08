// Punto de entrada de la app de escritorio
public class Main {
    public static void main(String[] args) {
        // Lógica: lanzar la UI en el hilo de eventos de Swing
        javax.swing.SwingUtilities.invokeLater(() -> {
            // Lógica: abrir la ventana de login
            new panel_login().setVisible(true);
        });
    }
}
