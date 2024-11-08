"billingsettlement.ersReference": "${response.getErsReference()!""}",
"billingsettlement.resultcode": "${response.getResultCode()!400}",
"billingsettlement.resultMessage": "${response.getResultMessage()!""}"

<#if (response.cycleStartEndDate)??>,
 "billingsettlement.cycleStartEndDate.cycleStartDate": "${response.getCycleStartEndDate().getCycleStartDate()!""}",
 "billingsettlement.cycleStartEndDate.cycleEndDate": "${response.getCycleStartEndDate().getCycleEndDate()!""}"
</#if> 

