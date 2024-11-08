"oms.tripId":"${(response.tripId)!'N/A'}",
<#if response.totalReceivedAmount??>
"oms.totalReceivedAmount": {
    "amount": ${(response.totalReceivedAmount.amount)!'N/A'},
    "reason": "${(response.totalReceivedAmount.reason)!'N/A'}"
},
</#if>
<#if response.totalMissingAmount??>
"oms.totalMissingAmount": {
    "amount": ${(response.totalMissingAmount.amount)!'N/A'},
    "reason": "${(response.totalMissingAmount.reason)!'N/A'}"
}
</#if>