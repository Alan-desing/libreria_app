package admin.proveedores;

import includes.estilos;
import includes.conexion_bd;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class crear extends JDialog {
    // visual: campos del formulario
    private JTextField tfNombre, tfContacto, tfEmail, tfTel, tfDir;
    // lógica: marca si se guardó correctamente
    private boolean guardado=false;

    public crear(Window owner){
        super(owner, "Nuevo Proveedor", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // visual: panel raíz
        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(new EmptyBorder(14,14,14,14));
        root.setBackground(estilos.COLOR_FONDO);

        // visual: formulario principal en card blanca
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
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

        // visual: creación de campos
        tfNombre   = field();
        tfContacto = field();
        tfEmail    = field();
        tfTel      = field();
        tfDir      = field();

        // visual: agregar filas al formulario
        addRow(card, g, "Nombre *", tfNombre);
        addRow(card, g, "Contacto / Referente", tfContacto);
        addRow(card, g, "Email", tfEmail);
        addRow(card, g, "Teléfono", tfTel);
        addRow(card, g, "Dirección", tfDir);

        // visual: scroll para evitar cortes en pantallas pequeñas
        JScrollPane sc = new JScrollPane(card,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sc.setBorder(null);

        // visual: botones inferiores
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT,10,0));
        JButton btnOk = estilos.botonRedondeado("Guardar");
        JButton btnCancel = estilos.botonBlanco("Cancelar");
        btnOk.setPreferredSize(new Dimension(160,38));
        btnCancel.setPreferredSize(new Dimension(140,38));
        actions.add(btnOk);
        actions.add(btnCancel);

        root.add(sc, BorderLayout.CENTER);
        root.add(actions, BorderLayout.SOUTH);
        setContentPane(root);

        // visual: configuración inicial de tamaño y posición
        setMinimumSize(new Dimension(780, 520));
        pack();
        setLocationRelativeTo(owner);

        // visual: enter para guardar
        getRootPane().setDefaultButton(btnOk);

        // lógica: acciones de botones
        btnOk.addActionListener(e -> onSave());
        btnCancel.addActionListener(e -> dispose());
    }

    // visual: crea y estiliza un campo de texto
    private JTextField field(){
        JTextField t=new JTextField();
        estilos.estilizarCampo(t);
        t.setFont(t.getFont().deriveFont(15f));
        t.setPreferredSize(new Dimension(520, 38));
        return t;
    }

    // visual: agrega una fila con etiqueta y campo
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

    // lógica + BD: guarda el proveedor
    private void onSave(){
        String nombre=tfNombre.getText().trim();
        if (nombre.isEmpty()){
            JOptionPane.showMessageDialog(this, "El nombre es obligatorio.", "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try (Connection cn = conexion_bd.getConnection();
             PreparedStatement ps = cn.prepareStatement(
                     "INSERT INTO proveedor (nombre,email,telefono,direccion,contacto_referencia) VALUES (?,?,?,?,?)")){
            ps.setString(1, nombre);
            ps.setString(2, tfEmail.getText().trim());
            ps.setString(3, tfTel.getText().trim());
            ps.setString(4, tfDir.getText().trim());
            ps.setString(5, tfContacto.getText().trim());
            ps.executeUpdate();
            guardado=true;
            dispose();
        } catch (Exception ex){
            JOptionPane.showMessageDialog(this, "Error guardando:\n"+ex.getMessage(), "BD", JOptionPane.ERROR_MESSAGE);
        }
    }

    // lógica: devuelve si se guardó correctamente
    public boolean fueGuardado(){ return guardado; }
}
