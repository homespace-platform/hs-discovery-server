package com.hs.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableEurekaServer
@SpringBootApplication
public class HsDiscoveryServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(HsDiscoveryServerApplication.class, args);
	}

}
