package com.salessystem.catalogservice.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name="suppliers")
public class Supplier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, unique = true, length = 20)
    private String tax_id;

    @Column(length = 100)
    private String contact_email;

    @Column(length = 15)
    private String phone_number;

    @Column(insertable = false, updatable = false)
    private LocalDateTime created_at;

    public Supplier() {
    }

    public Supplier(String name, String tax_id, String contact_email, String phone_number) {
        this.name = name;
        this.tax_id = tax_id;
        this.contact_email = contact_email;
        this.phone_number = phone_number;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTax_id() {
        return tax_id;
    }

    public void setTax_id(String tax_id) {
        this.tax_id = tax_id;
    }

    public String getContact_email() {
        return contact_email;
    }

    public void setContact_emails(String contact_email) {
        this.contact_email = contact_email;
    }

    public String getPhone_number() {
        return phone_number;
    }

    public void setPhone_number(String phone_number) {
        this.phone_number = phone_number;
    }

    public LocalDateTime getCreated_at() {
        return created_at;
    }
}
