"DMS": {
    "resultCode": ${response.resultCode?string.computer!400},
    "resultDescription": "${response.resultDescription!""}",
    "userId": "${response.userId!""}"
<#if response.resellerData??>,
    "resellerId": "${(response.resellerData.resellerId)!""}",
    "resellerName": "${(response.resellerData.resellerName)!""}",
    "distributorId": "${(response.resellerData.distributorId)!""}",
    "contractId": "${(response.resellerData.contractId)!""}",
    "resellerMSISDN": "${(response.resellerData.resellerMSISDN)!""}",
    "newParentResellerId": "${(response.resellerData.parentResellerId)!""}",
    "newParentResellerName": "${(response.resellerData.parentResellerName)!""}",
    "resellerPath": "${(response.resellerData.resellerPath)!""}",
    "resellerTypeId": "${(response.resellerData.resellerTypeId)!""}",
    "resellerTypeName": "${(response.resellerData.resellerTypeName)!""}",
    "status": "${(response.resellerData.status)!""}"
</#if>
}