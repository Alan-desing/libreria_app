public class Main {
    public static void main(String[] args) {
        // Arranca la aplicación mostrando el login
        javax.swing.SwingUtilities.invokeLater(() -> {
            new panel_login().setVisible(true);
        });
    }
}
