"updatedInventory":
    [<#list response as item>
        {
            "resellerId":"${(item.ownerId)!'N/A'}",
            "productSku":"${(item.productSku)!'N/A'}",
            "status":"${(item.status)!'N/A'}",
            "quantity":"${(item.quantity)!'N/A'}",
            "lastUpdatedTimestamp":"${(item.lastUpdatedStamp)!'N/A'}"
        }<#sep>,
    </#list>]