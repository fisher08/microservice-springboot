package com.rkdeep.license.service;


import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import com.rkdeep.license.config.ServiceConfig;
import com.rkdeep.license.model.License;
import com.rkdeep.license.model.Organization;
import com.rkdeep.license.repository.LicenseRepository;
import com.rkdeep.license.service.client.OrganizationDiscoveryClient;
import com.rkdeep.license.service.client.OrganizationFeignClient;
import com.rkdeep.license.service.client.OrganizationRestTemplateClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Service
public class LicenseService {

    Logger logger = LoggerFactory.getLogger(LicenseService.class);
    @Autowired
    MessageSource messages;

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    ServiceConfig config;

    @Autowired
    OrganizationFeignClient organizationFeignClient;

    @Autowired
    OrganizationRestTemplateClient organizationRestClient;

    @Autowired
    OrganizationDiscoveryClient organizationDiscoveryClient;

    public License getLicense(String licenseId, String organizationId, String clientType){
        License license = licenseRepository.findByOrganizationIdAndLicenseId(organizationId, licenseId);
        if (null == license) {
            throw new IllegalArgumentException(String.format(messages.getMessage("license.search.error.message", null, null),licenseId, organizationId));
        }

        Organization organization = retrieveOrganizationInfo(organizationId, clientType);
        if (null != organization) {
            license.setOrganizationName(organization.getName());
            license.setContactName(organization.getContactName());
            license.setContactEmail(organization.getContactEmail());
            license.setContactPhone(organization.getContactPhone());
        }

        return license.withComment(config.getProperty());
    }

    private Organization retrieveOrganizationInfo(String organizationId, String clientType) {
        Organization organization = null;

        switch (clientType) {
            case "feign":
                System.out.println("I am using the feign client");
                organization = organizationFeignClient.getOrganization(organizationId);
                break;
            case "rest":
                System.out.println("I am using the rest client");
                organization = organizationRestClient.getOrganization(organizationId);
                break;
            case "discovery":
                System.out.println("I am using the discovery client");
                organization = organizationDiscoveryClient.getOrganization(organizationId);
                break;
            default:
                organization = organizationRestClient.getOrganization(organizationId);
        }

        return organization;
    }

    public License createLicense(License license){
        license.setLicenseId(UUID.randomUUID().toString());
        licenseRepository.save(license);

        return license.withComment(config.getProperty());
    }

    public License updateLicense(License license){
        licenseRepository.save(license);

        return license.withComment(config.getProperty());
    }

    public String deleteLicense(String licenseId){
        String responseMessage = null;
        License license = new License();
        license.setLicenseId(licenseId);
        licenseRepository.delete(license);
        responseMessage = String.format(messages.getMessage("license.delete.message", null, null),licenseId);
        return responseMessage;

    }

    private void randomlyRunLong() throws TimeoutException {
        Random rand = new Random();
        int randomNum = rand.nextInt(3) + 1;
        if (randomNum==3) sleep();
    }
    private void sleep() throws TimeoutException {
        try {
            Thread.sleep(5000);
            throw new java.util.concurrent.TimeoutException();
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
        }
    }
    @CircuitBreaker(name = "licenseService")
    public List<License> getLicensesByOrganization(String organizationId) throws TimeoutException {
        randomlyRunLong();
        return licenseRepository.findByOrganizationId(organizationId);
    }
}