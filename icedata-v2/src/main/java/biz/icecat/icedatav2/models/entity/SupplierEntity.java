package biz.icecat.icedatav2.models.entity;

import biz.icecat.icedatav2.annotation.CsvMapping;
import biz.icecat.icedatav2.annotation.CsvRow;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.OneToMany;
import jakarta.persistence.JoinColumn;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

@Getter
@Setter
@Entity
@CsvRow
@Table(name = "suppliers")
@Accessors(chain = true)
public class SupplierEntity {

    @Id
    @Column(name = "supplier_id", unique = true)
    @CsvMapping(name = "supplier_id", ordinal = 0)
    private Long supplierId;
    @Column(name = "supplier_name")
    @CsvMapping(name = "supplier_name", ordinal = 1)
    private String supplierName;
    @Column(name = "is_sponsor")
    @CsvMapping(name = "is_sponsor", ordinal = 2)
    private int isSponsor;
    @Column(name = "brand_logo")
    @CsvMapping(name = "brand_logo", ordinal = 3)
    private String brandLogo;

    @OneToMany
    @JoinColumn(name = "supplier_id", referencedColumnName = "supplier_id")
    private List<SupplierMappingEntity> supplierMappingEntityList;
}
