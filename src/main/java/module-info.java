module org.etf.unibl {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.zaxxer.hikari;
    requires java.sql;
    requires org.slf4j;
    requires jbcrypt;
    requires org.bouncycastle.provider;
    requires org.bouncycastle.pkix;
    requires org.bouncycastle.util;

    opens org.etf.unibl to javafx.fxml;
    exports org.etf.unibl;
    exports org.etf.unibl.controller;
    exports org.etf.unibl.domain;
    opens org.etf.unibl.controller to javafx.fxml;
}
