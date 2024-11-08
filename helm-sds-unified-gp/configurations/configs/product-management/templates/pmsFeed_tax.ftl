"pms.resultcode": ${response.resultCode!400},
"pms.resultMessage": "${response.resultDescription!""}",

<#if response.getTax()??>
"pms.taxId": "${response.getTax().getId()}",
"pms.taxType": "${response.getTax().getTaxType()}",
"pms.fixedValue": "${response.getTax().getFixedValue()}",
"pms.percentValue": "${response.getTax().getPercentValue()}"
<#else>

</#if>