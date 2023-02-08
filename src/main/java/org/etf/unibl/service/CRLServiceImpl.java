package org.etf.unibl.service;

import org.bouncycastle.asn1.x509.CRLReason;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.ExtensionsGenerator;
import org.bouncycastle.cert.X509v2CRLBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CRLConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v2CRLBuilder;
import org.bouncycastle.jce.provider.X509CRLParser;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.x509.util.StreamParsingException;
import org.etf.unibl.App;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509CRL;
import java.security.cert.X509CRLEntry;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Objects;

public class CRLServiceImpl implements CRLService {


   private static final Logger LOGGER = LoggerFactory.getLogger(CRLServiceImpl.class);

   private static final String SIG_ALG = "SHA256WithRSAEncryption";
   private static final String CRL_LOCATION = "certificates/crl/crl.pem";

   /**
    * Calculate a date in seconds (suitable for the PKIX profile - RFC 5280)
    *
    * @param hoursInFuture hours ahead of now, may be negative.
    * @return a Date set to now + (hoursInFuture * 60 * 60) seconds
    */
   public static Date calculateDate(int hoursInFuture) {
      long secs = System.currentTimeMillis() / 1000;

      return new Date((secs + ((long) hoursInFuture * 60 * 60)) * 1000);
   }

   @Override
   public void removeCertificateFromCrl(X509Certificate certificate) throws Exception {
      var crl = fetchCRL();
      KeyPair caKey = App.getCaKey();
      X509Certificate caCert = App.getCaCert();

      X509CRL emptyCrl = createEmptyCRL(caKey.getPrivate(), caCert);
      X509CRLEntry revokedCert = crl.getRevokedCertificate(certificate.getSerialNumber());

      if (revokedCert == null) {
         // The certificate hasn't been revoked to begin with!
         return;
      }

      for (var revocation :
            crl.getRevokedCertificates()) {
         if (!Objects.equals(revocation.getSerialNumber(), certificate.getSerialNumber())) {
            revokeCertificate(emptyCrl, revocation.getSerialNumber());
         }
      }

      saveCRL(emptyCrl);
   }

   public X509CRL revokeCertificate(BigInteger certSerialNumber) throws StreamParsingException, IOException,
         GeneralSecurityException, OperatorCreationException {
      return revokeCertificate(fetchCRL(), certSerialNumber);
   }

   public X509CRL revokeCertificate(
         X509CRL crl,
         BigInteger certSerialNumber)
         throws IOException, GeneralSecurityException, OperatorCreationException {

      PrivateKey caPrivateKey = App.getCaKey().getPrivate();

      X509v2CRLBuilder crlGen = new JcaX509v2CRLBuilder(crl);

      crlGen.setNextUpdate(calculateDate(24 * 7));

      // add revocation
      ExtensionsGenerator extGen = new ExtensionsGenerator();

      CRLReason crlReason = CRLReason.lookup(CRLReason.privilegeWithdrawn);

      extGen.addExtension(Extension.reasonCode, false, crlReason);

      crlGen.addCRLEntry(certSerialNumber,
            new Date(), extGen.generate());

      ContentSigner signer = new JcaContentSignerBuilder(SIG_ALG)
            .setProvider("BC").build(caPrivateKey);

      JcaX509CRLConverter converter = new JcaX509CRLConverter().setProvider("BC");
      return converter.getCRL(crlGen.build(signer));
   }

   private X509CRL fetchCRL() throws StreamParsingException, IOException {
      X509CRLParser parser = new X509CRLParser();
      parser.engineInit(new ByteArrayInputStream(Files.readAllBytes(Paths.get(CRL_LOCATION))));

      return (X509CRL) parser.engineRead();
   }

   private X509CRL createEmptyCRL(
         PrivateKey caKey,
         X509Certificate caCert)
         throws IOException, GeneralSecurityException, OperatorCreationException {
      X509v2CRLBuilder crlGen = new JcaX509v2CRLBuilder(caCert.getSubjectX500Principal(),
            calculateDate(0));

      crlGen.setNextUpdate(calculateDate(24 * 7));

      // add extensions to CRL
      JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();

      crlGen.addExtension(Extension.authorityKeyIdentifier, false,
            extUtils.createAuthorityKeyIdentifier(caCert));

      ContentSigner signer = new JcaContentSignerBuilder(CRLServiceImpl.SIG_ALG)
            .setProvider("BC").build(caKey);

      JcaX509CRLConverter converter = new JcaX509CRLConverter().setProvider("BC");

      return converter.getCRL(crlGen.build(signer));
   }

   @Override
   public void saveCRL(X509CRL crl) throws Exception {
      StringWriter sw = new StringWriter();
      try (JcaPEMWriter jpw = new JcaPEMWriter(sw)) {
         jpw.writeObject(crl);
      } catch (IOException e) {
         LOGGER.error(e.getMessage());
      }
      String pem = sw.toString();
      Path path = Paths.get(CRL_LOCATION);
      byte[] strToBytes = pem.getBytes();
      Files.write(path, strToBytes);
   }
}
