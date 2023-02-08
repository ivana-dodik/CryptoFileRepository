package org.etf.unibl.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.stage.FileChooser;
import javafx.util.Pair;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.*;
import org.bouncycastle.cms.jcajce.*;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.OutputEncryptor;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.util.Store;
import org.etf.unibl.App;
import org.etf.unibl.domain.RepoFile;
import org.etf.unibl.domain.User;
import org.etf.unibl.repo.FileRepository;
import org.etf.unibl.repo.FileRepositoryImpl;
import org.etf.unibl.repo.PartRepository;
import org.etf.unibl.repo.PartRepositoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;


public class MainController implements Initializable {

   private static final Logger LOGGER = LoggerFactory.getLogger(MainController.class);

   private static final String ROOT_DIR = "repositories/";
   private static final String SUFFIX = ".splitPart";
   private static final int LOWER_BOUND = 4;
   private static final int UPPER_BOUND = 64;

   private final FileRepository fileRepository = new FileRepositoryImpl();
   private final PartRepository partRepository = new PartRepositoryImpl();

   private final ObservableList<RepoFile> files = FXCollections.observableArrayList();

   @FXML
   private ListView<RepoFile> filesListView;

   /**
    * Split a file into multiples files.
    * Inspired by this <a href="https://www.admios.com/blog/how-to-split-a-file-using-java">article</a>.
    *
    * @param sourceDir The directory of the currently logged-in user.
    * @param filename  Absolute path to the file that will be split.
    * @param numSplits Number of parts for the file to be split into.
    */
   public static List<Pair<Path, byte[]>> splitFile(final String sourceDir, final String filename, final int numSplits, final long sourceSize) throws IOException {

      if (numSplits <= 0) {
         throw new IllegalArgumentException("numSplits must be more than zero");
      }

      List<Pair<Path, byte[]>> partFiles = new ArrayList<>();

      // If the chosen file has fewer bytes than parts, we truncate the number of parts
      final int splits = Math.min((int) sourceSize, numSplits);

      final long bytesPerSplit = sourceSize / splits;
      final long remainingBytes = sourceSize % splits;
      int position = 0;

      try (RandomAccessFile sourceFile = new RandomAccessFile(filename, "r");
           FileChannel sourceChannel = sourceFile.getChannel()) {

         for (; position < splits; position++) {
            //write multipart files.
            partFiles.add(writePartToFile(sourceDir, bytesPerSplit, position * bytesPerSplit, sourceChannel));
         }

         if (remainingBytes > 0) {
            partFiles.add(writePartToFile(sourceDir, remainingBytes, position * bytesPerSplit, sourceChannel));
         }
      }
      return partFiles;
   }

   private static Pair<Path, byte[]> writePartToFile(final String sourceDir, long byteSize, long position, FileChannel sourceChannel) throws IOException {

      Path partDir = Paths.get(sourceDir + UUID.randomUUID());
      Files.createDirectories(partDir);
      Path fileName = Paths.get(partDir + "/file" + SUFFIX);
      try (RandomAccessFile toFile = new RandomAccessFile(fileName.toFile(), "rw");
           FileChannel toChannel = toFile.getChannel()) {
         sourceChannel.position(position);
         toChannel.transferFrom(sourceChannel, 0, byteSize);

         byte[] rawBytes = Files.readAllBytes(fileName);
         byte[] encrypted = encryptData(rawBytes, App.getCurrentUsersCert());
         Files.write(fileName, encrypted);

         byte[] signature = signData(encrypted, App.getCurrentUsersCert(), App.getCurrentUsersPrivateKey());
         return new Pair<>(partDir, signature);
      } catch (CertificateEncodingException | OperatorCreationException | CMSException e) {
         LOGGER.error(e.getMessage());
      }

      return null;
   }

   public static byte[] encryptData(byte[] data,
                                    X509Certificate encryptionCertificate)
         throws CertificateEncodingException, CMSException, IOException {
      byte[] encryptedData = null;
      if (null != data && null != encryptionCertificate) {
         CMSEnvelopedDataGenerator cmsEnvelopedDataGenerator = new CMSEnvelopedDataGenerator();
         JceKeyTransRecipientInfoGenerator jceKey = new JceKeyTransRecipientInfoGenerator(encryptionCertificate);
         cmsEnvelopedDataGenerator.addRecipientInfoGenerator(jceKey);
         CMSTypedData msg = new CMSProcessableByteArray(data);
         OutputEncryptor encryptor = new JceCMSContentEncryptorBuilder(CMSAlgorithm.AES128_CBC).setProvider("BC").build();
         CMSEnvelopedData cmsEnvelopedData = cmsEnvelopedDataGenerator.generate(msg, encryptor);
         encryptedData = cmsEnvelopedData.getEncoded();
      }
      return encryptedData;
   }

   public static byte[] decryptData(final byte[] encryptedData, final PrivateKey decryptionKey) throws CMSException {
      byte[] decryptedData = null;
      if (null != encryptedData && null != decryptionKey) {
         CMSEnvelopedData envelopedData = new CMSEnvelopedData(encryptedData);
         Collection<RecipientInformation> recip = envelopedData.getRecipientInfos().getRecipients();
         KeyTransRecipientInformation recipientInfo = (KeyTransRecipientInformation) recip.iterator().next();
         JceKeyTransRecipient recipient = new JceKeyTransEnvelopedRecipient(decryptionKey);
         decryptedData = recipientInfo.getContent(recipient);
      }
      return decryptedData;
   }

   public static byte[] signData(byte[] data, final X509Certificate signingCertificate, final PrivateKey signingKey) throws CertificateEncodingException, CMSException, IOException, OperatorCreationException {
      byte[] signedMessage;
      List<X509Certificate> certList = new ArrayList<>();
      CMSTypedData cmsData = new CMSProcessableByteArray(data);
      certList.add(signingCertificate);
      Store certs = new JcaCertStore(certList);
      CMSSignedDataGenerator cmsGenerator = new CMSSignedDataGenerator();
      ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256withRSA").build(signingKey);
      cmsGenerator.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder().setProvider("BC").build()).build(contentSigner, signingCertificate));
      cmsGenerator.addCertificates(certs);
      CMSSignedData cms = cmsGenerator.generate(cmsData, true);
      signedMessage = cms.getEncoded();
      return signedMessage;
   }

   public static boolean verifySignedData(final byte[] signedData) throws CMSException, IOException, OperatorCreationException, CertificateException {
      ByteArrayInputStream bIn = new ByteArrayInputStream(signedData);
      ASN1InputStream aIn = new ASN1InputStream(bIn);
      CMSSignedData s = new CMSSignedData(ContentInfo.getInstance(aIn.readObject()));
      aIn.close();
      bIn.close();
      Store certs = s.getCertificates();
      SignerInformationStore signers = s.getSignerInfos();
      Collection<SignerInformation> c = signers.getSigners();
      SignerInformation signer = c.iterator().next();
      Collection<X509CertificateHolder> certCollection = certs.getMatches(signer.getSID());
      Iterator<X509CertificateHolder> certIt = certCollection.iterator();
      X509CertificateHolder certHolder = certIt.next();
      return signer.verify(new JcaSimpleSignerInfoVerifierBuilder().build(certHolder));
   }

   @Override
   public void initialize(URL location, ResourceBundle resources) {
      files.addAll(fileRepository.getAllFilesByUserId(App.getCurrentUser().getUserId()));
      filesListView.setItems(files);
   }

   @FXML
   private void logout() throws IOException {
      // TODO: Add logout specific logic
      App.setCurrentUser(null);
      App.setRoot("welcome");
   }

   @FXML
   private void uploadFile(ActionEvent actionEvent) throws IOException {

      File uploadedFile = App.showFileChooser(actionEvent);
      String filename = uploadedFile.getName();
      User currentUser = App.getCurrentUser();

      String currentUserDir = ROOT_DIR + currentUser.getUsername();
      String fileDir = currentUserDir + "/" + filename + "/";

      Files.createDirectories(Paths.get(fileDir));

      int numberOfSplits = ThreadLocalRandom.current().nextInt(LOWER_BOUND, UPPER_BOUND);

      final long sourceSize = Files.size(Paths.get(uploadedFile.getAbsolutePath()));

      List<Pair<Path, byte[]>> pathsToPartsWithSigns = splitFile(fileDir, uploadedFile.getAbsolutePath(), numberOfSplits, sourceSize);

      Optional<RepoFile> repoFile = fileRepository.save(filename, currentUser.getUserId(), (int) sourceSize);
      repoFile.ifPresent(file -> {
         partRepository.saveAll(pathsToPartsWithSigns, file.getFileId());
         files.add(file);
      });
   }

   public void downloadFile(ActionEvent actionEvent) throws IOException, CMSException, CertificateException,
         OperatorCreationException {
      RepoFile repoFile = filesListView.getSelectionModel().getSelectedItem();
      List<String> paths = partRepository.getDirectoryPathsOrderedByIndex(repoFile.getFileId());

      byte[] allBytes = new byte[repoFile.getFilesize()];
      ByteBuffer byteBuffer = ByteBuffer.wrap(allBytes);
      for (var path :
            paths) {
         byte[] encryptedBytes = Files.readAllBytes(Paths.get(path + "/file.splitPart"));
         byte[] signed = signData(encryptedBytes, App.getCurrentUsersCert(), App.getCurrentUsersPrivateKey());
         if (!verifySignedData(signed)) {
            LOGGER.warn("WARNING: File was tempered with!");
         }
         byte[] rawBytes = decryptData(encryptedBytes, App.getCurrentUsersPrivateKey());
         byteBuffer.put(rawBytes);
      }

      var fileChooser = new FileChooser();
      fileChooser.setTitle("Download File");
      fileChooser.setInitialFileName(repoFile.getFilename());

      var owner = ((Node) actionEvent.getTarget()).getScene().getWindow();
      File file = fileChooser.showSaveDialog(owner);

      Files.write(file.toPath(), allBytes);
   }
}
