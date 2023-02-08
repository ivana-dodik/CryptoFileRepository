package org.etf.unibl.controller;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.etf.unibl.App;
import org.etf.unibl.dto.Credentials;
import org.etf.unibl.repo.UserRepositoryImpl;
import org.etf.unibl.repo.UsersRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class RegisterController {


   private static final Logger LOGGER = LoggerFactory.getLogger(RegisterController.class);
   private static final String ROOT_DIR = "repositories/";

   private final UsersRepository usersRepository;
   @FXML
   private TextField usernameField;
   @FXML
   private PasswordField passwordField;

   public RegisterController() {
      this.usersRepository = new UserRepositoryImpl();
   }

   @FXML
   private void switchToWelcome() throws IOException {
      App.setRoot("welcome");
   }

   @FXML
   private void register() throws IOException {
      var credentials = new Credentials(usernameField.getText(), passwordField.getText());

      if (usersRepository.register(credentials)) {
         Files.createDirectory(Paths.get(ROOT_DIR + credentials.getUsername()));

         LOGGER.info("Account created.");
         switchToWelcome();
      }
   }
}
