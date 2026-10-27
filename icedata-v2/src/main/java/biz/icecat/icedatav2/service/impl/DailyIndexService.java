package biz.icecat.icedatav2.service.impl;

import biz.icecat.icedatav2.configuration.properties.ApplicationProperties;
import biz.icecat.icedatav2.configuration.properties.DailyIndexProperties;
import biz.icecat.icedatav2.models.api.ApiDailyIndex;
import biz.icecat.icedatav2.models.entity.DailyIndexEntity;
import biz.icecat.icedatav2.models.entity.LanguageEntity;
import biz.icecat.icedatav2.repository.DailyIndexRepository;
import biz.icecat.icedatav2.repository.LanguagesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyIndexService {
    private final DailyIndexRepository repository;
    private final LanguagesRepository languagesRepository;
    private final ApplicationProperties applicationProperties;
    private final DailyIndexProperties properties;
    private final Clock clock;
    private Instant lastCompleted;

    public synchronized void refreshIfDue() {
        if (lastCompleted != null && clock.instant().isBefore(lastCompleted.plus(properties.getDelay()))) return;
        List<String> languages = languagesRepository.findAll().stream()
                .map(LanguageEntity::getShortCode)
                .filter(code -> code != null && code.matches("[a-zA-Z0-9_-]{2,10}"))
                .map(code -> code.toUpperCase(Locale.ROOT)).distinct().sorted().toList();
        if (languages.isEmpty()) {
            log.info("Daily index check skipped: import languages first");
            return;
        }
        for (String repo : properties.repositories()) {
            for (String language : languages) {
                for (String format : List.of("xml", "csv")) {
                    check(repo, language, format);
                }
            }
        }
        lastCompleted = clock.instant();
        log.info("Daily index check completed for {} languages and {} repositories", languages.size(), properties.repositories().size());
    }

    private void check(String repo, String language, String format) {
        DailyIndexEntity entry = repository.findByRepositoryAndLangCodeAndFormat(repo, language, format)
                .orElseGet(() -> new DailyIndexEntity().setRepository(repo).setLangCode(language).setFormat(format));
        HttpURLConnection connection = null;
        entry.setHttpStatus(null);
        try {
            String base = applicationProperties.getIcecatBaseUrl().replaceAll("/+$", "");
            connection = (HttpURLConnection) URI.create(base + "/" + repo + "/" + language
                    + "/daily.index." + format + ".gz").toURL().openConnection();
            connection.setRequestMethod("HEAD");
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(properties.getTimeoutMillis());
            connection.setReadTimeout(properties.getTimeoutMillis());
            String credentials = applicationProperties.getServiceUserName() + ":" + applicationProperties.getServicePassword();
            connection.setRequestProperty("Authorization", "Basic " + Base64.getEncoder()
                    .encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));
            int status = connection.getResponseCode();
            entry.setHttpStatus(status);
            if (status >= 200 && status < 300) {
                String modified = connection.getHeaderField("Last-Modified");
                if (modified == null) {
                    entry.setResult("MISSING_LAST_MODIFIED");
                } else {
                    entry.setLastModified(ZonedDateTime.parse(modified, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant())
                            .setResult("OK");
                }
            } else {
                entry.setResult("HTTP_ERROR");
            }
        } catch (DateTimeParseException exception) {
            entry.setResult("INVALID_LAST_MODIFIED");
        } catch (IOException exception) {
            entry.setResult("NETWORK_ERROR");
        } finally {
            if (connection != null) connection.disconnect();
        }
        entry.setCheckedAt(clock.instant());
        repository.save(entry);
    }

    public List<ApiDailyIndex> findAll() {
        return repository.findByRepositoryInOrderByLangCodeAscRepositoryAscFormatAsc(properties.repositories())
                .stream().map(this::toApi).toList();
    }

    private ApiDailyIndex toApi(DailyIndexEntity entry) {
        String freshness = "UNKNOWN";
        if (entry.getLastModified() != null && !entry.getLastModified().isAfter(clock.instant())) {
            LocalDate today = LocalDate.now(clock.withZone(ZoneOffset.UTC));
            LocalDate modified = entry.getLastModified().atZone(ZoneOffset.UTC).toLocalDate();
            if (modified.equals(today)) freshness = "TODAY";
            else if (modified.equals(today.minusDays(1))) freshness = "YESTERDAY";
            else if (modified.isBefore(today.minusDays(1))) freshness = "OLD";
        }
        return new ApiDailyIndex().setRepository(entry.getRepository()).setLangCode(entry.getLangCode())
                .setFormat(entry.getFormat()).setLastModified(entry.getLastModified()).setCheckedAt(entry.getCheckedAt())
                .setHttpStatus(entry.getHttpStatus()).setResult(entry.getResult()).setFreshness(freshness)
                .setCheckOverdue(entry.getCheckedAt() == null || clock.instant()
                        .isAfter(entry.getCheckedAt().plus(properties.getDelay()).plusSeconds(60)));
    }
}
