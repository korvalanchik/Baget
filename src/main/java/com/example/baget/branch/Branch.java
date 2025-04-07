package com.example.baget.branch;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "branches")
@Getter
@Setter
@NoArgsConstructor
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BranchNo")
    private Long branchNo;

    @Column(name = "BranchName", nullable = false, unique = true, length = 50)
    private String name;
}
