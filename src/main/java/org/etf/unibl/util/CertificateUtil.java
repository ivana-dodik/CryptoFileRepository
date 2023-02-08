package org.etf.unibl.util;

import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class CertificateUtil {
   private CertificateUtil() {
   }

   public static X509Certificate getCertificate(String filename) throws CertificateException, IOException {
      CertificateFactory cf = CertificateFactory.getInstance("X.509");
      byte[] caCertBytes = Files.readAllBytes(Paths.get(filename));

      return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(caCertBytes));
   }

   public static KeyPair getKeyPair(String filename) throws IOException {
      return readKeyPair(new File(filename));
   }

   private static KeyPair readKeyPair(File file) throws IOException {
      try (FileReader keyReader = new FileReader(file)) {

         PEMParser pemParser = new PEMParser(keyReader);
         JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
         PEMKeyPair pemKeyPair = (PEMKeyPair) pemParser.readObject();

         return converter.getKeyPair(pemKeyPair);
      }
   }
}
