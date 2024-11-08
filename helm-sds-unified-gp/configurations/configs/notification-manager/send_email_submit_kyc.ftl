<#assign customerData = customer>

<#if customerData.extraFields.brandName?matches("TARAJI", "i")>
Hello

This is the TARAJI brand content.
Please find the attached contract file for sim registartion of customer ${customerData.customerId}.

<#elseif customerData.extraFields.brandName?matches("TT", "i")>
Hello

This is the TT brand content.
Please find the attached contract file for sim registartion of customer ${customerData.customerId}.

</#if>