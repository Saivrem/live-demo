package biz.icecat.icedatav2.models.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@Entity
@Table(name = "supplier_mappings", indexes = {
        @Index(name = "supplier_id", columnList = "supplier_id"),
        @Index(name = "m_supplier_name", columnList = "m_supplier_name")
})
public class SupplierMappingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id", unique = true)
    private long recordId;

    @Column(name = "supplier_id")
    private long supplierId;

    @Column(name = "m_supplier_name")
    private String mappedSupplierName;
}
