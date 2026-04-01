package fileloader.com.example.fileloader.service;

import fileloader.com.example.fileloader.enitity.CallDetailsRecord;
import fileloader.com.example.fileloader.enitity.CdrLog;
import fileloader.com.example.fileloader.repository.CallDetailRecordsRepository;
import fileloader.com.example.fileloader.repository.CdrLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;

@Service
public class FileLoaderService {

    private static final Logger log = LoggerFactory.getLogger(FileLoaderService.class);
    private static final int EXPECTED_FIELD_COUNT = 33;

    // RECORD_DATE_FORMATTER: handles comma separator with exactly 3 digits for milliseconds
    // Format: yyyy-MM-dd HH:mm:ss,SSS (e.g., 2023-08-18 10:00:00,024)
    private static final DateTimeFormatter RECORD_DATE_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS");

    // TSTAMP_FORMATTER: handles variable-length milliseconds (1-3 digits)
    // Formats supported:
    // - yyyy-MM-dd HH:mm:ss.SSS (e.g., 2023-08-18 10:00:00.024)
    // - yyyy-MM-dd HH:mm:ss.SS  (e.g., 2023-08-18 10:59:49.97)
    // - yyyy-MM-dd HH:mm:ss.S   (e.g., 2023-08-18 10:59:49.9)
    private static final DateTimeFormatter TSTAMP_FORMATTER =
        new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd HH:mm:ss")
            .appendFraction(ChronoField.MILLI_OF_SECOND, 1, 3, true)  // 1-3 digits, with dot separator
            .toFormatter();

    private final CdrLogRepository cdrLogRepository;
    private final CallDetailRecordsRepository callDetailRecordsRepository;
    private final ResourceLoader resourceLoader;

    @Value("${fileloader.directory.path}")
    private String filePath;


    public FileLoaderService(CdrLogRepository cdrLogRepository, CallDetailRecordsRepository callDetailRecordsRepository,
                            ResourceLoader resourceLoader) {
        this.cdrLogRepository = cdrLogRepository;
        this.callDetailRecordsRepository = callDetailRecordsRepository;
        this.resourceLoader = resourceLoader;
    }


    @Scheduled(fixedRate = 60000)
    public void monitorDirectory() {
        try {
            File directory = resolveFilePath();
            if (directory == null || !directory.exists()) {
                log.warn("Directory does not exist: {}", filePath);
                return;
            }

            if (!directory.isDirectory()) {
                log.warn("Path is not a directory: {}", filePath);
                return;
            }

            File[] files = directory.listFiles((dir, name) -> name.startsWith("cdr.log"));
            if (files == null || files.length == 0) {
                log.debug("No cdr.log files found in directory: {}", directory.getAbsolutePath());
                return;
            }

            log.info("Found {} cdr.log file(s) to process", files.length);

            for (File file : files) {
                // Check if file was already processed using database records
                if (!isFileAlreadyProcessed(file.getName())) {
                    log.info("Processing file: {}", file.getAbsolutePath());
                    processFile(file);
                } else {
                    log.debug("File already processed: {}", file.getName());
                }
            }
        } catch (Exception e) {
            log.error("Error monitoring directory: {}", filePath, e);
        }
    }

    /**
     * Resolves the file path considering both classpath and absolute paths
     */
    private File resolveFilePath() {
        try {
            // Try as absolute path first
            File file = new File(filePath);
            if (file.exists()) {
                return file;
            }

            // Try as classpath resource
            Resource resource = resourceLoader.getResource("classpath:" + filePath);
            if (resource.exists()) {
                return resource.getFile();
            }

            // Try as file: URL
            resource = resourceLoader.getResource("file:" + filePath);
            if (resource.exists()) {
                return resource.getFile();
            }

            log.warn("Could not resolve file path: {}", filePath);
            return null;
        } catch (IOException e) {
            log.error("Error resolving file path: {}", filePath, e);
            return null;
        }
    }

    /**
     * Check if a file has already been processed by querying the database
     */
    private boolean isFileAlreadyProcessed(String fileName) {
        try {
            CdrLog existingLog = cdrLogRepository.findByFileName(fileName);
            return existingLog != null && existingLog.getUploadEndTime() != null;
        } catch (Exception e) {
            log.error("Error checking if file was processed: {}", fileName, e);
            return false;
        }
    }

    private void processFile(File file) {
        CdrLog cdrLog = new CdrLog();
        cdrLog.setFileName(file.getName());
        cdrLog.setUploadStartTime(LocalDateTime.now());
        cdrLog.setSuccessfulRecords(0);
        cdrLog.setFailedRecords(0);
        cdrLogRepository.save(cdrLog);

        List<CallDetailsRecord> recordList = new ArrayList<>();
        int lineNumber = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                lineNumber++;
                try {
                    // Skip empty or whitespace-only lines
                    if (line.trim().isEmpty()) {
                        log.debug("Skipping empty line at line number: {}", lineNumber);
                        continue;
                    }

                    CallDetailsRecord record = parseLine(line);
                    recordList.add(record);
                    cdrLog.setSuccessfulRecords(cdrLog.getSuccessfulRecords() + 1);
                } catch (Exception e) {
                    cdrLog.setFailedRecords(cdrLog.getFailedRecords() + 1);
                    log.warn("Failed to parse line {} in file {}: {}", lineNumber, file.getName(), e.getMessage());
                }
            }
            log.info("File processing completed: {} - Total lines: {}, Successful: {}, Failed: {}",
                    file.getName(), lineNumber, cdrLog.getSuccessfulRecords(), cdrLog.getFailedRecords());
        } catch (IOException e) {
            log.error("Error reading file: {}", file.getAbsolutePath(), e);
        }

        // Save all parsed records to the database
        if (!recordList.isEmpty()) {
            try {
                callDetailRecordsRepository.saveAll(recordList);
                log.info("Successfully saved {} records to database", recordList.size());
            } catch (Exception e) {
                log.error("Error saving records to database: {}", e.getMessage(), e);
                cdrLog.setFailedRecords(cdrLog.getFailedRecords() + recordList.size());
            }
        }

        // Update the upload end time and save final status
        cdrLog.setUploadEndTime(LocalDateTime.now());
        try {
            cdrLogRepository.save(cdrLog);
        } catch (Exception e) {
            log.error("Error updating CdrLog status for file: {}", file.getName(), e);
        }
    }

    private CallDetailsRecord parseLine(String line) {
        // Validate input
        if (line.trim().isEmpty()) {
            throw new RuntimeException("Line is empty");
        }

        String[] lines = line.split("\\|", -1); // -1 keeps trailing empty strings
        if (lines.length != EXPECTED_FIELD_COUNT) {
            throw new RuntimeException("Invalid line: expected " + EXPECTED_FIELD_COUNT + " parts, got " + lines.length);
        }

        try {
            CallDetailsRecord record = new CallDetailsRecord();
            record.setRecordDate(parseLocalDateTime(lines[0], RECORD_DATE_FORMATTER));
            record.setLSpc(parseInt(lines[1]));
            record.setLSsn(parseInt(lines[2]));
            record.setLRi(parseInt(lines[3]));
            record.setLGtI(parseInt(lines[4]));
            record.setLGtDigits(parseString(lines[5]));
            record.setRSpc(parseInt(lines[6]));
            record.setRSsn(parseInt(lines[7]));
            record.setRRi(parseInt(lines[8]));
            record.setRGtI(parseInt(lines[9]));
            record.setRGtDigits(parseString(lines[10]));
            record.setServiceCode(parseString(lines[11]));
            record.setOrNature(parseInt(lines[12]));
            record.setOrPlan(parseInt(lines[13]));
            record.setOrDigits(parseString(lines[14]));
            record.setDeNature(parseInt(lines[15]));
            record.setDePlan(parseInt(lines[16]));
            record.setDeDigits(parseString(lines[17]));
            record.setIsdnNature(parseInt(lines[18]));
            record.setIsdnPlan(parseInt(lines[19]));
            record.setMsisdn(parseString(lines[20]));
            record.setVlrNature(parseInt(lines[21]));
            record.setVlrPlan(parseInt(lines[22]));
            record.setVlrDigits(parseString(lines[23]));
            record.setImsi(parseString(lines[24]));
            record.setStatus(parseString(lines[25]));
            record.setType(parseString(lines[26]));
            record.setTstamp(parseLocalDateTime(lines[27], TSTAMP_FORMATTER));
            record.setLocalDialogId(parseLong(lines[28]));
            record.setRemoteDialogId(parseLong(lines[29]));
            record.setDialogDuration(parseLong(lines[30]));
            record.setUssdString(parseString(lines[31]));
            record.setId(parseString(lines[32]));

            return record;
        } catch (Exception e) {
            throw new RuntimeException("Error parsing line: " + e.getMessage(), e);
        }
    }

    /**
     * Parse string value, return null if empty
     */
    private String parseString(String s) {
        return (s != null && !s.trim().isEmpty()) ? s.trim() : null;
    }

    /**
     * Parse LocalDateTime with custom formatter
     */
    private LocalDateTime parseLocalDateTime(String s, DateTimeFormatter formatter) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(s.trim(), formatter);
        } catch (Exception e) {
            throw new RuntimeException("Invalid datetime format '" + s + "': " + e.getMessage(), e);
        }
    }

    /**
     * Parse Integer value with error handling
     */
    private Integer parseInt(String s) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(s.trim());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid integer value '" + s + "': " + e.getMessage(), e);
        }
    }

    /**
     * Parse Long value with error handling
     */
    private Long parseLong(String s) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(s.trim());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid long value '" + s + "': " + e.getMessage(), e);
        }
    }
}
