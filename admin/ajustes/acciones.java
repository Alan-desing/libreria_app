package admin.ajustes;

import includes.conexion_bd;

import java.sql.*;
import java.util.Map;

public class acciones {

    /* ====== Opciones / Parámetros ====== */

    public static void saveOpts(Map<String,String> m) throws Exception {
        try (Connection cn = conexion_bd.getConnection()){
            ensureAjusteTable(cn);
            setOpt(cn, "stock_minimo_general", String.valueOf(Math.max(0, parseInt(m.get("stock_minimo_general")))));
            setOpt(cn, "impuesto_iva",         nf2(parseDouble(m.get("impuesto_iva"))));
            setOpt(cn, "moneda_simbolo",       trimToLen(m.get("moneda_simbolo"), 4));
            setOpt(cn, "moneda_codigo",        trimToLen(m.get("moneda_codigo"), 8));
        }
    }

    public static void saveEmpresa(Map<String,String> m) throws Exception {
        try (Connection cn = conexion_bd.getConnection()){
            ensureAjusteTable(cn);
            setOpt(cn,"empresa_nombre",    nz(m.get("empresa_nombre")));
            setOpt(cn,"empresa_razon",     nz(m.get("empresa_razon")));
            setOpt(cn,"empresa_cuit",      nz(m.get("empresa_cuit")));
            setOpt(cn,"empresa_direccion", nz(m.get("empresa_direccion")));
            setOpt(cn,"empresa_telefono",  nz(m.get("empresa_telefono")));
            setOpt(cn,"empresa_email",     nz(m.get("empresa_email")));
            setOpt(cn,"empresa_web",       nz(m.get("empresa_web")));
        }
    }

    /* ====== Estados de pedido ====== */

    public static void addEstado(String nombre) throws Exception {
        try (Connection cn = conexion_bd.getConnection();
             PreparedStatement ps = cn.prepareStatement("INSERT INTO estado_pedido(nombre_estado) VALUES(?)")){
            ps.setString(1, nombre);
            ps.executeUpdate();
        }
    }
    public static void updEstado(int id, String nombre) throws Exception {
        try (Connection cn = conexion_bd.getConnection();
             PreparedStatement ps = cn.prepareStatement("UPDATE estado_pedido SET nombre_estado=? WHERE id_estado_pedido=?")){
            ps.setString(1, nombre);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }
    public static void delEstado(int id) throws Exception {
        try (Connection cn = conexion_bd.getConnection();
             PreparedStatement ps = cn.prepareStatement("DELETE FROM estado_pedido WHERE id_estado_pedido=?")){
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /* ====== Tipos de movimiento ====== */

    public static void addTipo(String nombre) throws Exception {
        try (Connection cn = conexion_bd.getConnection();
             PreparedStatement ps = cn.prepareStatement("INSERT INTO tipo_movimiento(nombre_tipo) VALUES(?)")){
            ps.setString(1, nombre);
            ps.executeUpdate();
        }
    }
    public static void updTipo(int id, String nombre) throws Exception {
        try (Connection cn = conexion_bd.getConnection();
             PreparedStatement ps = cn.prepareStatement("UPDATE tipo_movimiento SET nombre_tipo=? WHERE id_tipo_movimiento=?")){
            ps.setString(1, nombre);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }
    public static void delTipo(int id) throws Exception {
        try (Connection cn = conexion_bd.getConnection();
             PreparedStatement ps = cn.prepareStatement("DELETE FROM tipo_movimiento WHERE id_tipo_movimiento=?")){
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /* ====== Helpers ====== */

    private static void setOpt(Connection cn, String k, String v) throws Exception {
        try (PreparedStatement ps = cn.prepareStatement("""
            INSERT INTO ajuste(clave,valor) VALUES(?,?)
            ON DUPLICATE KEY UPDATE valor=VALUES(valor)
        """)){
            ps.setString(1, k);
            ps.setString(2, v);
            ps.executeUpdate();
        }
    }
    static void ensureAjusteTable(Connection cn) throws Exception {
        try (Statement st = cn.createStatement()){
            st.execute("""
              CREATE TABLE IF NOT EXISTS ajuste(
                clave VARCHAR(64) PRIMARY KEY, valor TEXT NOT NULL,
                actualizado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
              ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
        }
    }

    private static String trimToLen(String s, int n){ if (s==null) return ""; s=s.trim(); return s.length()>n? s.substring(0,n):s; }
    private static String nz(String s){ return s==null? "": s; }
    private static int parseInt(String s){ try { return Integer.parseInt(s.trim()); } catch(Exception e){ return 0; } }
    private static double parseDouble(String s){ try { return Double.parseDouble(s.replace(',','.').trim()); } catch(Exception e){ return 0.0; } }
    private static String nf2(double v){ return String.format(java.util.Locale.US, "%.2f", v); }
}
