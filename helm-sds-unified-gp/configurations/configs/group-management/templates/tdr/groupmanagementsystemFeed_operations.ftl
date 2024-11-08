"groupmanagementsystem.resultcode": "${response.getResultCode()!400}",
"groupmanagementsystem.resultMessage": "${response.getResultMessage()!""}"

<#if (response.operations)??>,
   "groupmanagementsystem.operations": [
   <#list response.getOperations() as operation>
   		{
   		"groupmanagementsystem.id": "${operation.getId()!""}",
	    "groupmanagementsystem.code": "${operation.getCode()!""}",
	    "groupmanagementsystem.module": "${operation.getModule()!""}",
	    "groupmanagementsystem.name": "${operation.getName()!""}",
	    "groupmanagementsystem.type": "${operation.getType()!""}",
	    "groupmanagementsystem.fromState": "${operation.getFromState()!""}",
	    "groupmanagementsystem.toState": "${operation.getToState()!""}"
   		}
   		<#if operation_has_next>, </#if>
   	</#list>	 
	]
<#else>

</#if>