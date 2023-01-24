import React, { useState, useEffect } from "react"
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { PropertyHouse } from "./svgindex";

const InboxSearchLinks = ({headerText, links, businessService, customClass="", logoIcon}) => {
    const { t } = useTranslation();
    const { roles: userRoles } = Digit.UserService.getUser().info;
    const [linksToShow, setLinksToShow] = useState([]);
    // const DynamicLogoIcon = lazy(() => import(`./${logoIcon}`));  //Needs a default export

    useEffect(() => {
      let linksToShow = links.filter(({ roles }) => roles.some((role) => userRoles.map(({ code }) => code).includes(role)) || !roles?.length);
        setLinksToShow(linksToShow);
    }, []);
    
    const renderHeader = () => <div className="header">
        <span className="logo">
           {logoIcon?.component === "PropertyHouse" && <PropertyHouse className={logoIcon?.customClass} />}
        </span>
        <span className="text">{t(headerText)}</span>
    </div>
    return (
        <div className={`inbox-search-links-container ${customClass}`}>
            {renderHeader()}
            <div className="contents">
                {linksToShow.map(({ url, text, hyperlink = false}, index) => {
                    return (
                    <span className="link" key={index}>
                        {hyperlink ? <a href={url}>{t(text)}</a> : <Link to={url}>{t(text)}</Link>}
                    </span>
                    );
                })}
            </div>
        </div>
    )
   
}

export default InboxSearchLinks;