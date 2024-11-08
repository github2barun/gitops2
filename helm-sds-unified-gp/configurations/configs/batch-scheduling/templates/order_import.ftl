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
        "id":"${orderList.sellerId}"
        },
        "items":[
        <#list orderList.items as itemsList>
            {
            "productSku":"${itemsList.productSku}",
            "reserveType":"${itemsList.reserveType}",
            "ranges":[
            <#list itemsList.ranges as rangesList>
                {

                "startSerial":"${rangesList.startSerial}",
                "endSerial":"${rangesList.endSerial}"

                }<#if rangesList_has_next>,</#if>
            </#list>
            ]

            }<#if itemsList_has_next>,</#if>
        </#list>
        ]
        <#if orderList.orderType??>
            , "orderType":"${orderList.orderType}"
        </#if>
        <#if orderList.returnType??>
            ,   "returnType":"${orderList.returnType}"
        </#if>
        <#if orderList.returnReason??>
            ,  "returnReason":"${orderList.returnReason}"
        </#if>
        <#if orderList.clientComment??>
            ,  "clientComment":"${orderList.clientComment}"
        </#if>
    </#list>
</#compress>
}