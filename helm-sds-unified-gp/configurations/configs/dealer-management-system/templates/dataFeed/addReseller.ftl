"DMS": [{
    "resultCode": ${response.resultCode?string.computer!400},
    "resultDescription": "${response.resultDescription!""}"
    <#if response.resellerInfo??>,
    "resellerInfo": {
        "reseller": {
            "resellerId": "${response.resellerInfo.reseller.resellerId!""}",
            "resellerName": "${response.resellerInfo.reseller.resellerName!""}",
            "resellerJuridicalName": "${(response.resellerInfo.reseller.resellerJuridicalName)!""}",
            "distributorId": "${response.resellerInfo.reseller.distributorId!""}",
            "contractId": "${response.resellerInfo.reseller.contractId!""}",
            "resellerMSISDN": "${response.resellerInfo.reseller.resellerMSISDN!""}",
            "parentResellerId": "${response.resellerInfo.reseller.parentResellerId!""}",
            "parentResellerName": "${response.resellerInfo.reseller.parentResellerName!""}",
            "resellerPath": "${response.resellerInfo.reseller.resellerPath!""}",
            "resellerTypeId": "${response.resellerInfo.reseller.resellerTypeId!""}",
            "resellerTypeName": "${response.resellerInfo.reseller.resellerTypeName!""}",
            "status": "${response.resellerInfo.reseller.status!""}",
            "region": "${response.resellerInfo.reseller.extraParams.get("region")!""}"
            <#if response.resellerInfo.reseller.address??>,
            "address": {
                         "street":"${response.resellerInfo.reseller.address.street!""}",
                         "zip":"${response.resellerInfo.reseller.address.zip!""}",
                         "city":"${response.resellerInfo.reseller.address.city!""}",
                         "country":"${response.resellerInfo.reseller.address.country!""}",
                         "email":"${response.resellerInfo.reseller.address.email!""}"
                }
            </#if>
        }
    <#if response.resellerInfo.users??>,
    "users":[<#list response.resellerInfo.users as user>
       {
           "id":"${(user.userId)!'N/A'}",
           "roleName":"${(user.roleName)!'N/A'}",
           "roleId":"${(user.roleId)!'N/A'}",
           "roleStartDate":"${(user.fields.parameters.roleStartDate)!'N/A'}",
           "roleExpiryDate":"${(user.fields.parameters.roleExpiryDate)!'N/A'}"
       }
       <#sep>,
       </#list>]
       </#if>
       <#if response.resellerInfo.reseller.extraParams.parameters??>,
       "additionalFields":[<#list response.resellerInfo.reseller.extraParams.parameters?keys as key>
       {
            "name":"${(key)!'N/A'}",
            "value":"${(response.resellerInfo.reseller.extraParams.parameters[key])!'N/A'}"
       }
       <#sep>,
       </#list>]
       </#if>
    }
    </#if>
}]