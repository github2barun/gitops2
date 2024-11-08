{
<#compress>
    "extraFields":{
         "parameters":{
             "FORCE_UPDATE":${request.forceUpdate}
         }
    },
    <#list request.requestObjectsList as orderList>
        "buyer":{
        "id":"${orderList.buyerId}"
        },
        "seller":{
        "id":"${orderList.sellerId}"
        },
        "receivers": [
        {
        "id":"${orderList.buyerId}"
        }
        ],
        "sender":{
        "id":"${orderList.initiatorId}"
        },
        "items":[
        <#list orderList.items as itemsList>
            {
            "productSku":"${orderList.productSku}",
                "data":{
                    <#if orderList.productSku?? && orderList.productSku?lower_case == "topup" >
                        "SUBSCRIBERMSISDN": "${orderList.buyerId}",
                        "accountTypeId": "AIRTIME",
                    </#if>
                    <#list itemsList.data as data>
                        <#list data?keys as key>
                            "${key}" : "${data[key]}"<#if key_has_next>,</#if>
                        </#list>
                    </#list>
                }
                ,"quantity":${itemsList.quantity}
            }<#if itemsList_has_next>,</#if>
        </#list>
        ]
        <#if orderList.orderType??>
            ,"orderType":"${orderList.orderType}"
        </#if>
        <#if orderList.paymentAgreement??>
            ,"paymentAgreement":"${orderList.paymentAgreement}"
        </#if>
        <#if orderList.paymentMode??>
            ,"paymentMode":"${orderList.paymentMode}"
        </#if>
        <#if orderList.clientComment??>
            ,"clientComment":"${orderList.clientComment}"
        </#if>
    </#list>
</#compress>
}