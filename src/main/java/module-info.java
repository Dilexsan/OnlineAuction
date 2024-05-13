module org.example.onllineauction {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.onllineauction to javafx.fxml;
    exports org.example.onllineauction;
}