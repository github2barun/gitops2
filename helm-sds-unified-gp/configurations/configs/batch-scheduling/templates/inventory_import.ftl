{
<#compress>
    "inventories": [
    <#list request.requestObjectsList as inventList>
        {
        "data" : {
        <#list inventList.data?keys as key>
            "${key}" : "${inventList.data[key]}"<#if key_has_next>,</#if>
        </#list>
        },
        <#list inventList.fields?keys as key>
            <#if key == "quantity" || key == "startNo" || key == "endNo">
                "${key}" : ${inventList.fields[key]}<#if key_has_next>,</#if>
            <#else>
                "${key}" : "${inventList.fields[key]}"<#if key_has_next>,</#if>
            </#if>
        </#list>
        }<#if inventList?has_next>,</#if>
    </#list>
    ],
    "batchId": "${request.batchId}",
    "importType": "${request.importType}",
    "extraFields":{
         "parameters":{
             "FORCE_UPDATE":${request.forceUpdate}
         }
    }
</#compress>
}