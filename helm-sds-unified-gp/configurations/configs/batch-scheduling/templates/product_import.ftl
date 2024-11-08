{
<#compress>
"batchId": "${request.batchId}",
"productVariants": [
<#list request.requestObjectsList as productList>
    {
        "recordId": "${productList?counter}",
        "productCode": "${productList.productCode}",
        "productSKU": "${productList.productSKU}"
        ,"supplierReference": "${(productList.supplierReference)!''}"
        ,"eanCode": "${(productList.eanCode)!''}"
        ,"status": <#if productList.status?? && productList.status="0">"blocked"<#else>"available"</#if>
        ,"unitPrice": {
                  "currency": "${(productList.currency)!''}",
                  "price": "${(productList.price)!''}",
                  "variablePrice": "${(productList.variablePrice)!''}"
         }
         ,"upsellOption": "${(productList.upsellOption)!''}"
         ,"upSellProducts": "${(productList.upSellProducts)!''}"
         ,"associateRule": <#if productList.associateRule?? && productList.associateRule?lower_case="y">true<#else>false</#if>
         ,"serviceType": "${(productList.serviceType)!''}"
         ,"gwCode": "${(productList.gwCode)!''}"
         ,"resellerType": "${(productList.resellerType)!''}"
         ,"operation": "${(productList.operation)!''}"
         ,"unitOfMeasure": {
            <#list productList.unitOfMeasure?keys as key>
               <#if (productList.unitOfMeasure[key]?? && productList.unitOfMeasure[key] != "")>
                "${key}" : "${productList.unitOfMeasure[key]}"<#if key_has_next>,</#if>
               </#if>
            </#list>
          }
         ,"weight": {
            <#list productList.weight?keys as key>
               <#if (productList.weight[key]?? && productList.weight[key] != "")>
                "${key}" : "${productList.weight[key]}"<#if key_has_next>,</#if>
               </#if>
            </#list>
         }
         ,"volume": {
            <#list productList.volume?keys as key>
               <#if (productList.volume[key]?? && productList.volume[key] != "")>
                "${key}" : "${productList.volume[key]}"<#if key_has_next>,</#if>
               </#if>
            </#list>
         }
         ,"warrantyPeriod": {
            <#list productList.warrantyPeriod?keys as key>
               <#if (productList.warrantyPeriod[key]?? && productList.warrantyPeriod[key] != "")>
                "${key}" : "${productList.warrantyPeriod[key]}"<#if key_has_next>,</#if>
               </#if>
            </#list>
         }
         ,"extraParameters": {
            <#list productList.extraParameters?keys as key>
               <#if (productList.extraParameters[key]?? && productList.extraParameters[key] != "")>
                "${key}" : "${productList.extraParameters[key]}"<#if key_has_next>,</#if>
               </#if>
            </#list>
         }
    }<#if productList_has_next>,</#if>
</#list>
]
</#compress>
}
