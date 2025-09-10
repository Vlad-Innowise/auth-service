package by.innowise.auth.util;

import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

@UtilityClass
public class DateTimeUtil {

    public LocalDateTime convertDateToLocalDateTime(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC);
    }

    public LocalDateTime getNowInUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
