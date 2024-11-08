"pms.resultcode": ${response.resultCode!400},
"pms.resultMessage": "${response.resultDescription!""}",

<#if response.getOperation()??>
	<#assign operation=response.getOperation() />

     "pms.operationId": "${operation.getId()!""}",
     "pms.operationCode": "${operation.getCode()!""}",
     "pms.operationName": "${operation.getName()!""}",
     "pms.operationDescription": "${operation.getDescription()!""}"
     
<#else>

</#if>