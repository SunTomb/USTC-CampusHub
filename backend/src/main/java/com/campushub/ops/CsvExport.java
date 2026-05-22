package com.campushub.ops;

public record CsvExport(String fileName, String contentType, String body) {

    private static final String UTF_8_BOM = String.valueOf('﻿');
    private static final String CSV_CONTENT_TYPE = "text/csv; charset=UTF-8";

    public static CsvExport of(String fileName, String bodyWithoutBom) {
        return new CsvExport(fileName, CSV_CONTENT_TYPE, UTF_8_BOM + (bodyWithoutBom == null ? "" : bodyWithoutBom));
    }
}
