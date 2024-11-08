"itemDetails":
[<#list response.inventories as item>
    {
    "searchFields": {
        "inventoryIdType": "${(item.searchFields.inventoryIdType)!'N/A'}",
        "ownerId": "${(item.searchFields.ownerId)!'N/A'}",
        "productSKU": "${(item.searchFields.productSKU)!'N/A'}",
        "quantity": "${(item.searchFields.quantity)!'N/A'}",
        "employeeId": "${(item.searchFields.employeeId)!'N/A'}",
        "validateHierarchy": "${(item.searchFields.validateHierarchy?string)!'N/A'}",
        "refNo": "${(item.searchFields.refNo)!'N/A'}"
        <#if item.searchFields.ranges?has_content>,
        "ranges": [<#list item.searchFields.ranges as range>
            {
            "startSerial":"${(range.inventoryFrom)!'N/A'}",
            "endSerial":"${(range.inventoryTo)!'N/A'}"
            } <#sep>,
        </#list>]
        </#if>
         <#if item.searchFields.serials?has_content>,
        "serials":[<#list item.searchFields.serials as serialNo>
        "${serialNo}"<#sep>,
            </#list>]
        </#if>
    },
    "updateFields": {
        "location": "${(item.updateFields.location)!'N/A'}",
        "ownerId": "${(item.updateFields.ownerId)!'N/A'}",
        "status": "${(item.updateFields.status)!'N/A'}",
        "updateReason": "${(item.updateFields.updateReason)!'N/A'}",
        "quantity": "${(item.updateFields.quantity)!'N/A'}",
        "refNo": "${(item.updateFields.refNo)!'N/A'}",
        "employeeId": "${(item.updateFields.employeeId)!'N/A'}",
        <#if item.updateFields.inventoryAttributes??>
        "inventoryAttributes": [<#list item.updateFields.inventoryAttributes as attribute>
            {
            "name":"${(attribute.name)!'N/A'}",
            "value":"${(attribute.value)!'N/A'}"
            } <#sep>,
            </#list>]
         </#if>
    }
    }<#sep>,
</#list>]