package biz.icecat.icedatav2.utils;

import biz.icecat.icedatav2.annotation.CsvMapping;
import biz.icecat.icedatav2.annotation.CsvRow;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class CsvUtilsTest {

    @Test
    void readsAnnotatedPrivateAndInheritedFieldsInOrdinalOrder() {
        assertThat(CsvUtils.toCsvRow(new Child()))
                .isEqualTo("\"42\",\"A,\"\"B\"\"\r\nC\"");
    }

    @Test
    void headerUsesSameColumnOrderAndEscapesAnnotationNames() {
        assertThat(CsvUtils.toCsvHeader(Child.class))
                .isEqualTo("\"id\",\"display,\"\"name\"\"\"");
    }

    @Test
    void recordFieldsSupportDifferentValueTypesAndNulls() {
        assertThat(CsvUtils.toCsvRow(new Sample(7, true, null)))
                .isEqualTo("\"7\",\"true\",\"\"");
    }

    @Test
    void missingClassAnnotationFailsInsteadOfExportingAllFields() {
        assertThatThrownBy(() -> CsvUtils.toCsvRow(new Object()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("@CsvRow");
    }

    @Test
    void duplicateOrdinalsFailInsteadOfProducingAmbiguousColumnOrder() {
        assertThatThrownBy(() -> CsvUtils.toCsvRow(new DuplicateOrdinals()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("unique");
    }

    @Test
    void noMappedFieldsFailsInsteadOfProducingAnEmptyRow() {
        assertThatThrownBy(() -> CsvUtils.toCsvRow(new Unmapped()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("no @CsvMapping");
    }

    @CsvRow
    private static class Parent {
        @CsvMapping(ordinal = 0)
        private final long id = 42;
    }

    private class Child extends Parent {
        private static final String CONSTANT = "ignored";
        private final List<String> relationship = List.of("ignored");
        @CsvMapping(name = "display,\"name\"", ordinal = 1)
        private final String text = "A,\"B\"\r\nC";
    }

    @CsvRow
    private record Sample(@CsvMapping(ordinal = 0) int number,
                          @CsvMapping(ordinal = 1) boolean enabled,
                          @CsvMapping(ordinal = 2) String missing) {}

    @CsvRow
    private static class DuplicateOrdinals {
        @CsvMapping(ordinal = 0)
        private String first;
        @CsvMapping(ordinal = 0)
        private String second;
    }

    @CsvRow
    private static class Unmapped {
        private String ignored;
    }
}
