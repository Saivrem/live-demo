package biz.icecat.icedatav2.repository;

import biz.icecat.icedatav2.models.entity.LanguageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LanguagesRepository extends JpaRepository<LanguageEntity, Long> {
}
