{
<#compress>
    "inventories": [
    <#list request.requestObjectsList as inventList>
        {
        "data" : {
        <#assign add_comma = false>
        <#list inventList.data?keys as key>
        <#if inventList.data[key]?has_content>
        <#if add_comma>,</#if>
            "${key}" : "${inventList.data[key]}"
            <#assign add_comma = true>
            </#if>
        </#list>
        },
        <#list inventList.fields?keys as key>
            "${key}" : "${inventList.fields[key]}"<#if key_has_next>,</#if>
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