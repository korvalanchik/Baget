package com.example.baget.items;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.util.Objects;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ItemId implements Serializable {

    @Column(name = "orderNo")
    private Long orderNo;

    @Column(name = "itemNo")
    private Long itemNo;

}
