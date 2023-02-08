package org.etf.unibl.repo;

import org.etf.unibl.domain.User;
import org.etf.unibl.dto.Credentials;

import java.util.Optional;

public interface UsersRepository {
   Optional<User> login(Credentials credentials);

   boolean register(Credentials credentials);

   void updateIsActive(boolean isActive, String username);
}
