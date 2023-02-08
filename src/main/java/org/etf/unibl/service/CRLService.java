package org.etf.unibl.service;

import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.x509.util.StreamParsingException;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;

public interface CRLService {
   void removeCertificateFromCrl(X509Certificate certificate) throws Exception;

   X509CRL revokeCertificate(BigInteger certSerialNumber) throws StreamParsingException, IOException, GeneralSecurityException, OperatorCreationException;

   X509CRL revokeCertificate(X509CRL crl, BigInteger certSerialNumber) throws IOException, GeneralSecurityException, OperatorCreationException;

   void saveCRL(X509CRL crl) throws Exception;
}
