"itemDetails":
[<#list response.body.feed as item>
    {
    "seller": {
    "id": "${(item.seller.id)!'N/A'}",
    "idType": "${(item.seller.idType)!'N/A'}",
    "type": "${(item.seller.locationId)!'N/A'}"
    },
    "productSku":"${(item.productIdentifier)!'N/A'}",
    "quantity":"${(item.quantity)!'N/A'}",
    "reserveType":"${(item.reserveType)!'N/A'}",
    "employeeId":"${(item.employeeId)!'N/A'}",
    "attributes":"${(item.attributes.toString())!'N/A'}",
    "refNo":"${(item.refNo)!'N/A'}",
    <#if item.productType == "TRACKABLE_NONSERIALIZED">
        "ranges": [<#list item.ranges as range>
        {
        "startSerial":"${(range.startSerialNumber)!'N/A'}",
        "endSerial":"${(range.endSerialNumber)!'N/A'}"
        } <#sep>,
    </#list>]
    </#if>
    <#if item.productType == "SERIALIZED">
        "serials":[<#list item.serials as serialNo>
        "${serialNo}"<#sep>,
    </#list>]
    </#if>
    }<#sep>,
</#list>]