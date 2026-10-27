package biz.icecat.icedatav2.repository;

import biz.icecat.icedatav2.models.entity.DailyIndexEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DailyIndexRepository extends JpaRepository<DailyIndexEntity, Long> {
    Optional<DailyIndexEntity> findByRepositoryAndLangCodeAndFormat(String repository, String langCode, String format);
    List<DailyIndexEntity> findByRepositoryInOrderByLangCodeAscRepositoryAscFormatAsc(List<String> repositories);
}
