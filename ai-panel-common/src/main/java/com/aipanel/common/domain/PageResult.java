package com.aipanel.common.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Pagination result wrapper
 *
 * @param <T> List element type
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Pagination Result")
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Current page number", example = "1")
    private int pageNum;

    @Schema(description = "Page size", example = "20")
    private int pageSize;

    @Schema(description = "Total records", example = "100")
    private long total;

    @Schema(description = "Total pages", example = "5")
    private int totalPages;

    @Schema(description = "Data list")
    private List<T> list;

    @Schema(description = "Has next page", example = "true")
    private boolean hasNext;

    @Schema(description = "Has previous page", example = "false")
    private boolean hasPrevious;

    /**
     * Create PageResult from data
     */
    public static <T> PageResult<T> of(List<T> list, int pageNum, int pageSize, long total) {
        PageResult<T> result = new PageResult<>();
        result.setList(list != null ? list : Collections.emptyList());
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        result.setTotal(total);
        result.setTotalPages(pageSize > 0 ? (int) Math.ceil((double) total / pageSize) : 0);
        result.setHasNext(pageNum < result.getTotalPages());
        result.setHasPrevious(pageNum > 1);
        return result;
    }

    /**
     * Create empty PageResult
     */
    public static <T> PageResult<T> empty() {
        return of(Collections.emptyList(), 1, 20, 0);
    }

    /**
     * Create empty PageResult with specified page info
     */
    public static <T> PageResult<T> empty(int pageNum, int pageSize) {
        return of(Collections.emptyList(), pageNum, pageSize, 0);
    }

    /**
     * Transform list elements
     */
    public <R> PageResult<R> map(Function<T, R> mapper) {
        List<R> mappedList = this.list.stream()
            .map(mapper)
            .collect(Collectors.toList());

        PageResult<R> result = new PageResult<>();
        result.setList(mappedList);
        result.setPageNum(this.pageNum);
        result.setPageSize(this.pageSize);
        result.setTotal(this.total);
        result.setTotalPages(this.totalPages);
        result.setHasNext(this.hasNext);
        result.setHasPrevious(this.hasPrevious);
        return result;
    }

    /**
     * Check if result is empty
     */
    public boolean isEmpty() {
        return this.list == null || this.list.isEmpty();
    }

    /**
     * Check if result is not empty
     */
    public boolean isNotEmpty() {
        return !isEmpty();
    }

    /**
     * Get list size
     */
    public int size() {
        return this.list != null ? this.list.size() : 0;
    }
}
