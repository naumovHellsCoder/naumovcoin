module com.naumov.blockchain {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires java.sql;

    opens com.naumov.blockchain to javafx.fxml;
    exports com.naumov.blockchain;
    exports com.naumov.blockchain.controller;
    opens com.naumov.blockchain.controller to javafx.fxml;
}