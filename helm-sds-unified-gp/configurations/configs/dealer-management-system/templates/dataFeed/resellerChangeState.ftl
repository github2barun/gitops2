"DMS": {
    "resultCode": ${response.resultCode?string.computer!400},
    "resultDescription": "${response.resultDescription!""}",
<#if response.resellerData??>,
    "posId": "${response.resellerData.resellerId!""}",
    "posName": "${response.resellerData.resellerName!""}",
    "posParent": "${response.resellerData.parentResellerId!""}",
    "posPath": "${response.resellerData.resellerPath!""}",
    <#if response.resellerData.extraParams??>,
        "region": "${response.resellerData.extraParams.region!""}",
    </#if>
    "posStatus": "${response.resellerData.status!""}",
</#if>
    "batchId": "${response.batchId!""}"
    <#if response.resultDetails??>,
    "resultDetails":[<#list response.resultDetails?keys as key>
        {
           "${(key)!'N/A'}":"${(response.resultDetails[key])!'N/A'}"
        }
       <#sep>,
       </#list>]
    </#if>
}