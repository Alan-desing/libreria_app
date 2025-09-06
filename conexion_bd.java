import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.mindrot.jbcrypt.BCrypt;

public class conexion_bd {

    // ---- Configuración de conexión (BD ACTUAL: libreria) ----
    private static final String URL  =
        "jdbc:mysql://localhost:3306/libreria?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    // Activa prints de depuración en consola
    private static final boolean DEBUG = true;

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            System.err.println("❌ Error de conexión: " + e.getMessage());
            return null;
        }
    }

    /**
     * Login compatible con PHP password_hash/password_verify (bcrypt $2y$).
     * - Sólo usa la columna 'contrasena' (hash bcrypt $2y$…).
     * - Normaliza $2y -> $2a para jBCrypt.
     * - Requiere id_estado_usuario = 1 (Activo).
     */
    public static boolean verificarLogin(String email, String passwordPlano) {
        final String sql =
            "SELECT contrasena, id_estado_usuario " +
            "FROM usuario " +
            "WHERE email = ? " +
            "LIMIT 1";

        try (Connection cn = getConnection()) {
            if (cn == null) {
                if (DEBUG) System.err.println("❌ No hay conexión a la BD");
                return false;
            }

            try (PreparedStatement ps = cn.prepareStatement(sql)) {
                ps.setString(1, email.trim());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        if (DEBUG) System.out.println("⚠️ No existe el email en BD: " + email);
                        return false;
                    }

                    int estado = rs.getInt("id_estado_usuario");
                    if (estado != 1) {
                        if (DEBUG) System.out.println("⚠️ Usuario con estado no activo: " + estado);
                        return false;
                    }

                    String hash = rs.getString("contrasena");
                    if (hash == null || hash.isEmpty()) {
                        if (DEBUG) System.out.println("⚠️ contrasena NULL/vacía para: " + email);
                        return false;
                    }

                    // Compatibilidad con PHP ($2y$ → $2a$ para jBCrypt)
                    String normalized = hash.replaceFirst("^\\$2y\\$", "\\$2a\\$");

                    boolean ok;
                    if (normalized.startsWith("$2a$") || normalized.startsWith("$2b$")) {
                        ok = BCrypt.checkpw(passwordPlano, normalized);
                        if (DEBUG) System.out.println("🔐 bcrypt check = " + ok);
                    } else {
                        // Por si en algún dataset viejo queda texto plano
                        ok = passwordPlano.equals(hash);
                        if (DEBUG) System.out.println("🪪 fallback texto plano = " + ok);
                    }

                    return ok;
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ SQL Error: " + e.getMessage());
            return false;
        }
    }

    // (Opcional) pequeño main para probar rápido desde consola:
    public static void main(String[] args) {
        String email = "alanbissio@gmail.com";
        String pass  = "123456";
        boolean ok = verificarLogin(email, pass);
        System.out.println("Login de prueba para " + email + " = " + ok);
    }
}
