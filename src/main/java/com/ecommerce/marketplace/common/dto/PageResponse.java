package com.ecommerce.marketplace.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Standard paginated response wrapper for cursor-based pagination.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    private List<T> data;
    private long totalCount;
    private String nextCursor;
    private boolean hasMore;

    public static <T> PageResponse<T> of(List<T> data, long totalCount, String nextCursor) {
        return new PageResponse<>(data, totalCount, nextCursor, nextCursor != null);
    }

    public static <T> PageResponse<T> from(org.springframework.data.domain.Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getTotalElements(), null, page.hasNext());
    }

    public static <T> PageResponse<T> empty() {
        return new PageResponse<>(List.of(), 0, null, false);
    }
}
