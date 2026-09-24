package ni.edu.uam.facturacion.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class Producto {
    private Integer id;
    private String codigo;
    private String nombre;
    private Categoria categoria;
    private BigDecimal precioVenta;
    private int existencia;
    private String rutaImagen;
    private boolean activo;
}