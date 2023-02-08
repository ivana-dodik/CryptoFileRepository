package org.etf.unibl.domain;

public class RepoFile {
   private final int fileId;
   private final String filename;
   private final int userId;
   private final int filesize;

   public RepoFile(int fileId, String filename, int userId, int filesize) {
      this.fileId = fileId;
      this.filename = filename;
      this.userId = userId;
      this.filesize = filesize;
   }

   public int getFileId() {
      return fileId;
   }

   public String getFilename() {
      return filename;
   }

   public int getFilesize() {
      return filesize;
   }

   @Override
   public String toString() {
      return filename;
   }
}
