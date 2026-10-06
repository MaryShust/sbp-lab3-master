package com.example.sbp.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;

/**
 * Централизованная защита от XSS: экранирует все строковые значения
 * перед сериализацией в JSON (Jackson).
 */
@Configuration
public class HtmlEscapingConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer htmlEscapingCustomizer() {
        return builder -> {
            builder.serializerByType(String.class, stringEscapingSerializer());
            builder.serializerByType(CharSequence.class, charSequenceEscapingSerializer());
        };
    }

    private JsonSerializer<String> stringEscapingSerializer() {
        return new StdSerializer<>(String.class) {
            @Override
            public void serialize(String value, JsonGenerator gen, SerializerProvider provider) throws IOException {
                gen.writeString(sanitize(value));
            }
        };
    }

    private JsonSerializer<CharSequence> charSequenceEscapingSerializer() {
        return new StdSerializer<>(CharSequence.class) {
            @Override
            public void serialize(CharSequence value, JsonGenerator gen, SerializerProvider provider) throws IOException {
                gen.writeString(value == null ? null : sanitize(value.toString()));
            }
        };
    }

    private String sanitize(String value) {
        if (value == null) {
            return null;
        }
        return HtmlUtils.htmlEscape(value);
    }
}