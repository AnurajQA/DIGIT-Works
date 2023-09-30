import React from "react";
import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";

export const statusBasedNavigation = ( status, contractNumber, measurementNumber, tenantId, value ) => {
    const { t } = useTranslation();

    let linkTo = `/${window?.contextPath}/employee/measurement/update?tenantId=${tenantId}&workOrderNumber=${contractNumber}&mbNumber=${measurementNumber}`;

    if (status !== "DRAFTED") {
        linkTo = `/${window?.contextPath}/employee/measurement/view?tenantId=${tenantId}&workOrderNumber=${contractNumber}&mbNumber=${measurementNumber}`;
    }

    return (
        <Link to={linkTo}>
            {value ? value : t("ES_COMMON_NA")}
        </Link>
    );
};