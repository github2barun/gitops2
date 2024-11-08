{
<#compress>
    "orders": [
    <#list request.requestObjectsList as order>
        {
        <#list order.fields?keys as key>
            "${key}" : "${order.fields[key]}"<#if key_has_next>,</#if>
        </#list>
        }<#if order?has_next>,</#if>
    </#list>
    ]
</#compress>