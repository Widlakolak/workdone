package com.workdone.backend.analysis;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OfferFingerprintFactoryTest {

    private final OfferFingerprintFactory factory = new OfferFingerprintFactory();

    @Test
    void shouldNormalizeInputBeforeHashing() {
        String first = factory.createFrom("  Java-Developer ", "Acme!", "Łódź");
        String second = factory.createFrom("java   developer", "ACME", "Lodz");

        assertThat(first).isEqualTo(second);
        assertThat(first).isEqualTo("7140ef24b6312694a80d7ef5b43bdff7151515b5a279a18fca1b19054a77b639");
    }
}