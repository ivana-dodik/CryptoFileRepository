package org.etf.unibl.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.x509.util.StreamParsingException;
import org.etf.unibl.App;
import org.etf.unibl.domain.User;
import org.etf.unibl.dto.Credentials;
import org.etf.unibl.exception.InvalidCertificateException;
import org.etf.unibl.repo.UserRepositoryImpl;
import org.etf.unibl.repo.UsersRepository;
import org.etf.unibl.service.CRLService;
import org.etf.unibl.service.CRLServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.X509CRL;
import java.security.interfaces.RSAPrivateKey;
import java.util.Optional;


public class LoginCredentialsController {

   private static final Logger LOGGER = LoggerFactory.getLogger(LoginCredentialsController.class);

   private final UsersRepository usersRepository;
   private final CRLService crlService = new CRLServiceImpl();
   private int loginAttempts;
   @FXML
   private TextField usernameField;
   @FXML
   private PasswordField passwordField;

   public LoginCredentialsController() {
      this.usersRepository = new UserRepositoryImpl();
   }

   private RSAPrivateKey readPrivateKey(File file) throws IOException {
      try (FileReader keyReader = new FileReader(file)) {

         PEMParser pemParser = new PEMParser(keyReader);
         JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
         PrivateKeyInfo privateKeyInfo = PrivateKeyInfo.getInstance(pemParser.readObject());

         return (RSAPrivateKey) converter.getPrivateKey(privateKeyInfo);
      }
   }

   @FXML
   private void switchToWelcome() throws IOException {
      App.setRoot("welcome");
   }

   private void switchToMain(User currentUser) throws IOException {
      App.setCurrentUser(currentUser);

      RSAPrivateKey currentUsersPrivateKey = readPrivateKey(
            new File(
                  "certificates/private/"
                        + currentUser.getUsername()
                        + "-key.pem"
            ));
      App.setCurrentUsersPrivateKey(currentUsersPrivateKey);

      App.setRoot("main");
   }

   private void reactivateAccount(User currentUser) {
      currentUser.setActive(true);
      usersRepository.updateIsActive(true, currentUser.getUsername());
      LOGGER.info("Congratulations your account has been reactivated");
      try {
         crlService.removeCertificateFromCrl(App.getCurrentUsersCert());
      } catch (Exception e) {
         LOGGER.error(e.getMessage());
         LOGGER.error("Could not reactivate the certificate, cannot be removed from CRL.");
      }
   }

   private void deactivateAccount(ActionEvent actionEvent) {
      try {
         X509CRL crl = crlService.revokeCertificate(App.getCurrentUsersCert().getSerialNumber());
         try {
            crlService.saveCRL(crl);

            usersRepository.updateIsActive(false, usernameField.getText());

            showAlert("Your account has been deactivated and your certificate has been revoked\n"
                        + "you can reactivate it by entering the correct password or register a new account.",
                  Alert.AlertType.WARNING,
                  actionEvent);
         } catch (Exception e) {
            LOGGER.error(e.getMessage());
            LOGGER.error("Couldn't save crl to disk!");
         }
      } catch (StreamParsingException | GeneralSecurityException | OperatorCreationException | IOException e) {
         LOGGER.error(e.getMessage());
         LOGGER.error("Couldn't revoke certificate!");
      }
   }

   public void login(ActionEvent actionEvent) {
      var credentials = new Credentials(usernameField.getText(), passwordField.getText());
      if (!App.getCurrentUsersCert().getSubjectX500Principal().getName().contains(credentials.getUsername())) {
         LOGGER.warn("The given certificate doesn't belong to this user!!!");
         return;
      }

      try {
         Optional<User> user = usersRepository.login(credentials);
         if (user.isPresent()) {
            var currentUser = user.get();

            if (!currentUser.isActive()) {
               reactivateAccount(currentUser);
            }

            switchToMain(currentUser);
         } else {
            loginAttempts++;
            LOGGER.warn("Login attempts: " + loginAttempts);
            if (loginAttempts == 3) {
               deactivateAccount(actionEvent);
            } else {
               showAlert("Bad credentials", Alert.AlertType.WARNING, actionEvent);
            }
         }
      } catch (InvalidCertificateException e) {
         LOGGER.error(e.getMessage());
         showAlert(e.getMessage(), Alert.AlertType.ERROR, actionEvent);
      } catch (Exception e) {
         LOGGER.error(e.getMessage());
      }
   }
   /*public void login(ActionEvent actionEvent) {
      var credentials = new Credentials(usernameField.getText(), passwordField.getText());
      try {
         Optional<User> user = usersRepository.login(credentials);
         if (user.isPresent()) {
            var currentUser = user.get();

            if (!currentUser.isActive()) {
               reactivateAccount(currentUser);
            }

            switchToMain(currentUser);
         } else {
            loginAttempts++;
            LOGGER.warn("Login attempts: " + loginAttempts);
            if (loginAttempts == 3) {
               deactivateAccount(actionEvent);
            } else {
               showAlert("Bad credentials", Alert.AlertType.WARNING, actionEvent);
            }
         }
      } catch (InvalidCertificateException e) {
         LOGGER.error(e.getMessage());
         showAlert(e.getMessage(), Alert.AlertType.ERROR, actionEvent);
      } catch (Exception e) {
         LOGGER.error(e.getMessage());
      }
   }*/

   private void showAlert(String contentText, Alert.AlertType alertType, ActionEvent actionEvent) {
      var alert = new Alert(alertType);
      alert.setContentText(contentText);

      // To show alert on-top
      alert.initModality(Modality.APPLICATION_MODAL);
      var owner = ((Node) actionEvent.getTarget()).getScene().getWindow();
      alert.initOwner(owner);

      alert.show();
   }
}