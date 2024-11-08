{
<#compress>
"batchId": "${request.batchId}",
"splitContractCommissionRequestModel": [
<#list request.requestObjectsList as contractList>
    {
        "recordId": "${contractList?counter}"
        ,"contractImportId": "${(contractList.contractImportId?trim)!''}"
        ,"productVariantName": "${(contractList.productId?trim)!''}"
        ,"splitAmount": "${(contractList.denomination?trim)!''}"
        ,"commission": "${(contractList.commission?trim)!''}"
        ,"commissionType": <#if contractList.commissionType?? && contractList.commissionType?trim?lower_case="ptc">"Percentage"<#elseif contractList.commissionType?? && contractList.commissionType?trim?lower_case="amt">"Absolute"<#else>${contractList.commissionType}</#if>
    }<#if contractList_has_next>,</#if>
</#list>
]
</#compress>
}