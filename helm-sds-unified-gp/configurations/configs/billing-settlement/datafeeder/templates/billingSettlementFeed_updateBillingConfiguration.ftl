"billingsettlement.ersReference": "${response.getErsReference()!""}",
"billingsettlement.resultcode": "${response.getResultCode()!400}",
"billingsettlement.resultMessage": "${response.getResultMessage()!""}"

<#if (response.billingConfiguration)??>,
 "billingsettlement.billingConfiguration.vendor": "${response.getBillingConfiguration().getVendor()!""}",
 "billingsettlement.billingConfiguration.reseller": "${response.getBillingConfiguration().getReseller()!""}",
 "billingsettlement.billingConfiguration.billingMode": "${response.getBillingConfiguration().getBillingMode()!""}",
 "billingsettlement.billingConfiguration.billingFrequency": "${response.getBillingConfiguration().getBillingFrequency()!""}",
 "billingsettlement.billingConfiguration.initiationDate": "${response.getBillingConfiguration().getInitiationDate()!""}",
 "billingsettlement.billingConfiguration.cycleStartDate": "${response.getBillingConfiguration().getCycleStartDate()!""}",
 "billingsettlement.billingConfiguration.cycleEndDate": "${response.getBillingConfiguration().getCycleEndDate()!""}", 
 "billingsettlement.billingConfiguration.settlementAgreement": "${response.getBillingConfiguration().getSettlementAgreement()!""}",
 "billingsettlement.billingConfiguration.settlementMode": "${response.getBillingConfiguration().getSettlementMode()!""}"
</#if>