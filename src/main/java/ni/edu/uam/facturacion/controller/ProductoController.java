package ni.edu.uam.facturacion.controller;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import ni.edu.uam.facturacion.dao.CategoriaDAO;
import ni.edu.uam.facturacion.dao.ProductoDAO;
import ni.edu.uam.facturacion.model.Categoria;
import ni.edu.uam.facturacion.model.Producto;

import java.io.File;
import java.math.BigDecimal;
import java.sql.SQLException;

public class ProductoController {

    // ---------- Componentes del FXML ----------
    @FXML private TextField txtCodigo, txtNombre, txtPrecio, txtExistencia;
    @FXML private ComboBox<Categoria> cmbCategoria;
    @FXML private CheckBox chkActivo;
    @FXML private ImageView imgProducto;

    @FXML private TableView<Producto> tblProductos;
    @FXML private TableColumn<Producto, String> colCodigo, colNombre, colCategoria, colPrecio, colActivo;
    @FXML private TableColumn<Producto, Integer> colExistencia;

    // ---------- Datos ----------
    private final ProductoDAO productoDAO = new ProductoDAO();
    private final CategoriaDAO categoriaDAO = new CategoriaDAO();
    private final ObservableList<Producto> productos = FXCollections.observableArrayList();

    private Producto seleccionado;   // producto elegido en la tabla
    private String rutaImagen;       // ruta de la imagen elegida

    // ---------- Inicio ----------
    @FXML
    public void initialize() {
        // Categoría está anidada, por eso se usan lambdas en vez de PropertyValueFactory
        colCodigo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCodigo()));
        colNombre.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombre()));
        colCategoria.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategoria().getNombre()));
        colPrecio.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPrecioVenta().toString()));
        colExistencia.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getExistencia()));
        colActivo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().isActivo() ? "Sí" : "No"));

        tblProductos.setItems(productos);

        // Al seleccionar una fila, se llena el formulario
        tblProductos.getSelectionModel().selectedItemProperty().addListener((obs, anterior, sel) -> {
            if (sel != null) {
                seleccionado = sel;
                txtCodigo.setText(sel.getCodigo());
                txtNombre.setText(sel.getNombre());
                cmbCategoria.setValue(cmbCategoria.getItems().stream()
                        .filter(c -> c.getId().equals(sel.getCategoria().getId()))
                        .findFirst().orElse(null));
                txtPrecio.setText(sel.getPrecioVenta().toString());
                txtExistencia.setText(String.valueOf(sel.getExistencia()));
                chkActivo.setSelected(sel.isActivo());
                rutaImagen = sel.getRutaImagen();
                mostrarImagen(rutaImagen);
            }
        });

        cargarCategorias();
        cargarProductos();
    }

    // ---------- Acciones ----------
    @FXML
    private void guardar() {
        Producto p = leerFormulario();
        if (p == null) return;
        try {
            productoDAO.guardar(p);                    // INSERT en PostgreSQL
            cargarProductos();
            mensaje(Alert.AlertType.INFORMATION, "Producto agregado correctamente.");
            limpiar();
        } catch (SQLException e) {
            mensaje(Alert.AlertType.ERROR, "No se pudo guardar (¿código repetido?): " + e.getMessage());
        }
    }

    @FXML
    private void actualizar() {
        if (seleccionado == null) {
            mensaje(Alert.AlertType.WARNING, "Seleccione un producto de la tabla.");
            return;
        }
        Producto p = leerFormulario();
        if (p == null) return;
        try {
            p.setId(seleccionado.getId());
            productoDAO.actualizar(p);                 // UPDATE
            cargarProductos();
            mensaje(Alert.AlertType.INFORMATION, "Producto actualizado correctamente.");
            limpiar();
        } catch (SQLException e) {
            mensaje(Alert.AlertType.ERROR, "No se pudo actualizar: " + e.getMessage());
        }
    }

    @FXML
    private void eliminar() {
        if (seleccionado == null) {
            mensaje(Alert.AlertType.WARNING, "Seleccione un producto de la tabla.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar el producto \"" + seleccionado.getNombre() + "\"?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

        try {
            productoDAO.eliminar(seleccionado.getId()); // DELETE
            cargarProductos();
            limpiar();
        } catch (SQLException e) {
            mensaje(Alert.AlertType.ERROR, "No se pudo eliminar: " + e.getMessage());
        }
    }

    @FXML
    private void seleccionarImagen() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Seleccionar imagen");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File archivo = fc.showOpenDialog(txtCodigo.getScene().getWindow());
        if (archivo != null) {
            rutaImagen = archivo.getAbsolutePath();
            mostrarImagen(rutaImagen);
        }
    }

    @FXML
    private void limpiar() {
        seleccionado = null;
        rutaImagen = null;
        txtCodigo.clear();
        txtNombre.clear();
        txtPrecio.clear();
        txtExistencia.clear();
        cmbCategoria.getSelectionModel().clearSelection();
        chkActivo.setSelected(true);
        imgProducto.setImage(null);
        tblProductos.getSelectionModel().clearSelection();
    }

    @FXML
    private void cerrar() {
        ((Stage) txtCodigo.getScene().getWindow()).close();
    }

    // ---------- Métodos auxiliares ----------

    /** Valida el formulario y arma un Producto. Devuelve null si algo está mal. */
    private Producto leerFormulario() {
        if (txtCodigo.getText().isBlank() || txtNombre.getText().isBlank()) {
            mensaje(Alert.AlertType.WARNING, "Código y nombre son obligatorios.");
            return null;
        }
        if (cmbCategoria.getValue() == null) {
            mensaje(Alert.AlertType.WARNING, "Seleccione una categoría.");
            return null;
        }
        try {
            BigDecimal precio = new BigDecimal(txtPrecio.getText().trim());
            int existencia = Integer.parseInt(txtExistencia.getText().trim());

            if (precio.compareTo(BigDecimal.ZERO) <= 0 || existencia < 0) {
                mensaje(Alert.AlertType.WARNING, "Precio mayor que cero y existencia no negativa.");
                return null;
            }

            Producto p = new Producto();
            p.setCodigo(txtCodigo.getText().trim());
            p.setNombre(txtNombre.getText().trim());
            p.setCategoria(cmbCategoria.getValue());
            p.setPrecioVenta(precio);
            p.setExistencia(existencia);
            p.setRutaImagen(rutaImagen);
            p.setActivo(chkActivo.isSelected());
            return p;
        } catch (NumberFormatException e) {
            mensaje(Alert.AlertType.ERROR, "Precio o existencia no válidos.");
            return null;
        }
    }

    private void cargarCategorias() {
        try {
            cmbCategoria.setItems(FXCollections.observableArrayList(categoriaDAO.listar()));
        } catch (SQLException e) {
            mensaje(Alert.AlertType.ERROR, "Error al cargar categorías: " + e.getMessage());
        }
    }

    private void cargarProductos() {
        try {
            productos.setAll(productoDAO.listar());
        } catch (SQLException e) {
            mensaje(Alert.AlertType.ERROR, "Error al cargar productos: " + e.getMessage());
        }
    }

    private void mostrarImagen(String ruta) {
        if (ruta == null || ruta.isBlank()) {
            imgProducto.setImage(null);
            return;
        }
        File f = new File(ruta);
        imgProducto.setImage(f.exists() ? new Image(f.toURI().toString()) : null);
    }

    private void mensaje(Alert.AlertType tipo, String texto) {
        Alert alert = new Alert(tipo, texto);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}