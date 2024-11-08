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
        "receiver":
        {
            "type" : "RESELLERMSISDN",
            "accountTypeId" : "RESELLER",
            <#list transactionList.receiver?keys as key>
                            "${key}" : "${transactionList.receiver[key]}"<#if key_has_next>,</#if>
            </#list>
        },

        "sender":
        {
            "type" : "RESELLERMSISDN",
            "accountTypeId": "RESELLER",
            <#list transactionList.sender?keys as key>
                "${key}" : "${transactionList.sender[key]}"<#if key_has_next>,</#if>
            </#list>
        },
        "product":
        {
            "productSKU": "C2C_WITHDRAW_TRANSFER",
            "amount":
            {
                "currency" : "BDT",
                <#list transactionList.product.amount?keys as key>
                    "${key}" : "${transactionList.product.amount[key]}"<#if key_has_next>,</#if>
                </#list>
            }
        },
        "transactionProperties":
        {
            "map":
            {
                <#list transactionList.transactionProperties.map?keys as key>
                    "${key}" : "${transactionList.transactionProperties.map[key]}"<#if key_has_next>,</#if>
                </#list>,
                "recordId": ${transactionList?counter},
	            "PULL_BACK" : 0
            }
        }
    }<#if transactionList_has_next>,</#if>
</#list>
]
</#compress>
}