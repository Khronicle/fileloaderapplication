package fileloader.com.example.fileloader.enitity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "cdr_logs")
@Data
public class CdrLog {
    // TODO : Provide the fields and methods for CdrLog entity.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "upload_start_time")
    private LocalDateTime uploadStartTime;

    @Column(name = "upload_end_time")
    private LocalDateTime uploadEndTime;

    @Column(name = "successful_records")
    private int successfulRecords;

    @Column(name = "failed_records")
    private int failedRecords;
}
