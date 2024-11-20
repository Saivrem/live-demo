package biz.icecat.icedatav2.models.entity;

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
@Table(name = "suppliers")
@Accessors(chain = true)
public class SupplierEntity {

    @Id
    @Column(name = "supplier_id", unique = true)
    private Long supplierId;
    @Column(name = "supplier_name")
    private String supplierName;
    @Column(name = "is_sponsor")
    private int isSponsor;
    @Column(name = "brand_logo")
    private String brandLogo;

    @OneToMany
    @JoinColumn(name = "supplier_id", referencedColumnName = "supplier_id")
    private List<SupplierMappingEntity> supplierMappingEntityList;
}
