package com.example.baget.parts;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ProfilListDTO {
    @Size(max = 30)
    private String description;

    private Double profilWidth;

    private Double onHand;

    private Double listPrice_1;

    private Double listPrice_2;

    private Double listPrice_3;
}
