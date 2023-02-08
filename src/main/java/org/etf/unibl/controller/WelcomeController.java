package org.etf.unibl.controller;

import javafx.fxml.FXML;
import org.etf.unibl.App;

import java.io.IOException;

public class WelcomeController {

   @FXML
   private void switchToLogin() throws IOException {
      App.setRoot("loginCert");
   }

   public void switchToRegister() throws IOException {
      App.setRoot("register");
   }
}
