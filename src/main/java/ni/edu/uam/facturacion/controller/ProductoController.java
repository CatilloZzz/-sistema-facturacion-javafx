package ni.edu.uam.facturacion.controller;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
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

    @FXML private TextField txtCodigo, txtNombre, txtPrecio, txtExistencia;
    @FXML private ComboBox<Categoria> cmbCategoria;
    @FXML private CheckBox chkActivo;
    @FXML private ImageView imgProducto;

    @FXML private TextField txtBuscar;
    @FXML private ComboBox<String> cmbEstado;
    @FXML private ComboBox<Categoria> cmbFiltroCategoria;

    @FXML private TableView<Producto> tblProductos;
    @FXML private TableColumn<Producto, String> colCodigo, colNombre, colCategoria, colPrecio, colActivo;
    @FXML private TableColumn<Producto, Integer> colExistencia;

    private final ProductoDAO productoDAO = new ProductoDAO();
    private final CategoriaDAO categoriaDAO = new CategoriaDAO();

    private final ObservableList<Producto> productos = FXCollections.observableArrayList();
    private FilteredList<Producto> productosFiltrados;

    private static final Categoria TODAS = new Categoria(0, "Todas", true);

    private Producto seleccionado;
    private String rutaImagen;

    @FXML
    public void initialize() {
        colCodigo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCodigo()));
        colNombre.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombre()));
        colCategoria.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategoria().getNombre()));
        colPrecio.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPrecioVenta().toString()));
        colExistencia.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getExistencia()));
        colActivo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().isActivo() ? "Sí" : "No"));

        productosFiltrados = new FilteredList<>(productos, p -> true);
        tblProductos.setItems(productosFiltrados);

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

        cmbEstado.setItems(FXCollections.observableArrayList("Todos", "Activos", "Inactivos"));
        cmbEstado.setValue("Todos");

        cargarCategorias();
        cargarProductos();

        txtBuscar.textProperty().addListener((obs, a, b) -> aplicarFiltros());
        cmbEstado.valueProperty().addListener((obs, a, b) -> aplicarFiltros());
        cmbFiltroCategoria.valueProperty().addListener((obs, a, b) -> aplicarFiltros());
    }

    private void aplicarFiltros() {
        String texto = txtBuscar.getText() == null ? "" : txtBuscar.getText().trim().toLowerCase();
        String estado = cmbEstado.getValue();
        Categoria catFiltro = cmbFiltroCategoria.getValue();

        productosFiltrados.setPredicate(p -> {
            boolean coincideTexto = texto.isEmpty()
                    || p.getCodigo().toLowerCase().contains(texto)
                    || p.getNombre().toLowerCase().contains(texto)
                    || p.getCategoria().getNombre().toLowerCase().contains(texto);

            boolean coincideEstado = estado == null
                    || "Todos".equals(estado)
                    || ("Activos".equals(estado) && p.isActivo())
                    || ("Inactivos".equals(estado) && !p.isActivo());

            boolean coincideCategoria = catFiltro == null
                    || catFiltro == TODAS
                    || p.getCategoria().getId().equals(catFiltro.getId());

            return coincideTexto && coincideEstado && coincideCategoria;
        });
    }

    @FXML
    private void guardar() {
        Producto p = leerFormulario(null);
        if (p == null) return;
        try {
            productoDAO.guardar(p);
            cargarProductos();
            mensaje(Alert.AlertType.INFORMATION, "Producto agregado correctamente.");
            limpiar();
        } catch (SQLException e) {
            mensaje(Alert.AlertType.ERROR, "No se pudo guardar: " + e.getMessage());
        }
    }

    @FXML
    private void actualizar() {
        if (seleccionado == null) {
            mensaje(Alert.AlertType.WARNING, "Seleccione un producto de la tabla.");
            return;
        }
        Producto p = leerFormulario(seleccionado.getId());
        if (p == null) return;
        try {
            p.setId(seleccionado.getId());
            productoDAO.actualizar(p);
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
            productoDAO.eliminar(seleccionado.getId());
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

    private Producto leerFormulario(Integer idExcluido) {
        String codigo = txtCodigo.getText().trim();
        String nombre = txtNombre.getText().trim();

        if (codigo.isEmpty()) {
            mensaje(Alert.AlertType.WARNING, "El código es obligatorio.");
            return null;
        }
        if (nombre.isEmpty()) {
            mensaje(Alert.AlertType.WARNING, "El nombre es obligatorio.");
            return null;
        }
        if (cmbCategoria.getValue() == null) {
            mensaje(Alert.AlertType.WARNING, "Debe seleccionar una categoría.");
            return null;
        }
        if (codigoDuplicado(codigo, idExcluido)) {
            mensaje(Alert.AlertType.ERROR, "Ya existe un producto con el código \"" + codigo + "\".");
            return null;
        }

        BigDecimal precio;
        try {
            precio = new BigDecimal(txtPrecio.getText().trim());
        } catch (NumberFormatException e) {
            mensaje(Alert.AlertType.ERROR, "El precio debe ser numérico.");
            return null;
        }
        if (precio.compareTo(BigDecimal.ZERO) <= 0) {
            mensaje(Alert.AlertType.WARNING, "El precio debe ser mayor que cero.");
            return null;
        }

        int existencia;
        try {
            existencia = Integer.parseInt(txtExistencia.getText().trim());
        } catch (NumberFormatException e) {
            mensaje(Alert.AlertType.ERROR, "La existencia debe ser un número entero.");
            return null;
        }
        if (existencia < 0) {
            mensaje(Alert.AlertType.WARNING, "La existencia no puede ser negativa.");
            return null;
        }

        Producto p = new Producto();
        p.setCodigo(codigo);
        p.setNombre(nombre);
        p.setCategoria(cmbCategoria.getValue());
        p.setPrecioVenta(precio);
        p.setExistencia(existencia);
        p.setRutaImagen(rutaImagen);
        p.setActivo(chkActivo.isSelected());
        return p;
    }

    private boolean codigoDuplicado(String codigo, Integer idExcluido) {
        return productos.stream().anyMatch(p ->
                p.getCodigo().equalsIgnoreCase(codigo) && !p.getId().equals(idExcluido));
    }

    private void cargarCategorias() {
        try {
            ObservableList<Categoria> categorias =
                    FXCollections.observableArrayList(categoriaDAO.listar());
            cmbCategoria.setItems(categorias);

            ObservableList<Categoria> opcionesFiltro = FXCollections.observableArrayList(TODAS);
            opcionesFiltro.addAll(categorias);
            cmbFiltroCategoria.setItems(opcionesFiltro);
            cmbFiltroCategoria.setValue(TODAS);
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