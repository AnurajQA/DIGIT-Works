import { Loader } from "@egovernments/digit-ui-react-components";
import React from "react";
import { useRouteMatch } from "react-router-dom";
import EmployeeApp from "./pages/employee";
import MeasurementCard from "./components/MeasurementCard";
import MeasureTable from "./components/MeasureTable";
import MeasureCard from "./components/MeasureCard";
import MeasureRow from "./components/MeasureRow";

const MeasurementModule = ({ stateCode, userType, tenants }) => {
    const { path, url } = useRouteMatch();
    const language = Digit.StoreData.getCurrentLanguage();
    const tenantId = Digit.ULBService.getCurrentTenantId();
    const moduleCode = ["Measurement", "common-masters", "workflow", tenantId];
    const { isLoading, data: store } = Digit.Services.useStore({
        stateCode,
        moduleCode,
        language,
    });


  if (isLoading) {
    return <Loader />;
  }

  return <EmployeeApp path={path} stateCode={stateCode} />;
};

const componentsToRegister = {
rementModule,
  MeasurementCard,
  MeasurementModule,
  MeasureCard,
  MeasureTable,
  MeasureRow,
};

export const initMeasurementComponents = () => {
  Object.entries(componentsToRegister).forEach(([key, value]) => {
    Digit.ComponentRegistryService.setComponent(key, value);
  });
};
