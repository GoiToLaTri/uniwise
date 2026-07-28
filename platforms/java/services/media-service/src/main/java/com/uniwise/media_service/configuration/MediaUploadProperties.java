package com.uniwise.media_service.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "media.upload")
public class MediaUploadProperties {
    private DataSize thumbnailMaxSize = DataSize.ofMegabytes(10);
    private DataSize videoMaxSize = DataSize.ofMegabytes(200);
}
