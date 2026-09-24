package ni.edu.uam.facturacion;

import javafx.application.Application;
import ni.edu.uam.facturacion.application.FacturacionApplication;
import ni.edu.uam.facturacion.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.SQLException;

public class Launcher {
    public static void main(String[] args) {
         Application.launch(FacturacionApplication.class, args);
    }
}
