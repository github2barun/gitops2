{
<#compress>
    "batchId": "${request.batchId}",
    "serials": [
     <#list request.requestObjectsList as serials>
     {
            <#list serials?keys as key>
                 "${key}" : "${(serials[key])}"<#sep>,
            </#list>
     }
     <#sep>,</#list>
   ]
</#compress>
}