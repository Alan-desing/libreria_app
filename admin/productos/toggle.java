package admin.productos;

import includes.conexion_bd;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class toggle {
    public static void ejecutar(Component parent, int idProducto){
        try (Connection cn = conexion_bd.getConnection()){
            // Ver estado actual
            int actual;
            try (PreparedStatement ps = cn.prepareStatement("SELECT activo FROM producto WHERE id_producto=?")){
                ps.setInt(1, idProducto);
                try (ResultSet rs = ps.executeQuery()){
                    if (!rs.next()){
                        JOptionPane.showMessageDialog(parent,"Producto inexistente","BD",JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    actual = rs.getInt(1);
                }
            }

            int nuevo = (actual==1)?0:1;
            try (PreparedStatement up = cn.prepareStatement(
                    "UPDATE producto SET activo=?, actualizado_en=NOW() WHERE id_producto=?")){
                up.setInt(1, nuevo);
                up.setInt(2, idProducto);
                up.executeUpdate();
            }
            JOptionPane.showMessageDialog(parent, nuevo==1 ? "Producto activado." : "Producto desactivado.");
        } catch (Exception ex){
            JOptionPane.showMessageDialog(parent,"Error:\n"+ex.getMessage(),"BD",JOptionPane.ERROR_MESSAGE);
        }
    }
}
