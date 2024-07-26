package com.example.baget.nextpart;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;


@Entity
@Getter
@Setter
@Table(name = "nextpart")
public class Nextpart {

    @Id
    private String sequence_name;

    private Long newPart;

}
