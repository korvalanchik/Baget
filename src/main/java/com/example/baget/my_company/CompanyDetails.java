package com.example.baget.my_company;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "company_details")
@Getter
@Setter
public class CompanyDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;
    private String initials;
    private String edrpou;
    private String phone;
    private String bankAccount;
    private String ipn;
    private String address;
    private String comment;
    private String workTitle;
    private String defaultRecipient;
}
