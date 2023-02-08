package org.etf.unibl.repo;

import org.etf.unibl.domain.RepoFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FileRepositoryImpl implements FileRepository {

   private static final Logger LOGGER = LoggerFactory.getLogger(FileRepositoryImpl.class);

   private static final String SAVE_FILE_QUERY = "INSERT INTO krz.files\n" +
         "(filename, user_id, filesize)\n" +
         "VALUES(?, ?, ?)";

   private static final String FIND_FILES_BY_USER_ID_QUERY = "SELECT file_id, filename, user_id, filesize\n" +
         "FROM krz.files\n" +
         "WHERE user_id = ?";

   @Override
   public Optional<RepoFile> save(String filename, int userId, int filesize) {
      try (Connection connection = DataSource.getConnection();
           PreparedStatement preparedStatement = connection
                 .prepareStatement(SAVE_FILE_QUERY, Statement.RETURN_GENERATED_KEYS)) {
         preparedStatement.setString(1, filename);
         preparedStatement.setInt(2, userId);
         preparedStatement.setInt(3, filesize);

         int affectedRows = preparedStatement.executeUpdate();

         if (affectedRows == 0) {
            throw new SQLException("Creating a RepoFile failed, no rows affected.");
         }

         try (ResultSet resultSet = preparedStatement.getGeneratedKeys()) {
            if (resultSet.next()) {
               var fileId = resultSet.getInt(1);
               var repoFile = new RepoFile(fileId, filename, userId, filesize);
               return Optional.of(repoFile);
            }
         }
      } catch (SQLException e) {
         LOGGER.error(e.getMessage());
      }
      return Optional.empty();
   }

   @Override
   public List<RepoFile> getAllFilesByUserId(int userId) {

      List<RepoFile> files = new ArrayList<>();

      try (Connection connection = DataSource.getConnection();
           PreparedStatement preparedStatement = connection.prepareStatement(FIND_FILES_BY_USER_ID_QUERY)) {
         preparedStatement.setInt(1, userId);

         try (ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
               var repoFile = new RepoFile(resultSet.getInt(1),
                     resultSet.getString(2),
                     resultSet.getInt(3),
                     resultSet.getInt(4));
               files.add(repoFile);
            }
         }
      } catch (SQLException e) {
         LOGGER.error(e.getMessage());
      }

      return files;
   }
}
