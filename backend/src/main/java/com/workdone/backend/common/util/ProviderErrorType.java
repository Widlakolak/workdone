package com.workdone.backend.common.util;

public enum ProviderErrorType {
    TIMEOUT,
    HTTP_4XX,
    HTTP_5XX,
    OTHER,
    CIRCUIT_OPEN
}