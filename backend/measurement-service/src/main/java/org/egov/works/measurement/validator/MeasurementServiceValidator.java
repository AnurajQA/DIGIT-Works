package org.egov.works.measurement.validator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;
import org.egov.tracer.model.CustomException;
import org.egov.works.measurement.config.ErrorConfiguration;
import org.egov.works.measurement.config.MBServiceConfiguration;
import org.egov.works.measurement.repository.ServiceRequestRepository;
import org.egov.works.measurement.service.WorkflowService;
import org.egov.works.measurement.util.ContractUtil;
import org.egov.works.measurement.util.MdmsUtil;
import org.egov.works.measurement.util.MeasurementRegistryUtil;
import org.egov.works.measurement.util.MeasurementServiceUtil;
import org.egov.works.measurement.web.models.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.egov.works.measurement.config.ServiceConstants.MDMS_TENANTS_MASTER_NAME;
import static org.egov.works.measurement.config.ServiceConstants.MDMS_TENANT_MODULE_NAME;

@Component
@Slf4j
public class MeasurementServiceValidator {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ContractUtil contractUtil;

    @Autowired
    private MBServiceConfiguration MBServiceConfiguration;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private ErrorConfiguration errorConfigs;

    @Autowired
    private ServiceRequestRepository serviceRequestRepository;


    @Autowired
    private MeasurementRegistryUtil measurementRegistryUtil;

    @Autowired
    private MeasurementServiceUtil measurementServiceUtil;
    @Autowired
    private MdmsUtil mdmsUtil;
    @Autowired
    private ObjectMapper objectMapper;


    @Value("${egov.filestore.host}")
    private String baseFilestoreUrl;

    @Value("${egov.filestore.endpoint}")
    private String baseFilestoreEndpoint;


    public void validateTenantId(MeasurementServiceRequest measurementRequest){
        Set<String> validTenantSet = new HashSet<>();
        List<String> masterList = Collections.singletonList(MDMS_TENANTS_MASTER_NAME);
        Map<String, Map<String, JSONArray>> response = mdmsUtil.fetchMdmsData(measurementRequest.getRequestInfo(), MBServiceConfiguration.getStateLevelTenantId(), MDMS_TENANT_MODULE_NAME,masterList);
        String node = response.get(MDMS_TENANT_MODULE_NAME).get(MDMS_TENANTS_MASTER_NAME).toString();
        try {
            JsonNode currNode = objectMapper.readTree(node);
            for (JsonNode tenantNode : currNode) {
                // Assuming each item in the array has a "code" field
                String tenantId = tenantNode.get("code").asText();
                validTenantSet.add(tenantId);
            }
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        List<MeasurementService> measurementList = measurementRequest.getMeasurements();
        for(int i=0;i<measurementList.size();i++){
            if(!validTenantSet.contains(measurementList.get(i).getTenantId())){
                throw new CustomException("TENANT_ID_NOT_FOUND",measurementList.get(i).getTenantId()+" Tenant Id is Not found");
            }
        }
    }
    public void validateContracts(MeasurementServiceRequest measurementServiceRequest) {
        measurementServiceRequest.getMeasurements().forEach(measurement -> {
            // validate contracts
            Boolean isValidContract = contractUtil.validContract(measurement, measurementServiceRequest.getRequestInfo());
            contractUtil.validateByReferenceId(measurementServiceRequest);
            if (!isValidContract) {
                throw errorConfigs.invalidContract;
            }
        });
    }
    public void validateContractsOnUpdate(MeasurementServiceRequest measurementServiceRequest) {
        measurementServiceRequest.getMeasurements().forEach(measurement -> {
            // validate contracts
            Boolean isValidContract = contractUtil.validContract(measurement, measurementServiceRequest.getRequestInfo());
            if (!isValidContract) {
                throw errorConfigs.invalidContract;
            }
        });
    }

    public void validateExistingServiceDataAndEnrich(MeasurementServiceRequest measurementServiceRequest) {
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        List<String> mbNumbers = new ArrayList<>();
        Map<String, MeasurementService> mbNumberToServiceMap = new HashMap<>();

        for (MeasurementService measurementService : measurementServiceRequest.getMeasurements()) {
            mbNumbers.add(measurementService.getMeasurementNumber());
        }

        List<MeasurementService> existingMeasurementService = serviceRequestRepository.getMeasurementServicesFromMBSTable(namedParameterJdbcTemplate, mbNumbers);
        enrichMeasurementServiceWithMeasurement(existingMeasurementService,measurementServiceRequest);
        if(existingMeasurementService.size()!=measurementServiceRequest.getMeasurements().size()){
            throw errorConfigs.measurementServiceDataNotExist;
        }

        // if wfStatus is rejected then throw error
        checkDataRejected(existingMeasurementService);

        // Create a map to associate mbNumbers with corresponding MeasurementRegistry objects
        for (MeasurementService existingService : existingMeasurementService) {
            mbNumberToServiceMap.put(existingService.getMeasurementNumber(), existingService);
        }
        workflowService.changeDataAccordingToWfActions(measurementServiceRequest,mbNumberToServiceMap); // Changing update data according to workflow actions
        List<MeasurementService> orderedExistingMeasurementService = createOrderedMeasurementServiceList(mbNumbers, mbNumberToServiceMap); //ordering measurementServices
        matchIdsAndMbNumber(orderedExistingMeasurementService,measurementServiceRequest.getMeasurements()); // Match ids and measurement Numbers
        setServiceAuditDetails(orderedExistingMeasurementService, measurementServiceRequest); // Set audit details for orderedExistingMeasurementService
    }

    public void enrichMeasurementServiceWithMeasurement(List<MeasurementService> existingMeasurementServices,MeasurementServiceRequest measurementServiceRequest){
        MeasurementCriteria criteria=MeasurementCriteria.builder().ids(new ArrayList<>()).tenantId(existingMeasurementServices.get(0).getTenantId()).build();
        for(MeasurementService measurementService:existingMeasurementServices){
            criteria.getIds().add(measurementService.getId());
        }
        MeasurementSearchRequest measurementSearchRequest=MeasurementSearchRequest.builder().requestInfo(measurementServiceRequest.getRequestInfo()).criteria(criteria).build();
        List<Measurement> existingMeasurements=measurementRegistryUtil.searchMeasurements(measurementSearchRequest).getBody().getMeasurements();
        //Create a map that stores existingMeasurements as it is attached with existingMeasurementServices measurementNumber
        Map<String, Measurement> measurementMap = new HashMap<>();

        // Populate the map
        for (Measurement existingMeasurement : existingMeasurements) {
            measurementMap.put(existingMeasurement.getMeasurementNumber(), existingMeasurement);
        }
        List<Measurement> orderedExistingMeasurements=new ArrayList<>();
        for(MeasurementService measurementService:existingMeasurementServices){
            orderedExistingMeasurements.add(measurementMap.get(measurementService.getMeasurementNumber()));
        }
        List<MeasurementService> measurementServices=measurementServiceUtil.convertToMeasurementServiceList(measurementServiceRequest,orderedExistingMeasurements);
        // Ordering existing MeasurementServices
        for(int i=0;i<existingMeasurements.size();i++){
            measurementServices.get(i).setWfStatus(existingMeasurementServices.get(i).getWfStatus());
            existingMeasurementServices.set(i,measurementServices.get(i));
        }
    }

    public void checkDataRejected(List<MeasurementService> existingMeasurementService){
        for(MeasurementService measurementService:existingMeasurementService){
            if(measurementService.getWfStatus().equals(MBServiceConfiguration.rejectedStatus)){
                throw errorConfigs.rejectedError(measurementService.getMeasurementNumber());
            }
        }
    }


    public void matchIdsAndMbNumber(List<MeasurementService> orderedExistingMeasurementService, List<MeasurementService> measurementServices) {
        List<String> existingIds = orderedExistingMeasurementService.stream()
                .map(MeasurementService::getId)
                .collect(Collectors.toList());

        List<String> existingMeasurementNumbers = orderedExistingMeasurementService.stream()
                .map(MeasurementService::getMeasurementNumber)
                .collect(Collectors.toList());

        List<String> newIds = measurementServices.stream()
                .map(MeasurementService::getId)
                .collect(Collectors.toList());

        List<String> newMeasurementNumbers = measurementServices.stream()
                .map(MeasurementService::getMeasurementNumber)
                .collect(Collectors.toList());

        if (!existingIds.equals(newIds) || !existingMeasurementNumbers.equals(newMeasurementNumbers)) {
            throw errorConfigs.idsAndMbNumberMismatch;
        }
    }


    public List<MeasurementService> createOrderedMeasurementServiceList(List<String> mbNumbers, Map<String, MeasurementService> mbNumberToServiceMap) {
        List<MeasurementService> orderedExistingMeasurementService = new ArrayList<>();

        // Populate orderedExistingMeasurementService to match the order of mbNumbers
        for (String mbNumber : mbNumbers) {
            MeasurementService existingService = mbNumberToServiceMap.get(mbNumber);
            orderedExistingMeasurementService.add(existingService);
        }

        return orderedExistingMeasurementService;
    }

    public void setServiceAuditDetails(List<MeasurementService> measurementServiceExisting,MeasurementServiceRequest measurementServiceRequest){
        List<MeasurementService> measurementServices=measurementServiceRequest.getMeasurements();
        for(int i=0;i<measurementServiceExisting.size();i++){
            measurementServices.get(i).setAuditDetails(measurementServiceExisting.get(i).getAuditDetails());
            measurementServices.get(i).getAuditDetails().setLastModifiedBy(measurementServiceRequest.getRequestInfo().getUserInfo().getUuid());
            measurementServices.get(i).getAuditDetails().setLastModifiedTime(System.currentTimeMillis());
            for(Measure measure:measurementServices.get(i).getMeasures()){
                measure.setAuditDetails(measurementServiceExisting.get(i).getAuditDetails());
            }
        }
        measurementServiceRequest.setMeasurements(measurementServices);
    }

    public boolean checkDocumentIdsMatch(List<String> documentIds, String responseJson) {
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            Map<String, String> fileStoreIds = objectMapper.readValue(responseJson, Map.class);

            for (String documentId : documentIds) {
                if (!fileStoreIds.containsKey(documentId)) {
                    return false; // At least one document ID was not found
                }
            }

            return true; // All document IDs were found
        } catch (IOException e) {
            e.printStackTrace();
            return false; // Error occurred while parsing the response
        }
    }

}
