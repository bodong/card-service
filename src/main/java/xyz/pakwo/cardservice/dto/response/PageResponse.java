package xyz.pakwo.cardservice.dto.response;

import java.util.List;

/**
 * @author sarwo.wibowo
 **/
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {
}
