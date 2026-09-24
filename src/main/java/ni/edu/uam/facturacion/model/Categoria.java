package ni.edu.uam.facturacion.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Categoria {
    private Integer id;
    private String nombre;
    private boolean activa;

    @Override
    public String toString() {
        return nombre;   // lo que muestra el ComboBox
    }
}