"itemDetails":
[<#list response.body.feed as item>
    {
    "ownerId":"${(item.ownerId)!'N/A'}",
    "locationId":"${(item.locationId)!'N/A'}",
    "refNo":"${(item.refNo)!'N/A'}",
    "items": [<#list item.items as inventory>
        {
        "productIdentifier":"${(inventory.productIdentifier)!'N/A'}",
        "inventoryId":"${(inventory.inventoryId)!'N/A'}"
        } <#sep>,
    </#list>]
        "updateAttributes":[<#list item.updateAttributes as attribute>
        {
        "name":"${(attribute.name)!'N/A'}",
        "value":"${(attribute.value)!'N/A'}"
        } <#sep>,
    </#list>]
    }<#sep>,
</#list>]