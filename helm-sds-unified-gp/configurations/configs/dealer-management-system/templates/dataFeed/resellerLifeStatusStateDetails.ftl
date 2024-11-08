"DMS":
[<#list response.resellerDataTransactions as response>
    {
    "resultCode": ${response.resultCode?string.computer!400},
    "resultDescription": "${response.resultDescription!""}"
    <#if response.resellerData??>,
        "resellerInfo": {
        "reseller": {
        "resellerId": "${response.resellerData.resellerId!""}",
        "resellerName": "${response.resellerData.resellerName!""}",
        "resellerMSISDN": "${response.resellerData.resellerMSISDN!""}",
        "resellerType": "${response.resellerData.resellerTypeId!""}",
        "resellerParentId": "${response.resellerData.parentResellerId!""}",
        "resellerPath": "${response.resellerData.resellerPath!""}"
        <#if response.ftlResultChangesMap??>,
            "resellerUpdates":{<#list response.ftlResultChangesMap as key, vals>
            "${(key)!'N/A'}":
            {<#list vals as statusKey, statusValue>
                "${(statusKey)!'N/A'}": "${(statusValue)!'N/A'}"

                <#sep>,
            </#list>}
            <#sep>,
        </#list>}
        </#if>
        }
        }
    </#if>
    }
    <#sep>,
</#list>]