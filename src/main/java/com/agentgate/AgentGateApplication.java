package com.agentgate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.File;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class AgentGateApplication {

    static {
        ensureDataDirectoryExists();
    }

    public static void main(String[] args) {
        ensureDataDirectoryExists();
        SpringApplication.run(AgentGateApplication.class, args);
    }

    public static void ensureDataDirectoryExists() {
        File dataDir = new File("data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
    }
}
