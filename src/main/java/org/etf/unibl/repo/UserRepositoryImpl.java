package org.etf.unibl.repo;

import org.etf.unibl.domain.User;
import org.etf.unibl.dto.Credentials;
import org.etf.unibl.service.CertificateService;
import org.etf.unibl.service.CertificateServiceImpl;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class UserRepositoryImpl implements UsersRepository {

   private static final Logger LOGGER = LoggerFactory.getLogger(UserRepositoryImpl.class);
   private static final String REGISTER_QUERY = "INSERT INTO krz.users\n" +
         "(username, passphrase, is_active, created_at)\n" +
         "VALUES(?, ?, 1, CURRENT_TIMESTAMP)";
   private static final String FIND_USER_QUERY = "SELECT user_id, username, passphrase, is_active\n" +
         "FROM krz.users\n" +
         "WHERE username = ?";
   private static final String UPDATE_IS_ACTIVE_QUERY = "UPDATE krz.users\n" +
         "SET is_active=?\n" +
         "WHERE username=?";
   private final CertificateService certificateService = new CertificateServiceImpl();

   @Override
   public Optional<User> login(Credentials credentials) {
      try (Connection connection = DataSource.getConnection();
           PreparedStatement preparedStatement = connection.prepareStatement(FIND_USER_QUERY)
      ) {
         preparedStatement.setString(1, credentials.getUsername());
         try (ResultSet resultSet = preparedStatement.executeQuery()) {
            if (resultSet.next()) {
               int userId = resultSet.getInt(1);
               String username = resultSet.getString(2);
               String password = resultSet.getString(3);
               boolean isActive = resultSet.getBoolean(4);

               if (BCrypt.checkpw(credentials.getPassword(), password)) {
                  var user = new User(userId, username, isActive);
                  return Optional.of(user);
               }
            }
         }
      } catch (SQLException e) {
         LOGGER.error(e.getMessage());
      }
      return Optional.empty();
   }

   @Override
   public boolean register(Credentials credentials) {
      try (Connection connection = DataSource.getConnection();
           PreparedStatement findUserPreparedStatement = connection.prepareStatement(FIND_USER_QUERY);
           PreparedStatement registerPreparedStatement = connection.prepareStatement(REGISTER_QUERY)
      ) {
         // check if the username is already taken
         findUserPreparedStatement.setString(1, credentials.getUsername());

         try (ResultSet resultSet = findUserPreparedStatement.executeQuery()) {
            if (resultSet.next()) {
               LOGGER.warn("Username already taken");
               return false;
            }
         }

         // register user
         String username = credentials.getUsername();
         registerPreparedStatement.setString(1, username);
         String passwordHash = BCrypt.hashpw(credentials.getPassword(), BCrypt.gensalt());
         registerPreparedStatement.setString(2, passwordHash);

         try {
            certificateService.issueCertificate(username, 180);
            registerPreparedStatement.executeUpdate();
            return true;
         } catch (Exception e) {
            LOGGER.error("Couldn't issue a certificate to the user, something went wrong!");
            return false;
         }
      } catch (Exception e) {
         LOGGER.error(e.getMessage());
         return false;
      }
   }

   @Override
   public void updateIsActive(boolean isActive, String username) {
      try (Connection connection = DataSource.getConnection();
           PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_IS_ACTIVE_QUERY)) {
         preparedStatement.setBoolean(1, isActive);
         preparedStatement.setString(2, username);
         preparedStatement.executeUpdate();
      } catch (SQLException e) {
         LOGGER.error(e.getMessage());
      }
   }
}
