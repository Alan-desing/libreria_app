import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.mindrot.jbcrypt.BCrypt;

public class conexion_bd {

    // Datos conexión 
    private static final String URL = "jdbc:mysql://localhost:3306/librerial?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    
    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            System.err.println("❌ Error de conexión: " + e.getMessage());
            return null;
        }
    }

    // Verifica con la tabala 
    public static boolean verificarLogin(String emailOUser, String passwordPlano) {
        final String sql = "SELECT password, contrasena FROM usuario "
                         + "WHERE email = ? AND id_estado_usuario = 1 LIMIT 1";

        try (Connection cn = getConnection()) {
            if (cn == null) {
                System.err.println("❌ No hay conexión a la BD");
                return false;
            }

            try (PreparedStatement ps = cn.prepareStatement(sql)) {
                ps.setString(1, emailOUser);

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        // No existe el email
                        return false;
                    }

                    
                    String hash = rs.getString("password");
                    if (hash == null || hash.isEmpty()) {
                        hash = rs.getString("contrasena");
                    }
                    if (hash == null || hash.isEmpty()) {
                        return false;
                    }

                    String normalized = hash.replaceFirst("^\\$2y\\$", "\\$2a\\$");

                    if (normalized.startsWith("$2a$") || normalized.startsWith("$2b$")) {
                        return BCrypt.checkpw(passwordPlano, normalized);
                    } else {

                        return passwordPlano.equals(hash);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ SQL Error: " + e.getMessage());
            return false;
        }
    }
}
