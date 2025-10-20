package admin.alertas;

import includes.estilos;
import includes.conexion_bd;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.*;

/** 
 * Ventana modal para ver el detalle de una alerta específica.
 * Permite marcarla como atendida, reabrirla o eliminarla directamente desde la interfaz.
 */
public class ver extends JDialog {
    private final int idAlerta;
    private boolean cambios=false; // indica si hubo modificaciones

    // etiquetas visuales para mostrar los datos de la alerta
    private JLabel lbTipo, lbProducto, lbCodigo, lbSucursal, lbProveedor, lbEstado, lbCreada, lbAtendida, lbAtendidaPor, lbStock;

    // visual: constructor principal, arma la interfaz y define acciones
    public ver(Window owner, int idAlerta){
        super(owner, "Alerta #"+idAlerta, ModalityType.APPLICATION_MODAL);
        this.idAlerta = idAlerta;

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(new EmptyBorder(14,14,14,14));
        root.setBackground(Color.WHITE);

        // encabezado
        JLabel h = new JLabel("Detalle de alerta");
        h.setFont(new Font("Arial", Font.BOLD, 18));
        h.setAlignmentX(Component.LEFT_ALIGNMENT);
        root.add(h);
        root.add(Box.createVerticalStrut(8));

        // panel con los datos de la alerta en formato clave-valor
        JPanel grid = new JPanel(new GridLayout(4,2,8,8));
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbTipo=new JLabel(); lbProducto=new JLabel(); lbCodigo=new JLabel(); lbSucursal=new JLabel();
        lbProveedor=new JLabel(); lbEstado=new JLabel(); lbCreada=new JLabel(); lbAtendida=new JLabel();
        lbAtendidaPor=new JLabel(); lbStock=new JLabel();

        grid.add(lblPair("Tipo:", lbTipo));
        grid.add(lblPair("Producto:", lbProducto));
        grid.add(lblPair("Código:", lbCodigo));
        grid.add(lblPair("Sucursal:", lbSucursal));
        grid.add(lblPair("Proveedor:", lbProveedor));
        grid.add(lblPair("Estado:", lbEstado));
        grid.add(lblPair("Creada:", lbCreada));
        grid.add(lblPair("Atendida:", lbAtendida));
        root.add(grid);

        root.add(Box.createVerticalStrut(6));
        root.add(lblPair("Atendida por:", lbAtendidaPor));
        root.add(lblPair("Stock:", lbStock));
        root.add(Box.createVerticalStrut(12));

        // visual: barra de botones de acción
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0));
        JButton btnAtender = estilos.botonSm("Marcar atendida");
        JButton btnReabrir = estilos.botonSm("Reabrir");
        JButton btnEliminar = estilos.botonSmDanger("Eliminar");
        JButton btnCerrar = estilos.botonSm("Cerrar");

        actions.add(btnAtender); 
        actions.add(btnReabrir); 
        actions.add(btnEliminar); 
        actions.add(btnCerrar);
        root.add(actions);

        // configuración general del diálogo
        setContentPane(root);
        setSize(640, 360);
        setLocationRelativeTo(owner);

        // eventos de botones
        btnCerrar.addActionListener(e -> dispose());
        btnAtender.addActionListener(e -> { cambiarEstado(true); });
        btnReabrir.addActionListener(e -> { cambiarEstado(false); });
        btnEliminar.addActionListener(e -> { eliminar(); });

        cargar(); // carga los datos iniciales
    }

    // visual: genera un panel con una etiqueta y su valor alineados
    private JPanel lblPair(String k, JLabel v){
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JLabel lk = new JLabel(k); 
        lk.setFont(new Font("Arial", Font.BOLD, 14));
        v.setFont(new Font("Arial", Font.PLAIN, 14));
        p.add(lk, BorderLayout.WEST); 
        p.add(v, BorderLayout.CENTER);
        return p;
    }

    // BD + lógica: carga los datos completos de la alerta desde la base
    private void cargar(){
        String sql = """
            SELECT a.*, ta.nombre_tipo,
                   p.nombre AS producto, p.codigo, pr.nombre AS proveedor,
                   i.id_sucursal, i.stock_actual, i.stock_minimo, s.nombre AS sucursal,
                   u.nombre AS atendido_por
            FROM alerta a
            JOIN tipo_alerta ta ON ta.id_tipo_alerta=a.id_tipo_alerta
            JOIN producto p ON p.id_producto=a.id_producto
            LEFT JOIN proveedor pr ON pr.id_proveedor=p.id_proveedor
            JOIN inventario i ON i.id_inventario=a.id_inventario
            JOIN sucursal s ON s.id_sucursal=i.id_sucursal
            LEFT JOIN usuario u ON u.id_usuario=a.atendida_por
            WHERE a.id_alerta=? LIMIT 1
        """;
        try (Connection cn = conexion_bd.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)){
            ps.setInt(1, idAlerta);
            try (ResultSet r = ps.executeQuery()){
                if (r.next()){
                    lbTipo.setText(nv(r.getString("nombre_tipo")));
                    lbProducto.setText(nv(r.getString("producto")));
                    lbCodigo.setText(nv(r.getString("codigo")));
                    lbSucursal.setText(nv(r.getString("sucursal")));
                    lbProveedor.setText(nv(r.getString("proveedor")));
                    lbEstado.setText(r.getInt("atendida")==1 ? "Atendida" : "Activa");
                    lbCreada.setText(nv(r.getString("fecha_creada")));
                    lbAtendida.setText(nv(r.getString("fecha_atendida")));
                    lbAtendidaPor.setText(nv(r.getString("atendido_por")));
                    lbStock.setText(r.getInt("stock_actual")+" / min "+r.getInt("stock_minimo"));
                }
            }
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error cargando detalle:\n"+ex.getMessage(), "BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    // lógica: devuelve texto o “—” si está vacío o nulo
    private String nv(String s){ return (s==null || s.isEmpty())? "—" : s; }

    // BD + lógica: cambia el estado de la alerta (atender o reabrir)
    private void cambiarEstado(boolean atender){
        String sql = atender
                ? "UPDATE alerta SET atendida=1, fecha_atendida=NOW(), atendida_por=NULL WHERE id_alerta=? AND atendida=0"
                : "UPDATE alerta SET atendida=0, fecha_atendida=NULL, atendida_por=NULL WHERE id_alerta=?";
        try (Connection cn = conexion_bd.getConnection(); 
             PreparedStatement ps = cn.prepareStatement(sql)){
            ps.setInt(1, idAlerta); 
            int n = ps.executeUpdate();
            if (n>0){ 
                cambios=true; 
                JOptionPane.showMessageDialog(this, atender?"Marcada como atendida.":"Reabierta."); 
                cargar(); // refresca los datos
            }
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "No se pudo actualizar:\n"+ex.getMessage(), "BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    // BD + lógica: elimina la alerta actual previa confirmación
    private void eliminar(){
        int ok = JOptionPane.showConfirmDialog(this, "¿Eliminar alerta?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (ok!=JOptionPane.YES_OPTION) return;
        try (Connection cn = conexion_bd.getConnection(); 
             PreparedStatement ps = cn.prepareStatement("DELETE FROM alerta WHERE id_alerta=?")){
            ps.setInt(1, idAlerta); 
            int n = ps.executeUpdate();
            if (n>0){ 
                cambios=true; 
                JOptionPane.showMessageDialog(this, "Eliminada."); 
                dispose(); 
            }
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "No se pudo eliminar:\n"+ex.getMessage(), "BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    // lógica: permite saber si hubo cambios al cerrar el diálogo
    public boolean huboCambios(){ return cambios; }
}
