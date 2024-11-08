{
<#compress>
    "batchId": "${request.batchId}",
    "serials": [
     <#list request.requestObjectsList as serials>
     {
            <#list serials?keys as key>
            	<#assign value = serials[key]>
            	<#if (key == "updatedExpiryDate")>
            		"${key}" : "${(value?datetime("dd-MM-yyyy")?string("yyyy-MM-dd"))!''}"<#sep>,
            	<#else>
            		"${key}" : "${(value)!''}"<#sep>,
            	</#if>
            </#list>
     }
     <#sep>,</#list>
   ]
</#compress>
}

