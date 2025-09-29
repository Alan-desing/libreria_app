package admin.sucursales;

import includes.estilos;
import includes.conexion_bd;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.*;

public class ver extends JDialog {
    private final int id;
    private final boolean editModeInitial; // si arranca en modo edición
    private boolean cambios=false;

    private JTextField tfNombre, tfEmail, tfDireccion, tfTelefono;
    private JButton btnGuardar, btnCancelar, btnEditar;

    public ver(Window owner, int idSucursal, boolean startEditing){
        super(owner, (idSucursal<=0?"Nueva Sucursal":"Sucursal #"+idSucursal), ModalityType.APPLICATION_MODAL);
        this.id = idSucursal;
        this.editModeInitial = (id<=0) || startEditing;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(new EmptyBorder(14,14,14,14));
        root.setBackground(estilos.COLOR_FONDO);

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

        tfNombre    = field();
        tfEmail     = field();
        tfDireccion = field();
        tfTelefono  = field();

        addRow(card, g, "Nombre *", tfNombre);
        addRow(card, g, "Email", tfEmail);
        addRow(card, g, "Dirección", tfDireccion);
        addRow(card, g, "Teléfono", tfTelefono);

        JScrollPane sc = new JScrollPane(card,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sc.setBorder(null);

        JPanel actionsLeft = new JPanel(new FlowLayout(FlowLayout.LEFT,10,0));
        btnEditar = estilos.botonBlanco("Editar");
        actionsLeft.add(btnEditar);
        btnEditar.setVisible(id>0 && !editModeInitial);

        JPanel actionsRight = new JPanel(new FlowLayout(FlowLayout.RIGHT,10,0));
        btnGuardar = estilos.botonRedondeado(id<=0?"Crear":"Guardar cambios");
        btnCancelar= estilos.botonBlanco("Cancelar");
        btnGuardar.setPreferredSize(new Dimension(180,38));
        btnCancelar.setPreferredSize(new Dimension(140,38));
        actionsRight.add(btnGuardar); actionsRight.add(btnCancelar);

        JPanel actions = new JPanel(new BorderLayout());
        actions.setOpaque(false);
        actions.add(actionsLeft, BorderLayout.WEST);
        actions.add(actionsRight, BorderLayout.EAST);

        root.add(sc, BorderLayout.CENTER);
        root.add(actions, BorderLayout.SOUTH);
        setContentPane(root);

        setMinimumSize(new Dimension(820, 520));
        pack();
        setLocationRelativeTo(owner);

        btnGuardar.addActionListener(e -> onSave());
        btnCancelar.addActionListener(e -> dispose());
        btnEditar.addActionListener(e -> setEditing(true));

        if (id>0) cargar();
        setEditing(editModeInitial);
        if (editModeInitial) getRootPane().setDefaultButton(btnGuardar);
    }

    private void setEditing(boolean on){
        tfNombre.setEditable(on);
        tfEmail.setEditable(on);
        tfDireccion.setEditable(on);
        tfTelefono.setEditable(on);

        Color bg = on? Color.WHITE : new Color(248,248,248);
        tfNombre.setBackground(bg); tfEmail.setBackground(bg);
        tfDireccion.setBackground(bg); tfTelefono.setBackground(bg);

        btnEditar.setVisible(!on && id>0);
        btnGuardar.setEnabled(on);
    }

    private JTextField field(){
        JTextField t = new JTextField();
        estilos.estilizarCampo(t);
        t.setPreferredSize(new Dimension(520,38));
        t.setFont(t.getFont().deriveFont(15f));
        return t;
    }
    private void addRow(JPanel p, GridBagConstraints g, String label, JComponent comp){
        JPanel row = new JPanel(new BorderLayout()); row.setOpaque(false);
        JLabel lb = new JLabel(label);
        lb.setForeground(new Color(60,60,60));
        lb.setBorder(new EmptyBorder(0,2,4,2));
        row.add(lb, BorderLayout.NORTH);
        row.add(comp, BorderLayout.CENTER);
        p.add(row, g); g.gridy++;
    }

    private void cargar(){
        try (Connection cn = conexion_bd.getConnection();
             PreparedStatement ps = cn.prepareStatement("SELECT * FROM sucursal WHERE id_sucursal=?")){
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()){
                if (!rs.next()){ dispose(); return; }
                tfNombre.setText(rs.getString("nombre"));
                tfEmail.setText(rs.getString("email"));
                tfDireccion.setText(rs.getString("direccion"));
                tfTelefono.setText(rs.getString("telefono"));
            }
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this,
                    "Error cargando:\n"+ex.getMessage(),
                    "BD", JOptionPane.ERROR_MESSAGE);
            dispose();
        }
    }

    private void onSave(){
        String nombre = tfNombre.getText().trim();
        if (nombre.isEmpty()){
            JOptionPane.showMessageDialog(this, "El nombre es obligatorio.", "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try (Connection cn = conexion_bd.getConnection()){
            if (id<=0){
                try (PreparedStatement ps = cn.prepareStatement(
                        "INSERT INTO sucursal (nombre,direccion,telefono,email,creado_en) VALUES (?,?,?,?,NOW())")){
                    ps.setString(1, nombre);
                    ps.setString(2, nn(tfDireccion.getText()));
                    ps.setString(3, nn(tfTelefono.getText()));
                    ps.setString(4, nn(tfEmail.getText()));
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = cn.prepareStatement(
                        "UPDATE sucursal SET nombre=?, direccion=?, telefono=?, email=? WHERE id_sucursal=?")){
                    ps.setString(1, nombre);
                    ps.setString(2, nn(tfDireccion.getText()));
                    ps.setString(3, nn(tfTelefono.getText()));
                    ps.setString(4, nn(tfEmail.getText()));
                    ps.setInt(5, id);
                    ps.executeUpdate();
                }
            }
            cambios = true;
            dispose();
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this,
                    "Error guardando:\n"+ex.getMessage(),
                    "BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean huboCambios(){ return cambios; }
    private static String nn(String s){ return s==null? "" : s.trim(); }
}
