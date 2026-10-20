package com.aipanel.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorsOriginsTest {

    @Test
    void parsesTrimsAndDeduplicates() {
        assertThat(CorsOrigins.parse(" http://localhost:5173/ ,https://panel.example.com,,http://localhost:5173"))
            .containsExactly("http://localhost:5173", "https://panel.example.com");
        assertThat(CorsOrigins.parse("https://*.example.com")).containsExactly("https://*.example.com");
    }

    @Test
    void blankMeansNoOrigins() {
        assertThat(CorsOrigins.parse(null)).isEmpty();
        assertThat(CorsOrigins.parse("  ")).isEmpty();
    }

    @Test
    void wildcardOnlyAsSoleEntry() {
        assertThat(CorsOrigins.allowsAny(CorsOrigins.parse("*"))).isTrue();
        assertThatThrownBy(() -> CorsOrigins.parse("*,http://localhost:5173")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsValuesThatAreNotOrigins() {
        assertThatThrownBy(() -> CorsOrigins.parse("localhost:5173")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CorsOrigins.parse("https://panel.example.com/app")).isInstanceOf(IllegalArgumentException.class);
    }
}
