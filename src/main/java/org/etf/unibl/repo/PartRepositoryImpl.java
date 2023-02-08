package org.etf.unibl.repo;

import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PartRepositoryImpl implements PartRepository {

   private static final Logger LOGGER = LoggerFactory.getLogger(PartRepositoryImpl.class);

   private static final String SAVE_PART_QUERY = "INSERT INTO krz.parts\n" +
         "(`index`, file_id, directory_path, signature)\n" +
         "VALUES(?, ?, ?, ?)";

   private static final String FIND_DIRECTORY_PATHS_FOR_FILE_ID_ORDERED_BY_INDEX_QUERY = "SELECT directory_path\n" +
         "FROM krz.parts\n" +
         "WHERE file_id = ?\n" +
         "ORDER BY `index`";

   @Override
   public void saveAll(List<Pair<Path, byte[]>> pathNamesWithSigns, int fileId) {
      try (Connection connection = DataSource.getConnection()) {
         connection.setAutoCommit(false);

         for (int i = 0; i < pathNamesWithSigns.size(); i++) {
            try (PreparedStatement preparedStatement =
                       connection.prepareStatement(SAVE_PART_QUERY, Statement.RETURN_GENERATED_KEYS)) {
               preparedStatement.setInt(1, i + 1);
               preparedStatement.setInt(2, fileId);
               preparedStatement.setString(3, pathNamesWithSigns.get(i).getKey().toString());
               preparedStatement.setBytes(4, pathNamesWithSigns.get(i).getValue());

               int affectedRows = preparedStatement.executeUpdate();

               if (affectedRows == 0) {
                  throw new SQLException("Creating a Part failed, no rows affected.");
               }

               try (ResultSet resultSet = preparedStatement.getGeneratedKeys()) {
                  if (resultSet.next()) {
//                     System.out.println("TODO: Use this maybe...? " + resultSet.getInt(1));
                  }
               }
            } catch (SQLException e) {
               LOGGER.error(e.getMessage());
               connection.rollback();
            }
         }

         connection.commit();
      } catch (SQLException e) {
         LOGGER.error(e.getMessage());
      }
   }

   @Override
   public List<String> getDirectoryPathsOrderedByIndex(int fileId) {
      List<String> directoryPaths = new ArrayList<>();
      try (Connection connection = DataSource.getConnection();
           PreparedStatement preparedStatement =
                 connection.prepareStatement(FIND_DIRECTORY_PATHS_FOR_FILE_ID_ORDERED_BY_INDEX_QUERY)) {
         preparedStatement.setInt(1, fileId);

         try (ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
               directoryPaths.add(resultSet.getString(1));
            }
         }
      } catch (SQLException e) {
         LOGGER.error(e.getMessage());
      }
      return directoryPaths;
   }
}
