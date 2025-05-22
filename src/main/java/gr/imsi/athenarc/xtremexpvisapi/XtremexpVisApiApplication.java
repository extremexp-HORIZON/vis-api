package gr.imsi.athenarc.xtremexpvisapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class XtremexpVisApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(XtremexpVisApiApplication.class, args);
	}

}
