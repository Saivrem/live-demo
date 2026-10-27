package biz.icecat.icedatav2.service;

import biz.icecat.icedatav2.configuration.properties.ApplicationProperties;
import biz.icecat.icedatav2.configuration.properties.DailyIndexProperties;
import biz.icecat.icedatav2.models.entity.DailyIndexEntity;
import biz.icecat.icedatav2.models.entity.LanguageEntity;
import biz.icecat.icedatav2.repository.DailyIndexRepository;
import biz.icecat.icedatav2.repository.LanguagesRepository;
import biz.icecat.icedatav2.service.impl.DailyIndexService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DailyIndexServiceTest {
    private final DailyIndexRepository repository = mock(DailyIndexRepository.class);
    private final LanguagesRepository languages = mock(LanguagesRepository.class);
    private final DailyIndexProperties settings = new DailyIndexProperties();
    private final Map<String, DailyIndexEntity> stored = new HashMap<>();
    private final List<String> requests = new CopyOnWriteArrayList<>();
    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-10-27T00:00:01Z"));
    private HttpServer server;
    private DailyIndexService service;
    private volatile int xmlStatus = 200;
    private volatile String xmlModified = "Mon, 26 Oct 2026 23:59:59 GMT";
    private volatile String authorization;
    private final Clock clock = new Clock() {
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return now.get(); }
    };

    @BeforeEach
    void setup() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requests.add(exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath());
            authorization = exchange.getRequestHeaders().getFirst("Authorization");
            boolean xml = exchange.getRequestURI().getPath().endsWith("xml.gz");
            String modified = xml ? xmlModified : "Tue, 27 Oct 2026 00:00:00 GMT";
            if (modified != null) exchange.getResponseHeaders().set("Last-Modified", modified);
            exchange.sendResponseHeaders(xml ? xmlStatus : 200, -1);
            exchange.close();
        });
        server.start();
        ApplicationProperties app = new ApplicationProperties();
        app.setIcecatBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/export/");
        app.setServiceUserName("test-user");
        app.setServicePassword("test-password");
        service = new DailyIndexService(repository, languages, app, settings, clock);
        when(languages.findAll()).thenReturn(List.of(new LanguageEntity().setShortCode("en"),
                new LanguageEntity().setShortCode("EN")));
        when(repository.findByRepositoryAndLangCodeAndFormat(anyString(), anyString(), anyString()))
                .thenAnswer(call -> Optional.ofNullable(stored.get(call.getArgument(0) + "/" + call.getArgument(1) + "/" + call.getArgument(2))));
        when(repository.save(any())).thenAnswer(call -> {
            DailyIndexEntity entry = call.getArgument(0);
            stored.put(entry.getRepository() + "/" + entry.getLangCode() + "/" + entry.getFormat(), entry);
            return entry;
        });
        when(repository.findByRepositoryInOrderByLangCodeAscRepositoryAscFormatAsc(anyList()))
                .thenAnswer(call -> stored.values().stream()
                        .filter(entry -> ((List<?>) call.getArgument(0)).contains(entry.getRepository())).toList());
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void freeUsesAuthenticatedHeadRequestsForBothFormatsAndDeduplicatesLanguages() {
        service.refreshIfDue();
        service.refreshIfDue();
        assertThat(requests).containsExactly("HEAD /export/freexml/EN/daily.index.xml.gz",
                "HEAD /export/freexml/EN/daily.index.csv.gz");
        assertThat(authorization).isEqualTo("Basic " + Base64.getEncoder()
                .encodeToString("test-user:test-password".getBytes(StandardCharsets.UTF_8)));
        assertThat(stored).hasSize(2);
    }

    @Test
    void fullChecksBothRepositoriesButSwitchingToFreeHidesOldFullRows() {
        settings.setAccess(DailyIndexProperties.Access.FULL);
        service.refreshIfDue();
        assertThat(requests).hasSize(4);
        assertThat(service.findAll()).hasSize(4);
        settings.setAccess(DailyIndexProperties.Access.FREE);
        assertThat(service.findAll()).hasSize(2).allMatch(entry -> entry.getRepository().equals("freexml"));
    }

    @Test
    void nextSweepRunsOnlyAfterTheThreeHourDelayAndUpdatesExistingRows() {
        service.refreshIfDue();
        now.set(now.get().plus(Duration.ofHours(2)));
        service.refreshIfDue();
        assertThat(requests).hasSize(2);
        now.set(now.get().plus(Duration.ofHours(1)));
        service.refreshIfDue();
        assertThat(requests).hasSize(4);
        assertThat(stored).hasSize(2);
    }

    @ParameterizedTest
    @CsvSource({"200,missing,MISSING_LAST_MODIFIED", "200,invalid,INVALID_LAST_MODIFIED", "401,present,HTTP_ERROR", "404,present,HTTP_ERROR", "302,present,HTTP_ERROR"})
    void unsuccessfulCheckPreservesLastKnownFileDateAndContinuesWithOtherFiles(int status, String header, String result) {
        service.refreshIfDue();
        Instant previous = stored.get("freexml/EN/xml").getLastModified();
        now.set(now.get().plus(Duration.ofHours(3)));
        xmlStatus = status;
        if (header.equals("missing")) xmlModified = null;
        if (header.equals("invalid")) xmlModified = "invalid date";
        service.refreshIfDue();
        assertThat(stored.get("freexml/EN/xml").getLastModified()).isEqualTo(previous);
        assertThat(stored.get("freexml/EN/xml").getResult()).isEqualTo(result);
        assertThat(stored.get("freexml/EN/xml").getCheckedAt()).isEqualTo(now.get());
        assertThat(stored.get("freexml/EN/csv").getResult()).isEqualTo("OK");
        assertThat(requests).hasSize(4);
    }

    @ParameterizedTest
    @CsvSource({"2026-10-27T00:00:00Z,TODAY", "2026-10-26T23:59:59Z,YESTERDAY", "2026-10-25T23:59:59Z,OLD", "2026-10-27T12:00:00Z,UNKNOWN"})
    void freshnessUsesCalendarDaysInUtcAndMarksChecksOverdue(String modified, String freshness) {
        xmlModified = DateTimeFormatter.RFC_1123_DATE_TIME.format(Instant.parse(modified).atZone(ZoneOffset.UTC));
        service.refreshIfDue();
        assertThat(service.findAll().stream().filter(entry -> entry.getFormat().equals("xml")).findFirst().orElseThrow().getFreshness())
                .isEqualTo(freshness);
        assertThat(service.findAll()).allMatch(entry -> !entry.isCheckOverdue());
        now.set(now.get().plus(Duration.ofHours(4)));
        assertThat(service.findAll()).allMatch(entry -> entry.isCheckOverdue());
    }

    @Test
    void noLanguagesDoesNotConsumeTheFirstSweep() {
        when(languages.findAll()).thenReturn(List.of());
        service.refreshIfDue();
        assertThat(requests).isEmpty();
        when(languages.findAll()).thenReturn(List.of(new LanguageEntity().setShortCode("en")));
        service.refreshIfDue();
        assertThat(requests).hasSize(2);
    }

    @Test
    void networkFailureIsPersistedWithoutInventingAFileTimestamp() {
        server.stop(0);
        settings.setTimeoutMillis(100);
        service.refreshIfDue();
        assertThat(stored.values()).hasSize(2).allMatch(entry -> entry.getLastModified() == null
                && entry.getHttpStatus() == null && entry.getResult().equals("NETWORK_ERROR"));
    }
}
