package org.etf.unibl.service;

public interface CertificateService {
   void issueCertificate(String username, int durationInDays) throws Exception;
}
