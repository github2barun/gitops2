"itemDetails":
[<#list response.body.feed as item>
    {
    "refNo":"${(item.refNo)!'N/A'}",
    }<#sep>,
</#list>]