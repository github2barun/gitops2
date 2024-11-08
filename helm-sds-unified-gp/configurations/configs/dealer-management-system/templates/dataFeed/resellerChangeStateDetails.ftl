"DMS":
[<#list response.resellerDataTransactions as response>
{
    "resultCode": ${response.resultCode?string.computer!400},
    "resultDescription": "${response.resultDescription!""}",
    "batchId": "${response.batchId!""}"
    <#if response.resellerData??>,
        "resellerId": "${response.resellerData.resellerId!""}",
        "resellerName": "${response.resellerData.resellerName!""}",
        "resellerMSISDN": "${response.resellerData.resellerMSISDN!""}",
        "resellerType": "${response.resellerData.resellerTypeId!""}",
        "resellerParentId": "${response.resellerData.parentResellerId!""}",
        "resellerStatus": "${response.resellerData.status!""}",
        "resellerPath": "${response.resellerData.resellerPath!""}"}
    </#if>
    <#sep>,
</#list>]