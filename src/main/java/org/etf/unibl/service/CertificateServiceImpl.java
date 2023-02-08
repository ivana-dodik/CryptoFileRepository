package org.etf.unibl.service;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.etf.unibl.App;

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;

public class CertificateServiceImpl implements CertificateService {

   private static final String BC_PROVIDER = "BC";
   private static final String KEY_ALGORITHM = "RSA";
   private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
   private static final String CERTS_ROOT = "certificates/certs/";
   private static final String KEYS_ROOT = "certificates/private/";

   private static final String CA_CERT_PATH = "certificates/cacert.pem";

   private static final String CA_KEYPAIR_PATH = "certificates/private/cakey.pem";

   @Override
   public void issueCertificate(String username, int durationInDays) throws Exception {
      Security.addProvider(new BouncyCastleProvider());

      // Initialize a new KeyPair generator
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGORITHM, BC_PROVIDER);
      keyPairGenerator.initialize(2048);

      X509Certificate rootCert = App.getCaCert();
      KeyPair rootKeyPair = App.getCaKey();

      // Generating client request & certificate
      String issuedCertName = "CN=#USERNAME,OU=Elektrotehnicki Fakultet Banja Luka,O=Univerzitet u Banjoj Luci,L=Banja Luka,ST=Republika Srpska,C=BA";
      X500Name issuedCertSubject = new X500Name(issuedCertName.replace("#USERNAME", username));
      BigInteger issuedCertSerialNum = new BigInteger(Long.toString(new SecureRandom().nextLong()));
      KeyPair issuedCertKeyPair = keyPairGenerator.generateKeyPair();

      PKCS10CertificationRequestBuilder p10Builder = new JcaPKCS10CertificationRequestBuilder(issuedCertSubject, issuedCertKeyPair.getPublic());
      JcaContentSignerBuilder csrBuilder = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM).setProvider(BC_PROVIDER);

      ContentSigner csrContentSigner = csrBuilder.build(rootKeyPair.getPrivate());
      PKCS10CertificationRequest csr = p10Builder.build(csrContentSigner);

      Calendar calendar = Calendar.getInstance();
      calendar.add(Calendar.DATE, -1);
      Date startDate = calendar.getTime();

      calendar.add(Calendar.DAY_OF_YEAR, durationInDays);
      Date endDate = calendar.getTime();

      X509v3CertificateBuilder issuedCertBuilder = new X509v3CertificateBuilder(
            new X500Name(rootCert.getIssuerX500Principal().getName()), issuedCertSerialNum, startDate, endDate,
            csr.getSubject(), csr.getSubjectPublicKeyInfo()
      );

      JcaX509ExtensionUtils issuedCertExtUtils = new JcaX509ExtensionUtils();

      // mark certificate as not CA
      issuedCertBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));

      issuedCertBuilder.addExtension(Extension.authorityKeyIdentifier, false,
            issuedCertExtUtils.createAuthorityKeyIdentifier(rootCert));
      issuedCertBuilder.addExtension(Extension.subjectKeyIdentifier, false,
            issuedCertExtUtils.createSubjectKeyIdentifier(csr.getSubjectPublicKeyInfo()));

      issuedCertBuilder.addExtension(Extension.keyUsage, false,
            new KeyUsage(KeyUsage.dataEncipherment |
                  KeyUsage.digitalSignature |
                  KeyUsage.nonRepudiation));

      X509CertificateHolder issuedCertHolder = issuedCertBuilder.build(csrContentSigner);
      X509Certificate issuedCert = new JcaX509CertificateConverter()
            .setProvider(BC_PROVIDER)
            .getCertificate(issuedCertHolder);

      issuedCert.verify(rootCert.getPublicKey(), BC_PROVIDER);
      writeCertToFileBase64Encoded(issuedCert, CERTS_ROOT + username + "-cert.cer");

      writePrivateKeyToFile(issuedCertKeyPair, KEYS_ROOT + username + "-key.pem");
   }

   private void writePrivateKeyToFile(KeyPair keyPair, String filename) {
      try (PemWriter pemWriter = new PemWriter(new FileWriter(filename))) {
         pemWriter.writeObject(new PemObject("PRIVATE KEY", keyPair.getPrivate().getEncoded()));
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   private void writeCertToFileBase64Encoded(Certificate certificate, String fileName) throws Exception {
      StringWriter sw = new StringWriter();
      try (JcaPEMWriter jpw = new JcaPEMWriter(sw)) {
         jpw.writeObject(certificate);
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
      String pem = sw.toString();
      Path path = Paths.get(fileName);
      byte[] strToBytes = pem.getBytes();
      Files.write(path, strToBytes);
   }
}
