package admin.productos;

import includes.conexion_bd;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class eliminar {
    public static void ejecutar(Component parent, int idProducto, Runnable onSuccess){
        int r=JOptionPane.showConfirmDialog(parent,"¿Eliminar producto #"+idProducto+"?","Eliminar",
                JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE);
        if(r!=JOptionPane.YES_OPTION)return;
        try(Connection cn=conexion_bd.getConnection();
            PreparedStatement ps=cn.prepareStatement("DELETE FROM producto WHERE id_producto=?")){
            ps.setInt(1,idProducto);
            int n=ps.executeUpdate();
            if(n>0){ JOptionPane.showMessageDialog(parent,"Producto eliminado"); if(onSuccess!=null) onSuccess.run();}
            else JOptionPane.showMessageDialog(parent,"No se eliminó (ID inexistente)");
        }catch(Exception ex){ JOptionPane.showMessageDialog(parent,"Error: "+ex.getMessage()); }
    }
}
