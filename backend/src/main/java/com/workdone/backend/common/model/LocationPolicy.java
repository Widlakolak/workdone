package com.workdone.backend.common.model;

/**
 * Definiuje preferencje lokalizacyjne dla konkretnego miejsca.
 * Pozwala określić, jakie tryby pracy akceptujemy w danym mieście.
 */
public record LocationPolicy(
    String city,
    boolean allowRemote,
    boolean allowHybrid,
    boolean allowOnsite,
    Integer maxDaysInOffice // Opcjonalnie: ile dni w biurze max (null = bez limitu)
) {
    // Statyczna metoda pomocnicza do tworzenia domyślnej polityki "tylko zdalna"
    public static LocationPolicy remoteOnly(String city) {
        return new LocationPolicy(city, true, false, false, 0);
    }
}
