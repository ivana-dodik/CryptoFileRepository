package org.etf.unibl.repo;

import org.etf.unibl.domain.RepoFile;

import java.util.List;
import java.util.Optional;

public interface FileRepository {
   Optional<RepoFile> save(String filename, int userId, int filesize);

   List<RepoFile> getAllFilesByUserId(int userId);
}
