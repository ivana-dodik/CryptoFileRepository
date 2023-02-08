package org.etf.unibl.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.etf.unibl.App;
import org.etf.unibl.util.CertificateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class LoginCertificateController {
   private static final Logger LOGGER = LoggerFactory.getLogger(LoginCertificateController.class);

   private static final String BC_PROVIDER = "BC";
   public Button nextButton;
   public Label chosenFileLabel;

   @FXML
   private void switchToLoginCredentials() throws IOException {
      App.setRoot("loginCredentials");
   }

   @FXML
   private void switchToWelcome() throws IOException {
      App.setRoot("welcome");
   }

   @FXML
   private void uploadCert(ActionEvent actionEvent) throws CertificateException, IOException {
      Path certPath = App.showFileChooser(actionEvent).toPath();

      PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**.cer");

      if (matcher.matches(certPath)) {
         Security.addProvider(new BouncyCastleProvider());

         X509Certificate rootCert = App.getCaCert();
         X509Certificate userCert = CertificateUtil.getCertificate(certPath.toString());

         try {
            userCert.verify(rootCert.getPublicKey(), BC_PROVIDER);
         } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException | NoSuchProviderException e) {
            LOGGER.warn("Certificate couldn't be verified! You cannot login.");
            return;
         }

         nextButton.setDisable(false);
         chosenFileLabel.setText(certPath.getFileName().toString());
         App.setCurrentUsersCert(userCert);
      } else {
         LOGGER.warn("The selected file isn't a certificate!");
      }
   }
}
