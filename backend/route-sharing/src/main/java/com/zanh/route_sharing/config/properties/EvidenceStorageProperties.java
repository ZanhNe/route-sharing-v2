package com.zanh.route_sharing.config.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.evidence-storage")
public class EvidenceStorageProperties {
    @NotBlank(message = "app.evidence-storage.root không được để trống")
    private String root = "./.routeshare-data/evidence";
}
