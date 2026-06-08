package com.cvsearch.company;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Company {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

    @Column(unique = true)
    private Long organizationNumber;

	private String name;
	private String website;
	private String location;

    public Company() {

    }

    public Company(String name, String website, String location, Long organizationNumber) {
        this.organizationNumber = organizationNumber;
        this.name = name;
        this.website = website;
        this.location = location;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrganizationNumber() {
        return this.organizationNumber;
    }

    public void setOrganizationNumber(Long organizationNumber) {
        this.organizationNumber = organizationNumber;
    }

    public String getName() {
        return this.name;
    }

    public String getWebsite() {
        return this.website;
    }

    public String getLocation() {
        return this.location;
    }

    public void setName(String newName) {
        this.name = newName;
    }

    public void setWebsite(String newWebsite) {
        this.website = newWebsite;
        
    }

    public void setLocation(String newLocation) {
        this.location = newLocation;

    }
}
