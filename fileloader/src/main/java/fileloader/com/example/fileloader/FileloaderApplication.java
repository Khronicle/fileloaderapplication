package fileloader.com.example.fileloader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FileloaderApplication {

	public static void main(String[] args) {
		SpringApplication.run(FileloaderApplication.class, args);
	}

}
