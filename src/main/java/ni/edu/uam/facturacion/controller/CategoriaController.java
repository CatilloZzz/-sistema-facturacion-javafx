package ni.edu.uam.facturacion.controller;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import ni.edu.uam.facturacion.dao.CategoriaDAO;
import ni.edu.uam.facturacion.model.Categoria;

import java.sql.SQLException;

public class CategoriaController {

    @FXML private TextField txtNombre;
    @FXML private CheckBox chkActiva;
    @FXML private TableView<Categoria> tblCategorias;
    @FXML private TableColumn<Categoria, Integer> colId;
    @FXML private TableColumn<Categoria, String> colNombre, colActiva;

    private final CategoriaDAO dao = new CategoriaDAO();
    private final ObservableList<Categoria> categorias = FXCollections.observableArrayList();
    private Categoria seleccionada;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getId()));
        colNombre.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombre()));
        colActiva.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().isActiva() ? "Sí" : "No"));
        tblCategorias.setItems(categorias);

        tblCategorias.getSelectionModel().selectedItemProperty().addListener((obs, ant, sel) -> {
            if (sel != null) {
                seleccionada = sel;
                txtNombre.setText(sel.getNombre());
                chkActiva.setSelected(sel.isActiva());
            }
        });
        cargar();
    }

    @FXML
    private void guardar() {
        if (txtNombre.getText().isBlank()) {
            mensaje(Alert.AlertType.WARNING, "El nombre es obligatorio.");
            return;
        }
        try {
            dao.guardar(new Categoria(null, txtNombre.getText().trim(), chkActiva.isSelected()));
            cargar();
            limpiar();
            mensaje(Alert.AlertType.INFORMATION, "Categoría guardada.");
        } catch (SQLException e) {
            mensaje(Alert.AlertType.ERROR, "No se pudo guardar: " + e.getMessage());
        }
    }

    @FXML
    private void actualizar() {
        if (seleccionada == null) {
            mensaje(Alert.AlertType.WARNING, "Seleccione una categoría de la tabla.");
            return;
        }
        if (txtNombre.getText().isBlank()) {
            mensaje(Alert.AlertType.WARNING, "El nombre es obligatorio.");
            return;
        }
        try {
            dao.actualizar(new Categoria(seleccionada.getId(),
                    txtNombre.getText().trim(), chkActiva.isSelected()));
            cargar();
            limpiar();
            mensaje(Alert.AlertType.INFORMATION, "Categoría actualizada.");
        } catch (SQLException e) {
            mensaje(Alert.AlertType.ERROR, "No se pudo actualizar: " + e.getMessage());
        }
    }

    @FXML
    private void eliminar() {
        if (seleccionada == null) {
            mensaje(Alert.AlertType.WARNING, "Seleccione una categoría de la tabla.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar la categoría \"" + seleccionada.getNombre() + "\"?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        try {
            dao.eliminar(seleccionada.getId());
            cargar();
            limpiar();
        } catch (SQLException e) {
            mensaje(Alert.AlertType.ERROR,
                    "No se pudo eliminar (¿tiene productos asociados?): " + e.getMessage());
        }
    }

    @FXML
    private void limpiar() {
        seleccionada = null;
        txtNombre.clear();
        chkActiva.setSelected(true);
        tblCategorias.getSelectionModel().clearSelection();
    }

    @FXML
    private void cerrar() {
        ((Stage) txtNombre.getScene().getWindow()).close();
    }

    private void cargar() {
        try {
            categorias.setAll(dao.listar());
        } catch (SQLException e) {
            mensaje(Alert.AlertType.ERROR, "Error al cargar categorías: " + e.getMessage());
        }
    }

    private void mensaje(Alert.AlertType tipo, String texto) {
        Alert a = new Alert(tipo, texto);
        a.setHeaderText(null);
        a.showAndWait();
    }
}