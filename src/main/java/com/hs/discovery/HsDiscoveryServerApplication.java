package com.hs.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@EnableEurekaServer
@SpringBootApplication
public class HsDiscoveryServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(HsDiscoveryServerApplication.class, args);
	}

}
