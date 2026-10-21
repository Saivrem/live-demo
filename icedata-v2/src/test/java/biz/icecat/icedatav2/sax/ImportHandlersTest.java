package biz.icecat.icedatav2.sax;

import biz.icecat.icedatav2.base.BaseSaxTest;
import biz.icecat.icedatav2.models.refs.languages.Language;
import biz.icecat.icedatav2.models.refs.suppliers.Supplier;
import biz.icecat.icedatav2.utils.SaxUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class ImportHandlersTest extends BaseSaxTest {


    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5})
    void languagesFlushFullBatchesAndTheFinalRemainderExactlyOnce(int count) throws Exception {
        List<List<Language>> batches = new ArrayList<>();
        LanguageHandler handler = new LanguageHandler(2, batch -> batches.add(List.copyOf(batch)));
        String records = IntStream.rangeClosed(1, count).mapToObj(id ->
                "<Language ID=\"" + id + "\" Code=\"English &amp; Spanish\" ShortCode=\"en\" Updated=\"2024-01-02 03:04:05\"/>")
                .reduce("", String::concat);
        SaxUtils.getParser().parse(xml(records), handler);
        assertThat(handler.getProcessed()).isEqualTo(count);
        assertThat(batches.stream().flatMap(List::stream).map(Language::getLangId).toList())
                .containsExactlyElementsOf(IntStream.rangeClosed(1, count).mapToObj(recordId -> (long) recordId).toList());
        assertThat(batches).allSatisfy(batch -> assertThat(batch.size()).isBetween(1, 2));
        assertThat(batches).hasSize((count + 1) / 2);
        batches.stream().flatMap(List::stream).forEach(language -> {
            assertThat(language.getIntLangName()).isEqualTo("English & Spanish");
            assertThat(language.getShortCode()).isEqualTo("en");
            assertThat(language.getUpdated()).isNotNull();
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5})
    void suppliersFlushBatchesAndMapSponsorFlags(int count) throws Exception {
        List<List<Supplier>> batches = new ArrayList<>();
        SupplierHandler handler = new SupplierHandler(2, batch -> batches.add(List.copyOf(batch)));
        String records = IntStream.rangeClosed(1, count).mapToObj(id ->
                "<Supplier ID=\"" + id + "\" Name=\"Brand\" Sponsor=\"" + (id % 2) + "\" LogoPic=\"logo.png\"/>")
                .reduce("", String::concat);
        SaxUtils.getParser().parse(xml(records), handler);
        assertThat(handler.getProcessed()).isEqualTo(count);
        assertThat(batches).hasSize((count + 1) / 2);
        List<Supplier> suppliers = batches.stream().flatMap(List::stream).toList();
        assertThat(suppliers.stream().map(Supplier::getSupplierId).toList())
                .containsExactlyElementsOf(IntStream.rangeClosed(1, count).mapToObj(recordId -> (long) recordId).toList());
        assertThat(suppliers).allSatisfy(supplier -> {
            assertThat(supplier.isSponsor()).isEqualTo(supplier.getSupplierId() % 2 == 1);
            assertThat(supplier.getBrandLogo()).isEqualTo("logo.png");
        });
    }

    private ByteArrayInputStream xml(String records) {
        return xmlStream("<ICECAT-interface>" + records + "</ICECAT-interface>");
    }
}
