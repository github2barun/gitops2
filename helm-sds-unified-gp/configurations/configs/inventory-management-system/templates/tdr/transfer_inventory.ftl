"ims.resultcode":${(response.resultCode?string.computer)!'N/A'},
"ims.resultMessage":"${(response.resultMessage)!'N/A'}",
"ims.orderId":"${(response.orderId)!'N/A'}",
"ims.items":[<#list response.items as item>
{
    "productCode":"${(item.productCode)!'N/A'}",
    "categoryPath":"${(item.formattedCategoryPath())!'N/A'}",
    "category":"${(item.category)!'N/A'}",
    "subCategory":"${(item.subCategory)!'N/A'}",
    "productSku":"${(item.productSku)!'N/A'}",
    "productType":"${(item.productType)!'N/A'}",
    "status":"${(item.status)!'N/A'}",
    "quantity":"${(item.quantity)!'N/A'}",
    "serials":[<#list item.serials as serialNo>
        "${serialNo}"
        <#sep>,
    </#list>],
    "batchIds":[<#list item.batchIds as batchId>
        "${batchId}"
        <#sep>,
    </#list>],
    "ranges": [<#list item.ranges as range>
    {
        "batchId":"${(range.batchId)!'N/A'}",
        "startSerial":"${(range.startSerialNumber)!'N/A'}",
        "endSerial":"${(range.endSerialNumber)!'N/A'}"
    } <#sep>,
    </#list>],
    "updateAttributes": [<#list item.updateAttributes as attribute>
    {
    "name":"${(attribute.name)!'N/A'}",
    "value":"${(attribute.value)!'N/A'}"
    } <#sep>,
    </#list>]
}<#sep>,
</#list>],
"ims.seller.id":"${(response.sellerId)!'N/A'}",
"ims.seller.type":"${(response.sellerType)!'N/A'}",
"ims.seller.whId":"${(response.sellerAddress)!'N/A'}",
"ims.buyer.id":"${(response.buyerId)!'N/A'}",
"ims.buyer.type":"${(response.buyerType)!'N/A'}",
"ims.buyer.whId":"${(response.buyerAddress)!'N/A'}"