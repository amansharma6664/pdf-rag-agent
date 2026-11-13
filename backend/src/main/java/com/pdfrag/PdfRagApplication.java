package com.pdfrag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PdfRagApplication {

    public static void main(String[] args) {
        SpringApplication.run(PdfRagApplication.class, args);
    }
}
