package biz.icecat.icedatav2.persistence.repository;

import biz.icecat.icedatav2.persistence.entity.SupplierMappingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupplierMappingRepository extends JpaRepository<SupplierMappingEntity, Long> {
}