package xyz.pakwo.cardservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * @author sarwo.wibowo
 **/
@ConfigurationProperties(prefix = "card-service")
public record CardServiceProperties(Pagination pagination, Masking masking) {
    public record Pagination(
            int maxPageSize) {
    }

    public record Masking(
            List<String> jsonFields
    ) {
    }
}
