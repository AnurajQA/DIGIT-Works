package org.egov.works.measurement.repository.rowmapper;



import org.egov.works.measurement.web.models.AuditDetails;
import org.egov.works.measurement.web.models.Document;
import org.egov.works.measurement.web.models.*;
import org.egov.works.measurement.web.models.Measure;
import org.egov.works.measurement.web.models.Measurement;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Component
public class MeasurementRowMapper implements ResultSetExtractor<ArrayList<Measurement>> {


    @Override
    public ArrayList<Measurement> extractData(ResultSet rs) throws SQLException, DataAccessException {
        Map<String, Measurement> measurementMap = new LinkedHashMap<>();
        System.out.println("Records : " + rs.getFetchSize());
        while (rs.next()) {
            String uuid = rs.getString("id");
            Measurement measurement = measurementMap.get(uuid);

            System.out.println("id: " + rs.getString("id"));
            System.out.println("tenantId: " + rs.getString("tenantId"));

            if (measurement == null) {
                // Create a new Measurement object
                measurement = new Measurement();
                measurement.setId(UUID.fromString(uuid)); // Assuming you have a UUID field
                measurement.setTenantId(rs.getString("tenantId"));
                measurement.setMeasurementNumber(rs.getString("mbNumber"));
                measurement.setPhysicalRefNumber(rs.getString("phyRefNumber"));
                measurement.setReferenceId(rs.getString("referenceId"));
                measurement.setEntryDate(rs.getBigDecimal("entryDate"));
                measurement.setIsActive(rs.getBoolean("isActive"));

                AuditDetails auditDetails = new AuditDetails();
                auditDetails.setCreatedBy(rs.getString("createdby"));
                auditDetails.setCreatedTime(rs.getLong("createdtime"));
                auditDetails.setLastModifiedBy(rs.getString("lastmodifiedby"));
                auditDetails.setLastModifiedTime(rs.getLong("lastmodifiedtime"));

                measurement.setAuditDetails(auditDetails);

                measurement.setAdditionalDetails(rs.getString("additionalDetails"));
                measurement.setMeasures(new ArrayList<>());
                measurement.setDocuments(new ArrayList<>());

                measurementMap.put(uuid, measurement);
            }

            // Created a Measure object and add it to the Measurement
            Measure measure = new Measure();

            measure.setId(UUID.fromString(rs.getString("mmid"))); // Assuming you have a UUID field for measures
            measure.setReferenceId(rs.getString("mdreferenceId"));
            measure.setLength(rs.getBigDecimal("mmlength"));
            measure.setBreadth(rs.getBigDecimal("mmbreadth"));
            measure.setHeight(rs.getBigDecimal("mmheight"));
            measure.setNumItems(rs.getBigDecimal("mmnumOfItems"));
            measure.setCurrentValue(rs.getBigDecimal("mmcurrentValue"));
            measure.setCumulativeValue(rs.getBigDecimal("mmcumulativeValue"));
            measure.setTargetId(rs.getString("targetId"));
            measure.setIsActive(rs.getBoolean("mdisActive"));

            AuditDetails auditDetails = new AuditDetails();
            auditDetails.setCreatedBy(rs.getString("createdby"));
            auditDetails.setCreatedTime(rs.getLong("createdtime"));
            auditDetails.setLastModifiedBy(rs.getString("lastmodifiedby"));
            auditDetails.setLastModifiedTime(rs.getLong("lastmodifiedtime"));

            measure.setAuditDetails(auditDetails);

            // Add the Measure to the Measurement
            measurement.getMeasures().add(measure);

            // Retrieve and set document details
            Document document = new Document();

            document.setId(rs.getString("dcid")); // Assuming you have a UUID field for documents
            document.setDocumentType(rs.getString("documentType"));
            document.setFileStore(rs.getString("filestore"));
            document.setDocumentUid(rs.getString("documentuuid"));
            document.setAdditionalDetails(rs.getString("additionalDetails")); // Adjust the column name as per your table


            // Add the document to the Measurement
            measurement.getDocuments().add(document);
        }
        // Return the list of unique Measurement objects
        return new ArrayList<>(measurementMap.values());
    }
}