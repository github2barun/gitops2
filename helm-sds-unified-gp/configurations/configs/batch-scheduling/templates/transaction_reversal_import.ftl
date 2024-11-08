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
    "ersReference":"${transactionList.transactionProperties.map.ersReference}",
    "reasonCode":"reversal",
    "comment":"${transactionList.transactionProperties.map.comment}",
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