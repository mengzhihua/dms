package com.dms.common;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        DateTimeFormatter dt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter d = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return builder ->
                builder
                        .serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(dt))
                        .deserializerByType(LocalDateTime.class, new LocalDateTimeDeserializer(dt))
                        .serializerByType(LocalDate.class, new LocalDateSerializer(d))
                        .deserializerByType(LocalDate.class, new LocalDateDeserializer(d));
    }
}
