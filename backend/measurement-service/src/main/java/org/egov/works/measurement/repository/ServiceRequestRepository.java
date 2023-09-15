package org.egov.works.measurement.repository;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.egov.tracer.model.ServiceCallException;
import org.egov.works.measurement.repository.querybuilder.MeasurementQueryBuilder;
import org.egov.works.measurement.repository.rowmapper.MeasurementRowMapper;
import org.egov.works.measurement.web.models.AuditDetails;
import org.egov.works.measurement.web.models.Measurement;
import org.egov.works.measurement.web.models.MeasurementCriteria;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static org.egov.works.measurement.config.ServiceConstants.*;

@Repository
@Slf4j
public class ServiceRequestRepository {

    private ObjectMapper mapper;
    @Autowired
    private MeasurementQueryBuilder queryBuilder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private RestTemplate restTemplate;

    @Autowired
    private MeasurementRowMapper rowMapper;


//    public List<Measurement> getMeasurements(MeasurementCriteria searchCriteria) {
//        List<Object> preparedStmtList = new ArrayList<>();
//        String query = queryBuilder.getMeasurementSearchQuery(searchCriteria, preparedStmtList);
//        return jdbcTemplate.query(query, preparedStmtList.toArray(), rowMapper);
//    }

    public ArrayList<Measurement> getMeasurements(MeasurementCriteria searchCriteria) {
        List<Object> preparedStmtList = new ArrayList<>();
        String query = queryBuilder.getMeasurementSearchQuery(searchCriteria, preparedStmtList);
        System.out.println("Query  ::::::::::: " + query);
        System.out.println("preparedStmtList  ::::::::::: " + preparedStmtList);
        ArrayList<Measurement> measurementsList = jdbcTemplate.query(query, preparedStmtList.toArray(), rowMapper);
//        List<Measurement> measurementsList = jdbcTemplate.query(query, preparedStmtList.toArray(), BeanPropertyRowMapper.newInstance(Measurement.class));
//        ArrayList<Measurement> measurements = new ArrayList<>();
//
//        for (ArrayList<Measurement> sublist : measurementsList) {
//            measurements.addAll(sublist);
//        }

        return measurementsList;
    }



    @Autowired
    public ServiceRequestRepository(ObjectMapper mapper, RestTemplate restTemplate) {
        this.mapper = mapper;
        this.restTemplate = restTemplate;
    }


    public Object fetchResult(StringBuilder uri, Object request) {
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        Object response = null;
        try {
            response = restTemplate.postForObject(uri.toString(), request, Map.class);
        } catch (HttpClientErrorException e) {
            log.error(EXTERNAL_SERVICE_EXCEPTION, e);
            throw new ServiceCallException(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error(SEARCHER_SERVICE_EXCEPTION, e);
        }

        return response;
    }
}