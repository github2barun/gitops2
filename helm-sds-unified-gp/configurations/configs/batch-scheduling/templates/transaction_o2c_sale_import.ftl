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
        "sender":
        {
            "type" : "RESELLERID",
            <#list transactionList.sender?keys as key>
                "${key}" : "${transactionList.sender[key]}"<#if key_has_next>,</#if>
            </#list>
            "id" : "operator",
            "accountTypeId" : "RESELLER"
        },

        "receiver":
        {
            "type" : "RESELLERID",
            "type" : "RESELLERMSISDN",
            "accountTypeId": "RESELLER",
            <#list transactionList.receiver?keys as key>
                "${key}" : "${transactionList.receiver[key]}"<#if key_has_next>,</#if>
            </#list>
        },
        "product":
        {
            "productSKU": "${transactionList.product.productSKU}",
            "productSKU": "O2C_transfer",
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
        "recordId": ${transactionList?counter},
                <#list transactionList.transactionProperties.map?keys as key>
                    "${key}" : "${transactionList.transactionProperties.map[key]}"<#if key_has_next>,</#if>
                </#list>
                "recordId": ${transactionList?counter},
                "applicableContract" : "1"
            }
        }
    }<#if transactionList_has_next>,</#if>
</#list>
]
</#compress>
}

}