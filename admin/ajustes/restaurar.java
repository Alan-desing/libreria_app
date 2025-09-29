package admin.ajustes;

import includes.conexion_bd;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.*;

public class restaurar {

    /** Ejecuta un .sql en transacción, deshabilitando FK (igual al restaurar.php). */
    public static void ejecutar(File sqlFile) throws Exception {
        if (sqlFile==null || !sqlFile.exists())
            throw new IllegalArgumentException("Seleccioná un archivo .sql válido.");

        String content = Files.readString(sqlFile.toPath(), StandardCharsets.UTF_8);

        try (Connection cn = conexion_bd.getConnection()){
            cn.setAutoCommit(false);
            try (Statement st = cn.createStatement()){
                st.execute("SET FOREIGN_KEY_CHECKS=0");
                // Split básico por ';' seguido de salto de línea(s)
                for (String stmt : content.split(";[\\r\\n]+")) {
                    String s = stmt.trim();
                    if (s.isEmpty()) continue;
                    st.execute(s);
                }
                st.execute("SET FOREIGN_KEY_CHECKS=1");
            }
            cn.commit();
        } catch (Exception e){
            throw new Exception("Error al restaurar: " + e.getMessage(), e);
        }
    }
}
