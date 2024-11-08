"seller": {
    "id": "${(response.sellerId)!'N/A'}",
    "idType": "${(response.sellerIdType)!'N/A'}",
    "type": "${(response.sellerType)!'N/A'}"
    },
"buyer": {
    "id": "${(response.buyerId)!'N/A'}",
    "idType": "${(response.buyerIdType)!'N/A'}",
    "type": "${(response.buyerType)!'N/A'}"
    },
"locationId": "${(response.locationId)!'N/A'}",
"itemDetails":
    [<#list response.items as item>
        {
            "productSku":"${(item.productSku)!'N/A'}",
            "productType":"${(item.productType)!'N/A'}",
            "quantity":"${(item.quantity)!'N/A'}",
            <#if item.productType == "TRACKABLE_NONSERIALIZED">
                "ranges": [<#list item.ranges as range>
                {
                "batchId":"${(range.batchId)!'N/A'}",
                "startSerial":"${(range.startSerialNumber)!'N/A'}",
                "endSerial":"${(range.endSerialNumber)!'N/A'}",
                "quantity": "${range.endSerialNumber?number - range.startSerialNumber?number + 1}"
                } <#sep>,
                </#list>]
            <#else >
                "serials":[<#list item.serials as serialNo>
                "${serialNo}"<#sep>,
                </#list>]
            </#if>
        }<#sep>,
    </#list>]