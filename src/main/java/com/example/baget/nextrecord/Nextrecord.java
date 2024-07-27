package com.example.baget.nextrecord;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "nextrecord")
public class Nextrecord {

    @Id
    @Column(name = "sequence_name")
    private String sequenceName;

    @Column(name = "new_record")
    private Long newRecord;

}
