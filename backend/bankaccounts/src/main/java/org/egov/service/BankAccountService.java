package org.egov.service;


import lombok.extern.slf4j.Slf4j;
import org.egov.config.Configuration;
import org.egov.kafka.Producer;
import org.egov.repository.BankAccountRepository;
import org.egov.validator.BankAccountValidator;
import org.egov.web.models.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class BankAccountService {

    @Autowired
    private BankAccountValidator bankAccountValidator;

    @Autowired
    private EnrichmentService enrichmentService;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private Producer producer;

    @Autowired
    private Configuration configuration;

    public BankAccountRequest createBankAccount(BankAccountRequest bankAccountRequest) {
        log.info("BankAccountService::createBankAccount");
        bankAccountValidator.validateBankAccountOnCreate(bankAccountRequest);
        enrichmentService.enrichBankAccountOnCreate(bankAccountRequest);

        producer.push(configuration.getSaveBankAccountTopic(),bankAccountRequest);

        return bankAccountRequest;
    }

    /**
     * @param searchRequest
     * @return
     */
    public List<BankAccount> searchBankAccount(BankAccountSearchRequest searchRequest) {
        log.info("BankAccountService::searchBankAccount");
        bankAccountValidator.validateBankAccountOnSearch(searchRequest);
        enrichmentService.enrichBankAccountOnSearch(searchRequest);

        List<BankAccount> bankAccountList = bankAccountRepository.getBankAccount(searchRequest);
        if (!CollectionUtils.isEmpty(bankAccountList))
            return bankAccountList;

        return Collections.emptyList();
    }

    public Pagination countAllBankAccounts(BankAccountSearchRequest searchRequest) {
        log.info("BankAccountService::countAllBankAccounts");
        BankAccountSearchCriteria searchCriteria = searchRequest.getBankAccountDetails();
        Pagination pagination = searchRequest.getPagination();
        if (pagination == null) {
            pagination = new Pagination();
            searchRequest.setPagination(pagination);
        }

        searchCriteria.setIsCountNeeded(Boolean.TRUE);

        Integer count = bankAccountRepository.getBankAccountCount(searchRequest);
        pagination.setTotalCount(Double.valueOf(count));

        return pagination;
    }

    //TODO
    public BankAccountRequest updateBankAccount(BankAccountRequest bankAccountRequest) {
        log.info("BankAccountService::updateBankAccount");
        return bankAccountRequest;
    }
}