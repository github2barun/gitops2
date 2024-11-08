{
<#compress>
    "batchId": "${request.batchId}",
    "importType": "${request.importType}",
    "extraFields":{
         "parameters":{
             "FORCE_UPDATE":${request.forceUpdate}
         }
    },
    "status": "",
    "inventories": [
    <#list request.requestObjectsList as inventList>  
        {
        <#list inventList.fields?keys as key>
            "${key}" : "${inventList.fields[key]}",
        </#list>
	        
		<#if inventList.ranges?has_content>        
	        "ranges" : [ {
	        <#list inventList.ranges?keys as key>
	            "${key}" : "${inventList.ranges[key]}"<#if key_has_next>,</#if>
	        </#list>
	        } ]<#if inventList.serials?has_content>,</#if>
		</#if>
		<#if inventList.serials?has_content>        
	        "serials" : [
	        <#list inventList.serials?keys as key>
	            "${inventList.serials[key]}"<#if key_has_next>,</#if>
	        </#list>
	        ]
		</#if>
        }<#if inventList?has_next>,</#if>
    </#list>
    ]
</#compress>
}