package com.example.baget.parts;

import com.example.baget.vendors.Vendors;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Entity
@Table(name = "parts")
@Getter
@Setter
public class Parts {

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "parts_generator")
    @TableGenerator(
            name = "parts_generator",
            table = "nextrecord",
            pkColumnName = "sequence_name",
            valueColumnName = "new_record",
            pkColumnValue = "parts_sequence",
            allocationSize = 10,
            initialValue = 10
    )
    private Long partNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VendorNo", nullable = true)
    private Vendors vendor;

    @Column(name = "\"description\"", length = 30)
    private String description;

    @Column
    private Double profilWidth;

    @Column
    private Double inQuality;

    @Column
    private Double onHand;

    @Column
    private Double onOrder;

    @Column
    private Double cost;

    @Column
    private Double listPrice;

    @Column
    private Double listPrice_1;

    @Column
    private Double listPrice_2;

    @Column
    private Integer noPercent;

    @Column
    private Double listPrice_3;

    @Version
    private Long version;

}
