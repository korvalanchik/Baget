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
//@EqualsAndHashCode
public class ItemId implements Serializable {

//    @Column(name = "orderNo")
    private Long orderNo;

//    @Column(name = "itemNo")
    private Long itemNo;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItemId itemId = (ItemId) o;
        return Objects.equals(orderNo, itemId.orderNo) &&
                Objects.equals(itemNo, itemId.itemNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderNo, itemNo);
    }

}
