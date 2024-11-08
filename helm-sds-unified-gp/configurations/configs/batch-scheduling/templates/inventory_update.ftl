{
<#compress>
    "batchId": "${request.batchId}",
    "status": "${request.status}",
    "importType": "${request.importType}",
    "extraFields":{
         "parameters":{
             "FORCE_UPDATE":${request.forceUpdate}
         }
    },
    "inventories": [
<#list request.cxRecordList as cxRecord>
    {
    	"serials": [
    		"${cxRecord.msisdn}"
        ]
    }, 
	    {
    	"serials": [
    		"${cxRecord.sim}"
        ]
    }
<#if cxRecord?has_next>,</#if>
</#list>
]
</#compress>
}