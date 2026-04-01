package fileloader.com.example.fileloader.enitity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "call_details_records")
@Data
public class CallDetailsRecord {
    /***
     * This class is a reflection of the cdr_definition_test file.
     * Used this approach of adding the required length for the various fields instead of using an explicit sql file
     * Using the JPA auto generation feature to create the table in the database.
     * This is a common approach when using Spring Data JPA.
     */

    @Id
    @Column(name = "id", nullable = false, length = 150)
    private String id;

    @Column(name = "record_date", nullable = false)
    private LocalDateTime recordDate;

    @Column(name = "l_spc")
    private Integer lSpc;

    @Column(name = "l_ssn")
    private Integer lSsn;

    @Column(name = "l_ri")
    private Integer lRi;

    @Column(name = "l_gt_i")
    private Integer lGtI;

    @Column(name = "l_gt_digits", length = 18)
    private String lGtDigits;

    @Column(name = "r_spc")
    private Integer rSpc;

    @Column(name = "r_ssn")
    private Integer rSsn;

    @Column(name = "r_ri")
    private Integer rRi;

    @Column(name = "r_gt_i")
    private Integer rGtI;

    @Column(name = "r_gt_digits", length = 18)
    private String rGtDigits;

    @Column(name = "service_code", length = 50)
    private String serviceCode;

    @Column(name = "or_nature")
    private Integer orNature;

    @Column(name = "or_plan")
    private Integer orPlan;

    @Column(name = "or_digits", length = 18)
    private String orDigits;

    @Column(name = "de_nature")
    private Integer deNature;

    @Column(name = "de_plan")
    private Integer dePlan;

    @Column(name = "de_digits", length = 18)
    private String deDigits;

    @Column(name = "isdn_nature")
    private Integer isdnNature;

    @Column(name = "isdn_plan")
    private Integer isdnPlan;

    @Column(name = "msisdn", length = 18)
    private String msisdn;

    @Column(name = "vlr_nature")
    private Integer vlrNature;

    @Column(name = "vlr_plan")
    private Integer vlrPlan;

    @Column(name = "vlr_digits", length = 18)
    private String vlrDigits;

    @Column(name = "imsi", length = 100)
    private String imsi;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "type", nullable = false, length = 30)
    private String type;

    @Column(name = "tstamp", nullable = false)
    private LocalDateTime tstamp;

    @Column(name = "local_dialog_id")
    private Long localDialogId;

    @Column(name = "remote_dialog_id")
    private Long remoteDialogId;

    @Column(name = "dialog_duration")
    private Long dialogDuration;

    @Column(name = "ussd_string", length = 255)
    private String ussdString;
}
