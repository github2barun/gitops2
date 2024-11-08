{
<#compress>
    "batchId": "${request.batchId}",
    "importType": "${request.importType}",
    "extraFields":{
         "parameters":{
             "FORCE_UPDATE":${request.forceUpdate}
         }
    },
    "resellers": [
    <#list request.requestObjectsList as resellerList>
    {
        "recordId": ${resellerList?counter},
        "dealerData" : {
            "address": {
            <#list resellerList.address?keys as key>
                "${key}" : "${resellerList.address[key]}"<#if key_has_next>,</#if>
            </#list>
            },
            "extraParams": {
            "parameters": {
            <#list resellerList.extraParams?keys as key>
                "${key}" : "${resellerList.extraParams[key]}"<#if key_has_next>,</#if>
            </#list>
            }
            },
            "user": {
            <#list resellerList.user?keys as key>
                "${key}" : "${resellerList.user[key]}"<#if key_has_next>,</#if>
            </#list>
            },
            <#list resellerList.fields?keys as key>
                "${key}" : "${resellerList.fields[key]}"<#if key_has_next>,</#if>
            </#list>
        },

        "dealerPrincipal" : {
            "id": "${resellerList.fields['resellerId']}",
            "type": "RESELLERID"
        }
    }<#if resellerList_has_next>,</#if>
    </#list>
    ]
</#compress>
}