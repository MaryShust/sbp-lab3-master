package com.example.sbp.config;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RepositoryService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.io.InputStream;

@Slf4j
@Component
public class CamundaFormDeploymentConfig implements ApplicationRunner {

    private final RepositoryService repositoryService;

    public CamundaFormDeploymentConfig(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        deployForm("forms/payment-info.form", "payment-info.form");
        deployForm("forms/payment.form", "payment.form");
        deployForm("forms/petrovna-examination.form", "petrovna-examination.form");
    }

    private void deployForm(String classpathPath, String resourceName) throws Exception {
        ClassPathResource resource = new ClassPathResource(classpathPath);
        if (!resource.exists()) {
            log.warn("Form not found on classpath: {}", classpathPath);
            return;
        }
        try (InputStream stream = resource.getInputStream()) {
            repositoryService.createDeployment()
                    .name("Camunda Forms")
                    .addInputStream(resourceName, stream)
                    .enableDuplicateFiltering(false)
                    .deploy();
            log.info("Deployed form: {}", classpathPath);
        }
    }
}