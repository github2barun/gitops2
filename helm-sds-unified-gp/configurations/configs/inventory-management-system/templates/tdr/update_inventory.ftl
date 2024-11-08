"itemDetails":
    {
        "productSKU": "${(response.productSKU)!'N/A'}",
        "owner": "${(response.owner)!'N/A'}",
        "location": "${(response.refNo)!'N/A'}",
        "quantity": "${(response.quantity)!'N/A'}",
        "stateName": "${(response.stateName)!'N/A'}",
        <#list response.data?keys as prop>
            ${key} = ${prop},
            ${value} = ${response.data.get(prop)}
        </#list>
}