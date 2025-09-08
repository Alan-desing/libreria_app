import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.mindrot.jbcrypt.BCrypt;

public class conexion_bd {

    // Lógica: configuración de conexión a MySQL (BD ACTUAL: libreria)
    private static final String URL  =
        "jdbc:mysql://localhost:3306/libreria?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    // Lógica: flag para imprimir mensajes de depuración en consola
    private static final boolean DEBUG = true;

    // Lógica: crear conexión simple (sin pool)
    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            System.err.println("❌ Error de conexión: " + e.getMessage());
            return null;
        }
    }

    /**
     * Lógica: verificación de login compatible con PHP password_hash/password_verify (bcrypt $2y$).
     * - Usa la columna 'contrasena' (hash bcrypt).
     * - Normaliza $2y -> $2a porque jBCrypt trabaja con $2a/$2b.
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

            // Lógica: busca el usuario por email
            try (PreparedStatement ps = cn.prepareStatement(sql)) {
                ps.setString(1, email.trim());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        if (DEBUG) System.out.println("⚠️ No existe el email en BD: " + email);
                        return false;
                    }

                    // Lógica: estado de usuario (1 = activo)
                    int estado = rs.getInt("id_estado_usuario");
                    if (estado != 1) {
                        if (DEBUG) System.out.println("⚠️ Usuario con estado no activo: " + estado);
                        return false;
                    }

                    // Lógica: obtención del hash y normalización $2y -> $2a
                    String hash = rs.getString("contrasena");
                    if (hash == null || hash.isEmpty()) {
                        if (DEBUG) System.out.println("⚠️ contrasena NULL/vacía para: " + email);
                        return false;
                    }
                    String normalized = hash.replaceFirst("^\\$2y\\$", "\\$2a\\$");

                    // Lógica: verificación con jBCrypt o fallback
                    boolean ok;
                    if (normalized.startsWith("$2a$") || normalized.startsWith("$2b$")) {
                        ok = BCrypt.checkpw(passwordPlano, normalized);
                        if (DEBUG) System.out.println("🔐 bcrypt check = " + ok);
                    } else {
                        // Lógica: fallback extremo por si quedan datos viejos en texto plano
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

    
}
