package admin.ajustes;

import includes.conexion_bd;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.*;

public class restaurar {

    // ejecuta un archivo .sql dentro de una transacción, deshabilitando las claves foráneas temporalmente
    public static void ejecutar(File sqlFile) throws Exception {
        // valida que el archivo exista y sea válido
        if (sqlFile == null || !sqlFile.exists())
            throw new IllegalArgumentException("Seleccioná un archivo .sql válido.");

        // lee el contenido completo del archivo .sql en memoria
        String content = Files.readString(sqlFile.toPath(), StandardCharsets.UTF_8);

        // abre conexión a la base y comienza transacción
        try (Connection cn = conexion_bd.getConnection()) {
            cn.setAutoCommit(false);
            try (Statement st = cn.createStatement()) {
                // desactiva temporalmente las verificaciones de claves foráneas
                st.execute("SET FOREIGN_KEY_CHECKS=0");

                // separa las sentencias SQL por ';' seguido de salto de línea
                for (String stmt : content.split(";[\\r\\n]+")) {
                    String s = stmt.trim();
                    if (s.isEmpty()) continue;  // ignora líneas vacías
                    st.execute(s);              // ejecuta cada sentencia SQL
                }

                // vuelve a activar las verificaciones de claves foráneas
                st.execute("SET FOREIGN_KEY_CHECKS=1");
            }

            // confirma todos los cambios si no hubo errores
            cn.commit();
        } catch (Exception e) {
            // encapsula cualquier error en una excepción con mensaje descriptivo
            throw new Exception("Error al restaurar: " + e.getMessage(), e);
        }
    }
}
