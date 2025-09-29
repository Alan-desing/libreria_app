package includes;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.mindrot.jbcrypt.BCrypt;

public class conexion_bd {

    // === Configuración local ===
    private static final String URL =
        "jdbc:mysql://localhost:3306/libreria?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8";
    private static final String USER = "root";      // usuario por defecto en XAMPP
    private static final String PASS = "";          // contraseña vacía por defecto en XAMPP

    private static final boolean DEBUG = true;

    /** Conexión simple (sin pool). */
    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(URL, USER, PASS);
        } catch (SQLException e) {
            System.err.println("❌ Error de conexión: " + e.getMessage());
            return null;
        }
    }

    /** Verificar credenciales de usuario (bcrypt o texto plano). */
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

                    // Normaliza $2y$ → $2a$ para compatibilidad con jBCrypt
                    String normalized = hash.replaceFirst("^\\$2y\\$", "\\$2a\\$");

                    boolean ok;
                    if (normalized.startsWith("$2a$") || normalized.startsWith("$2b$")) {
                        ok = BCrypt.checkpw(passwordPlano, normalized);
                        if (DEBUG) System.out.println("🔐 bcrypt check = " + ok);
                    } else {
                        ok = passwordPlano.equals(hash); // fallback texto plano
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
