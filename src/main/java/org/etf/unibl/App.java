package org.etf.unibl;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.etf.unibl.domain.User;
import org.etf.unibl.util.CertificateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.security.KeyPair;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;

/**
 * JavaFX App
 */
public class App extends Application {

   private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

   private static final String CA_CERT_PATH = "certificates/cacert.pem";

   private static final String CA_KEYPAIR_PATH = "certificates/private/cakey.pem";
   private static Scene scene;

   private static User currentUser;

   private static X509Certificate currentUsersCert;
   private static RSAPrivateKey currentUsersPrivateKey;

   private static X509Certificate caCert;
   private static KeyPair caKey;

   static {
      try {
         caCert = CertificateUtil.getCertificate(CA_CERT_PATH);
      } catch (CertificateException | IOException e) {
         LOGGER.error(e.getMessage());
         LOGGER.error("Stopping because we cannot locate root certificate!");
      }

      try {
         caKey = CertificateUtil.getKeyPair(CA_KEYPAIR_PATH);
      } catch (IOException e) {
         LOGGER.error(e.getMessage());
         LOGGER.error("Stopping because we cannot locate root KeyPair!");
      }
   }

   public static void setRoot(String fxml) throws IOException {
      scene.setRoot(loadFXML(fxml));
   }

   public static User getCurrentUser() {
      return currentUser;
   }

   public static void setCurrentUser(User user) {
      currentUser = user;
   }

   private static Parent loadFXML(String fxml) throws IOException {
      FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
      return fxmlLoader.load();
   }

   public static void main(String[] args) {
      launch();
   }

   public static X509Certificate getCurrentUsersCert() {
      return currentUsersCert;
   }

   public static void setCurrentUsersCert(X509Certificate currentUsersCert) {
      App.currentUsersCert = currentUsersCert;
   }

   public static RSAPrivateKey getCurrentUsersPrivateKey() {
      return currentUsersPrivateKey;
   }

   public static void setCurrentUsersPrivateKey(RSAPrivateKey currentUsersPrivateKey) {
      App.currentUsersPrivateKey = currentUsersPrivateKey;
   }

   public static X509Certificate getCaCert() {
      return caCert;
   }

   public static KeyPair getCaKey() {
      return caKey;
   }

   public static File showFileChooser(ActionEvent actionEvent) {
      var fileChooser = new FileChooser();
      fileChooser.setTitle("Upload File");

      var owner = ((Node) actionEvent.getTarget()).getScene().getWindow();
      return fileChooser.showOpenDialog(owner);
   }

   @Override
   public void start(Stage stage) throws IOException {
      scene = new Scene(loadFXML("welcome"), 640, 480);
//      stage.setResizable(false);
      stage.setScene(scene);
      stage.show();
   }
}