module ni.edu.uam.sistemafacturacionjavafx {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires static lombok;
    requires org.postgresql.jdbc;

    exports ni.edu.uam.facturacion.application;
    opens ni.edu.uam.facturacion.application to javafx.fxml;

    opens ni.edu.uam.facturacion.controller to javafx.fxml;
    exports ni.edu.uam.facturacion.controller;

    opens ni.edu.uam.facturacion.model to javafx.base;
    exports ni.edu.uam.facturacion.model;
}