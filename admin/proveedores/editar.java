package admin.proveedores;

import includes.estilos;
import includes.conexion_bd;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.*;

public class editar extends JDialog {
    // lógica: id del proveedor a editar
    private final int id;
    // visual: campos del formulario
    private JTextField tfNombre, tfContacto, tfEmail, tfTel, tfDir;
    // lógica: indica si hubo cambios guardados
    private boolean cambios=false;

    public editar(Window owner, int idProveedor){
        super(owner, "Editar Proveedor", ModalityType.APPLICATION_MODAL);
        this.id = idProveedor;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // visual: contenedor raíz
        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(new EmptyBorder(14,14,14,14));
        root.setBackground(estilos.COLOR_FONDO);

        // visual: card blanca con formulario
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220,220,220),1,true),
                new EmptyBorder(16,16,16,16)
        ));
        GridBagConstraints g = new GridBagConstraints();
        g.insets=new Insets(8,8,8,8);
        g.fill=GridBagConstraints.HORIZONTAL;
        g.gridx=0; g.gridy=0; g.weightx=1;

        // visual: campos de texto
        tfNombre   = field();
        tfContacto = field();
        tfEmail    = field();
        tfTel      = field();
        tfDir      = field();

        // visual: filas con etiquetas y campos
        addRow(card, g, "Nombre *", tfNombre);
        addRow(card, g, "Contacto / Referente", tfContacto);
        addRow(card, g, "Email", tfEmail);
        addRow(card, g, "Teléfono", tfTel);
        addRow(card, g, "Dirección", tfDir);

        // visual: scroll por si la ventana es pequeña
        JScrollPane sc = new JScrollPane(card,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sc.setBorder(null);

        // visual: botones inferiores
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT,10,0));
        JButton btnOk = estilos.botonRedondeado("Guardar cambios");
        JButton btnCancel = estilos.botonBlanco("Cancelar");
        btnOk.setPreferredSize(new Dimension(180,38));
        btnCancel.setPreferredSize(new Dimension(140,38));
        actions.add(btnOk);
        actions.add(btnCancel);

        root.add(sc, BorderLayout.CENTER);
        root.add(actions, BorderLayout.SOUTH);
        setContentPane(root);

        // visual: tamaño mínimo y posición
        setMinimumSize(new Dimension(820, 540));
        pack();
        setLocationRelativeTo(owner);

        // visual: enter activa el botón guardar
        getRootPane().setDefaultButton(btnOk);

        // lógica: eventos de botones
        btnOk.addActionListener(e -> onSave());
        btnCancel.addActionListener(e -> dispose());

        // BD: carga los datos del proveedor
        cargar();
    }

    // visual: genera y estiliza un campo
    private JTextField field(){
        JTextField t=new JTextField();
        estilos.estilizarCampo(t);
        t.setFont(t.getFont().deriveFont(15f));
        t.setPreferredSize(new Dimension(520, 38));
        return t;
    }

    // visual: agrega fila al formulario
    private void addRow(JPanel p, GridBagConstraints g, String label, JComponent comp){
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        JLabel lb = new JLabel(label);
        lb.setForeground(new Color(60,60,60));
        lb.setBorder(new EmptyBorder(0,2,4,2));
        row.add(lb, BorderLayout.NORTH);
        row.add(comp, BorderLayout.CENTER);
        p.add(row, g);
        g.gridy++;
    }

    // BD: carga los datos existentes del proveedor
    private void cargar(){
        try (Connection cn = conexion_bd.getConnection();
             PreparedStatement ps = cn.prepareStatement("SELECT * FROM proveedor WHERE id_proveedor=?")){
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()){
                if (!rs.next()){ dispose(); return; }
                tfNombre.setText(rs.getString("nombre"));
                tfEmail.setText(rs.getString("email"));
                tfTel.setText(rs.getString("telefono"));
                tfDir.setText(rs.getString("direccion"));
                tfContacto.setText(rs.getString("contacto_referencia"));
            }
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error cargando:\n"+ex.getMessage(), "BD", JOptionPane.ERROR_MESSAGE);
            dispose();
        }
    }

    // lógica + BD: guarda los cambios del proveedor
    private void onSave(){
        String nombre=tfNombre.getText().trim();
        if (nombre.isEmpty()){
            JOptionPane.showMessageDialog(this, "El nombre es obligatorio.", "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try (Connection cn = conexion_bd.getConnection();
             PreparedStatement ps = cn.prepareStatement(
                     "UPDATE proveedor SET nombre=?, email=?, telefono=?, direccion=?, contacto_referencia=? WHERE id_proveedor=?")){
            ps.setString(1, nombre);
            ps.setString(2, tfEmail.getText().trim());
            ps.setString(3, tfTel.getText().trim());
            ps.setString(4, tfDir.getText().trim());
            ps.setString(5, tfContacto.getText().trim());
            ps.setInt(6, id);
            ps.executeUpdate();
            cambios=true;
            dispose();
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error guardando:\n"+ex.getMessage(), "BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    // lógica: devuelve si se guardaron cambios
    public boolean huboCambios(){ return cambios; }
}

