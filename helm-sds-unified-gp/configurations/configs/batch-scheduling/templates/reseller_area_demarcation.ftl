{
<#compress>
    "batchId": "${request.batchId}",
    "importType": "${request.importType}",
    <#list request.requestObjectsList as row>
        "principalId": {
            "id": "${row.msisdn}",
            "type": "RESELLERMSISDN"
        },
        "principalResellerType" : "${row.resellerType}",
        "toBeParent" : "${row.toBeParent}",
        "toBeOwner" : "${row.toBeOwner}",
        "toBeRegionCode" : "${row.toBeRegionCode}",
        "toBeClusterCode" : "${row.toBeClusterCode}",
        "toBeTerritoryCode" : "${row.toBeTerritoryCode}",
        "toBeThanaCode" : "${row.toBeThanaCode}"
    </#list>
</#compress>
}