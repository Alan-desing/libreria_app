package includes;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.mindrot.jbcrypt.BCrypt;

public class conexion_bd {

    // CONFIG REMOTA (Hostinger) 
    private static final String HOST = "srv804.hstgr.io";
    private static final String PORT = "3306";
    private static final String DB   = "u156482620_libreria";
    private static final String USER = "u156482620_Zava";
    private static final String PASS = "Zava4567"; 

    // Si tu servidor exige SSL estricto, poné requireSSL=true.
    // allowPublicKeyRetrieval=true ayuda si el server está con caching de keys.
    private static final String URL = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DB
            + "?useUnicode=true&characterEncoding=UTF-8"
            + "&serverTimezone=America/Argentina/Buenos_Aires"
            + "&useSSL=true&requireSSL=false&autoReconnect=true"
            + "&allowPublicKeyRetrieval=true";

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

    /** Verificar credenciales de usuario (bcrypt o texto plano de fallback). */
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

    //  Test rápido: ejecutalo para verificar conexión y contar productos 
    public static void main(String[] args) {
        try (Connection cn = getConnection()) {
            if (cn == null) throw new SQLException("Conexión nula");

            try (var st = cn.createStatement();
                 var rsNow = st.executeQuery("SELECT NOW()")) {
                rsNow.next();
                System.out.println("✅ Conexión OK. Hora servidor: " + rsNow.getString(1));
            }

            try (var st2 = cn.createStatement();
                 var rsCnt = st2.executeQuery("SELECT COUNT(*) FROM producto")) {
                rsCnt.next();
                System.out.println("📦 Productos en DB: " + rsCnt.getInt(1));
            }
        } catch (Exception e) {
            System.err.println("❌ Error de conexión/test: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
