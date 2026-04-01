package fileloader.com.example.fileloader.repository;

import fileloader.com.example.fileloader.enitity.CdrLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CdrLogRepository extends JpaRepository<CdrLog, Long> {
    /**
     * Find CdrLog by file name
     * @param fileName the name of the file
     * @return CdrLog entity if found, null otherwise
     */
    CdrLog findByFileName(String fileName);
}
