package org.egov.works.measurement.util;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.egov.common.contract.models.AuditDetails;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.works.measurement.config.ErrorConfiguration;
import org.egov.works.measurement.repository.ServiceRequestRepository;
import org.egov.works.measurement.web.models.Measure;
import org.egov.works.measurement.web.models.Measurement;
import org.egov.works.measurement.web.models.MeasurementCriteria;
import org.egov.works.measurement.web.models.MeasurementRequest;
import org.egov.works.measurement.web.models.MeasurementResponse;
import org.egov.works.measurement.web.models.MeasurementSearchRequest;
import org.egov.works.measurement.web.models.Pagination;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MeasurementRegistryUtil {

    @Autowired
    private ErrorConfiguration errorConfigs;
    @Autowired
    private ServiceRequestRepository serviceRequestRepository;

    /**
     * Calculates the cumulative value based on the latest measurement and curr measurement details
     * @param latestMeasurement recent measurement in Measurement Book for the given contract Number
     * @param currMeasurement current measurement received as a part of create measurement request
     */
    public void calculateCumulativeValue(Measurement latestMeasurement,Measurement currMeasurement){
        Map<String, BigDecimal> targetIdtoCumulativeMap = new HashMap<>();
        for(Measure measure:latestMeasurement.getMeasures()){
            targetIdtoCumulativeMap.put(measure.getTargetId(),measure.getCumulativeValue());
        }
        for(Measure measure:currMeasurement.getMeasures()){
            measure.setCumulativeValue( targetIdtoCumulativeMap.get(measure.getTargetId()).add(measure.getCurrentValue()));
        }
    }

    /**
     * Search for measurement ...
     * @param searchCriteria
     * @param measurementSearchRequest
     * @return
     */
    public List<Measurement> searchMeasurements(MeasurementCriteria searchCriteria, MeasurementSearchRequest measurementSearchRequest) {

        handleNullPagination(measurementSearchRequest);
        if (StringUtils.isEmpty(searchCriteria.getTenantId()) || searchCriteria == null) {
            throw errorConfigs.tenantIdRequired;
        }
        List<Measurement> measurements = serviceRequestRepository.getMeasurements(searchCriteria, measurementSearchRequest);
        return measurements;
    }

    /**
     *
     * @param body
     */
    private void handleNullPagination(MeasurementSearchRequest body){
        if (body.getPagination() == null) {
            body.setPagination(new Pagination());
            body.getPagination().setLimit(null);
            body.getPagination().setOffSet(null);
            body.getPagination().setOrder(Pagination.OrderEnum.DESC);
        }
    }

    /**
     * re-calculate the cumulative value in case of update cumulative value request
     * @param latestMeasurement
     * @param currMeasurement
     */
    public void calculateCumulativeValueOnUpdate(Measurement latestMeasurement,Measurement currMeasurement){
        Map<String,BigDecimal> targetIdtoCumulativeMap = new HashMap<>();
        for(Measure measure:latestMeasurement.getMeasures()){
            targetIdtoCumulativeMap.put(measure.getTargetId(),measure.getCumulativeValue().subtract(measure.getCurrentValue()));
        }
        for(Measure measure:currMeasurement.getMeasures()){
            measure.setCumulativeValue( targetIdtoCumulativeMap.get(measure.getTargetId()).add(measure.getCurrentValue()));
        }
    }

    /**
     *
     * @param measurements
     * @param measurementRegistrationRequest
     * @return
     */
    public MeasurementResponse makeUpdateResponse(List<Measurement> measurements,MeasurementRequest measurementRegistrationRequest) {
        MeasurementResponse response = new MeasurementResponse();
        response.setResponseInfo(ResponseInfo.builder()
                .apiId(measurementRegistrationRequest.getRequestInfo().getApiId())
                .msgId(measurementRegistrationRequest.getRequestInfo().getMsgId())
                .ts(measurementRegistrationRequest.getRequestInfo().getTs())
                .status("successful")
                .build());
        response.setMeasurements(measurements);
        return response;
    }

    /**
     * Method to return auditDetails for create/update flows
     *
     * @param by
     * @param isCreate
     * @return AuditDetails
     */
    public AuditDetails getAuditDetails(String by, Measurement measurement, Boolean isCreate) {
        Long time = System.currentTimeMillis();
        if(isCreate)
            return AuditDetails.builder().createdBy(by).lastModifiedBy(by).createdTime(time).lastModifiedTime(time).build();
        else
            return AuditDetails.builder().createdBy(measurement.getAuditDetails().getCreatedBy()).lastModifiedBy(by)
                    .createdTime(measurement.getAuditDetails().getCreatedTime()).lastModifiedTime(time).build();
    }
}
