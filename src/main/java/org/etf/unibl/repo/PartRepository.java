package org.etf.unibl.repo;

import javafx.util.Pair;

import java.nio.file.Path;
import java.util.List;

public interface PartRepository {
   void saveAll(List<Pair<Path, byte[]>> partNamesWithSigns, int fileId);

   List<String> getDirectoryPathsOrderedByIndex(int fileId);
}
