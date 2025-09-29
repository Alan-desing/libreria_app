package admin.inventario;

import includes.conexion_bd;
import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class sync {

    private sync(){} // utilidad

    /**
     * Ejecuta la sincronización de inventario como en la web (sync.php):
     * INSERT IGNORE en inventario para todos los productos que no tengan fila.
     * Muestra un mensaje de resultado y retorna true si pudo ejecutar.
     */
    public static boolean ejecutar(Window owner){
        String sql = """
            INSERT IGNORE INTO inventario (id_producto, stock_actual, stock_minimo)
            SELECT p.id_producto, 0, 0
            FROM producto p
            LEFT JOIN inventario i ON i.id_producto = p.id_producto
            WHERE i.id_producto IS NULL
        """;
        try (Connection cn = DB.get();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            int inserted = ps.executeUpdate();
            JOptionPane.showMessageDialog(owner,
                    "Inventario sincronizado.\nFilas creadas: " + inserted,
                    "Sincronización", JOptionPane.INFORMATION_MESSAGE);
            return true;

        } catch (Exception ex){
            JOptionPane.showMessageDialog(owner,
                    "No se pudo sincronizar:\n"+ex.getMessage(),
                    "BD", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

        // BD: helper local unificado
    static class DB {
        static java.sql.Connection get() throws Exception {
            return conexion_bd.getConnection();
        }
    }
}
