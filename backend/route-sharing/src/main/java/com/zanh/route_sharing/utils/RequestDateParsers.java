package com.zanh.route_sharing.utils;

import com.zanh.route_sharing.exception.BusinessException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

public final class RequestDateParsers {

    private RequestDateParsers() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static LocalDateTime parseFlexibleDateTime(String value, String paramName, boolean endOfDay) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            if (value.contains("T")) {
                return LocalDateTime.parse(value);
            }

            LocalDate date = LocalDate.parse(value);
            return endOfDay ? date.plusDays(1).atStartOfDay() : date.atStartOfDay();

        } catch (DateTimeParseException ex) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    paramName + " không đúng định dạng. Hãy dùng yyyy-MM-dd hoặc yyyy-MM-ddTHH:mm:ss");
        }
    }
}