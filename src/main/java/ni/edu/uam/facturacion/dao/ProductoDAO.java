package ni.edu.uam.facturacion.dao;

import ni.edu.uam.facturacion.model.Categoria;
import ni.edu.uam.facturacion.model.Producto;
import ni.edu.uam.facturacion.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductoDAO {

    // Consulta base reutilizada por listar() y buscar()
    private static final String SELECT_BASE = """
        SELECT p.id, p.codigo, p.nombre, p.precio_venta, p.existencia,
               p.ruta_imagen, p.activo,
               c.id AS cat_id, c.nombre AS cat_nombre, c.activa AS cat_activa
        FROM producto p
        JOIN categoria c ON c.id = p.categoria_id
        """;


    public void guardar(Producto p) throws SQLException {
        String sql = """
            INSERT INTO producto
            (codigo, nombre, categoria_id, precio_venta, existencia, ruta_imagen, activo)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, p.getCodigo());
            ps.setString(2, p.getNombre());
            ps.setInt(3, p.getCategoria().getId());
            ps.setBigDecimal(4, p.getPrecioVenta());
            ps.setInt(5, p.getExistencia());
            ps.setString(6, p.getRutaImagen());
            ps.setBoolean(7, p.isActivo());
            ps.executeUpdate();
        }
    }

    public List<Producto> listar() throws SQLException {
        List<Producto> lista = new ArrayList<>();
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(SELECT_BASE + " ORDER BY p.id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public Producto buscar(int id) throws SQLException {
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(SELECT_BASE + " WHERE p.id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        }
        return null;
    }

    public void actualizar(Producto p) throws SQLException {
        String sql = """
            UPDATE producto SET codigo = ?, nombre = ?, categoria_id = ?,
                   precio_venta = ?, existencia = ?, ruta_imagen = ?, activo = ?
            WHERE id = ?
            """;
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, p.getCodigo());
            ps.setString(2, p.getNombre());
            ps.setInt(3, p.getCategoria().getId());
            ps.setBigDecimal(4, p.getPrecioVenta());
            ps.setInt(5, p.getExistencia());
            ps.setString(6, p.getRutaImagen());
            ps.setBoolean(7, p.isActivo());
            ps.setInt(8, p.getId());
            ps.executeUpdate();
        }
    }

    public void eliminar(int id) throws SQLException {
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement("DELETE FROM producto WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Producto mapear(ResultSet rs) throws SQLException {
        Producto p = new Producto();
        p.setId(rs.getInt("id"));
        p.setCodigo(rs.getString("codigo"));
        p.setNombre(rs.getString("nombre"));
        p.setPrecioVenta(rs.getBigDecimal("precio_venta"));
        p.setExistencia(rs.getInt("existencia"));
        p.setRutaImagen(rs.getString("ruta_imagen"));
        p.setActivo(rs.getBoolean("activo"));
        p.setCategoria(new Categoria(
                rs.getInt("cat_id"),
                rs.getString("cat_nombre"),
                rs.getBoolean("cat_activa")));
        return p;
    }
}