package biz.icecat.icedatav2.utils;

import biz.icecat.icedatav2.annotation.CsvMapping;
import biz.icecat.icedatav2.annotation.CsvRow;
import lombok.experimental.UtilityClass;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@UtilityClass
public class CsvUtils {

    public static String escapeCsvField(Object value) {
        return "\"" + (value == null ? "" : value.toString().replace("\"", "\"\"")) + "\"";
    }

    public static String toCsvRow(Object object) {
        Objects.requireNonNull(object, "CSV row object must not be null");
        return csvFields(object.getClass()).stream()
                .map(field -> {
                    ReflectionUtils.makeAccessible(field);
                    return ReflectionUtils.getField(field, object);
                })
                .map(CsvUtils::escapeCsvField)
                .collect(Collectors.joining(","));
    }

    public static String toCsvHeader(Class<?> type) {
        return csvFields(type).stream()
                .map(field -> {
                    String name = field.getAnnotation(CsvMapping.class).name();
                    return name.isBlank() ? field.getName() : name;
                })
                .map(CsvUtils::escapeCsvField)
                .collect(Collectors.joining(","));
    }

    private static List<Field> csvFields(Class<?> type) {
        if (!type.isAnnotationPresent(CsvRow.class)) {
            throw new IllegalArgumentException("Class " + type.getName() + " must be annotated with @CsvRow");
        }
        List<Field> fields = new ArrayList<>();
        ReflectionUtils.doWithFields(type, fields::add,
                field -> field.isAnnotationPresent(CsvMapping.class)
                        && !Modifier.isStatic(field.getModifiers()) && !field.isSynthetic());
        if (fields.isEmpty()) {
            throw new IllegalArgumentException("Class " + type.getName() + " has no @CsvMapping fields");
        }
        Set<Integer> ordinals = new HashSet<>();
        for (Field field : fields) {
            int ordinal = field.getAnnotation(CsvMapping.class).ordinal();
            if (ordinal < 0 || !ordinals.add(ordinal)) {
                throw new IllegalArgumentException("CSV ordinal must be nonnegative and unique: " + ordinal);
            }
        }
        fields.sort(Comparator.comparingInt(field -> field.getAnnotation(CsvMapping.class).ordinal()));
        return fields;
    }
}
