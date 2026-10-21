package biz.icecat.icedatav2.it.suites;

import biz.icecat.icedatav2.it.BaseDataImportIT;
import org.junit.jupiter.api.Test;

class DataImportIT extends BaseDataImportIT {

    @Test
    void schedulerDownloadsGzipParsesAndPersistsBothSourcesIncludingPartialBatch() {
        updateDatabase();

        verifyImportedCounts();
        verifyImportedLanguage();
        verifyImportedSupplier("Original");
    }

    @Test
    void nextDailyImportUpdatesExistingIdsInsteadOfCreatingDuplicates() throws Exception {
        updateDatabase();
        supplierDocument("Updated");
        updateDatabase();

        verifyImportedCounts();
        verifyImportedSupplier("Updated");
    }

    @Test
    void unavailableSourceKeepsExistingDataAndDoesNotPreventOtherImport() {
        importLanguages();
        makeLanguagesSourceUnavailable();
        updateDatabase();

        verifyImportedCounts();
    }
}
