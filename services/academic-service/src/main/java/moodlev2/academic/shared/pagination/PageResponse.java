package moodlev2.academic.shared.pagination;

import java.util.List;

public record PageResponse<T>(
        List<T> content, int page, int size, long totalElements, int totalPages) {}
