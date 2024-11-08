"oms.orderId":"${(response.orderId)!'N/A'}",
"oms.orderType":"${(response.orderType)!'N/A'}",
"oms.orderStatus":"${(response.orderStatus)!'N/A'}",
"oms.tripId":"${(response.tripId)!'N/A'}",
"oms.items":[<#list response.items as item>
{
    "productCode":"${(item.productCode)!'N/A'}",
    "categoryPath":"${(item.categoryPath)!'N/A'}",
    "productSku":"${(item.productSku)!'N/A'}",
    "quantity":"${(item.quantity)!'N/A'}",
    "reserveType":"${(item.reserveType)!'N/A'}",
    "serials":[<#list item.serials as serialNo>
        "${serialNo}"
        <#sep>,
    </#list>],
    "batchIds":[<#list item.batchIds as batchId>
        "${batchId}"
        <#sep>,
    </#list>],
    "ranges":[<#list item.ranges as range>
        {
            "batchId":"${(range.batchId)!'N/A'}",
            "startSerial":"${(range.startSerial)!'N/A'}",
            "endSerial":"${(range.endSerial)!'N/A'}"
        }
        <#sep>,
    </#list>],
    "data":{<#list item.attributes?keys as key>
        "${key!'N/A'}":"${item.attributes[key]!'N/A'}"
        <#sep>,
    </#list>}
}
    <#sep>,
</#list>],
"oms.missingItems":[<#list response.missingItems as item>
    {
    "inventoryId":"${(item.inventoryId)!'N/A'}",
    "productSku":"${(item.productSKU)!'N/A'}",
    "ownerId":"${(item.ownerId)!'N/A'}",
    "locationId":"${(item.locationId)!'N/A'}",
    "quantity":"${(item.quantity)!'N/A'}",
    "serialNo":"${(item.serialNo)!'N/A'}",
    "batchId":"${(item.batchId)!'N/A'}",
    "refNo":"${(item.refNo)!'N/A'}",
    "startSerial":"${(item.startSerial)!'N/A'}",
    "endSerial":"${(item.endSerial)!'N/A'}"
    }
    <#sep>,
</#list>],
"oms.additionalFields":{<#list response.orderFields as field>
"${(field.name)!'N/A'}":"${(field.value)!'N/A'}"
    <#sep>,
</#list>},
"oms.linkedOrderIds":[<#list response.linkedOrders as order>{
    "internalOrderId":"${(order.id)!'N/A'}",
    "status":"${(order.state)!'N/A'}"
}<#sep>,
</#list>],
<#if response.seller??>
"oms.seller.id":"${(response.seller.id)!'N/A'}",
"oms.seller.additionalFields":{<#list response.seller.additionalFields as field>
"${(field.name)!'N/A'}":"${(field.value)!'N/A'}"
    <#sep>,
</#list>},
</#if>
<#if response.buyer??>
"oms.buyer.id":"${(response.buyer.id)!'N/A'}",
"oms.buyer.additionalFields":{<#list response.buyer.additionalFields as field>
"${(field.name)!'N/A'}":"${(field.value)!'N/A'}"
    <#sep>,
</#list>},
</#if>
<#if response.sender??>
"oms.sender.id":"${(response.sender.id)!'N/A'}",
"oms.sender.additionalFields":{<#list response.sender.additionalFields as field>
"${(field.name)!'N/A'}":"${(field.value)!'N/A'}"
    <#sep>,
</#list>},
</#if>
"oms.receivers":[<#list response.receivers as receiver>
{
    <#if receiver??>
    "id":"${(receiver.id)!'N/A'}",
    "additionalFields":{<#list receiver.additionalFields as field>
    "${(field.name)!'N/A'}":"${(field.value)!'N/A'}"
        <#sep>,
    </#list>}
    </#if>
}
    <#sep>,
</#list>],
"oms.deliveryPaths":[<#list response.deliveryPaths as deliveryPath>
{
    <#if deliveryPath.pickupPoint??>
    "oms.pickup.id":"${(deliveryPath.pickupPoint.id)!'N/A'}",
    "oms.pickup.additionalFields":{<#list deliveryPath.pickupPoint.additionalFields as field>
    "${(field.name)!'N/A'}":"${(field.value)!'N/A'}"
        <#sep>,
    </#list>},
    "oms.pickup.omsFields":{<#list deliveryPath.pickupPoint.omsFields as field>
    "${(field.name)!'N/A'}":"${(field.value)!'N/A'}"
        <#sep>,
    </#list>},
    </#if>
    <#if deliveryPath.dropPoint??>
    "oms.drop.id":"${(deliveryPath.dropPoint.id)!'N/A'}",
    "oms.drop.additionalFields":{<#list deliveryPath.dropPoint.additionalFields as field>
    "${(field.name)!'N/A'}":"${(field.value)!'N/A'}"
        <#sep>,
    </#list>},
    </#if>
    <#if deliveryPath.reconciliationPoint??>
    "oms.reconciliation.id":"${(deliveryPath.reconciliationPoint.id)!'N/A'}",
    "oms.reconciliation.additionalFields":{<#list deliveryPath.reconciliationPoint.additionalFields as field>
    "${(field.name)!'N/A'}":"${(field.value)!'N/A'}"
        <#sep>,
    </#list>},
    </#if>
    "oms.delivery.agentType":"${(deliveryPath.agentType)!'N/A'}"
}
    <#sep>,
</#list>]