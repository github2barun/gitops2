{
<#compress>
"batchId": "${request.batchId}",
"contractRequestModel": [
<#list request.requestObjectsList as contractList>
    {
        "recordId": "${contractList?counter}"
        ,"contractImportId": "${(contractList.contractImportId?trim)!''}"
        ,"productVariantName": "${(contractList.productVariantName?trim)!''}"
        ,"fromRange": "${(contractList.fromRange?trim)!''}"
        ,"toRange": "${(contractList.toRange?trim)!''}"
        ,"commission": "${(contractList.commission?trim)!''}"
        ,"commissionType": <#if contractList.commissionType?? && contractList.commissionType?trim?lower_case="p">"Percentage"<#else>"Absolute"</#if>
        ,"externalBonus": "${(contractList.externalBonus?trim?upper_case)!''}"
        ,"validFromDate": "${(contractList.validFromDate?trim)!''}"
    }<#if contractList_has_next>,</#if>
</#list>
]
</#compress>
}
