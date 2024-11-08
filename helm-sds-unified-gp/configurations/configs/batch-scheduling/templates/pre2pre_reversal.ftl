{
<#compress>
"batchId": "${request.batchId}",
"importType": "${request.importType}",
"extraFields":
    {
         "parameters":{
             "FORCE_UPDATE":${request.forceUpdate}
         }
    },

"requestList": [
<#list request.requestObjectsList as transactionList>
    {
    "ersReference":"${transactionList.transactionProperties.map.referenceNumber}",
    "reasonCode":"${transactionList.transactionProperties.map.remarks}",
    "comment":"${transactionList.transactionProperties.map.remarks}",
     "transactionProperties":
        {
            "map":
            {
                "recordId": ${transactionList?counter}
            }
        }
    }<#if transactionList_has_next>,</#if>
</#list>
]
</#compress>
}