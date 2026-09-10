module ni.edu.uam.sistemafacturacionjavafx {
    requires javafx.controls;
    requires javafx.fxml;


    opens ni.edu.uam.sistemafacturacionjavafx to javafx.fxml;
    exports ni.edu.uam.sistemafacturacionjavafx;
}