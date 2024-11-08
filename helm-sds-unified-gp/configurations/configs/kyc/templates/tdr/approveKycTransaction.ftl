"kyc.salesTransactionTime":"${(response.customer.dateCreated)?datetime?string["yyyy-MM-dd HH:mm:ss"]!'N/A'}",
"kyc.resultcode":${(response.baseResponse.resultCode)!'N/A'},
"kyc.resultMessage":"${(response.baseResponse.resultMessage)!'N/A'}",
"kyc.customerId":"${(response.customer.customerId)!'N/A'}",
"kyc.nationalIdCardType":"${(response.customer.nationalIdCardType)!'N/A'}",
"kyc.nationalIdNumber":"${(response.customer.nationalIdNumber)!'N/A'}",
"kyc.status":"${(response.customer.kycStatus)!'N/A'}",
"kyc.msisdn":"${(response.customer.msisdn)!'N/A'}",
"kyc.msisdnType":"N/A",
"kyc.contactPerson":"${(response.customer.contactPerson)!'N/A'}",
"kyc.simSerialNumber":"${(response.customer.extraFields.simSerialNumber)!'N/A'}",
"kyc.brandName":"${(response.customer.extraFields.brandName)!'N/A'}",
"kyc.brandCode":"${(response.customer.extraFields.brandCode)!'N/A'}",
"kyc.brandPrefix":"${(response.customer.extraFields.brandPrefix)!'N/A'}",
"kyc.simType":"${(response.customer.extraFields.simType)!'N/A'}",
"kyc.approverType":"${(response.user.resellerType)!'N/A'}",
"kyc.approverID":"${(response.user.userId)!'N/A'}",
"kyc.approverName":"${(response.extraFields.member)!'N/A'}",
"kyc.approverGroup":
<#list response.extraFields.groups as approval>
"${(approval)!'N/A'}<#if approval_has_next>,</#if>"
</#list>,
"kyc.createrId":"${(response.customer.createrId)!'N/A'}",
<#list response.user.resellerPath?split("/") as parentList>
<#if (parentList_has_next)>
 <#assign parent = parentList>
 </#if>
</#list>
<#if response.operationType == "GRACE_CHECK">
"kyc.parentId":"${(parent)!'N/A'}",
"kyc.graceExceddedOn":"${(response.customer.graceExpiryDate)?datetime!'N/A'}"
<#else>
"kyc.parentId":"${(parent)!'N/A'}"
</#if>