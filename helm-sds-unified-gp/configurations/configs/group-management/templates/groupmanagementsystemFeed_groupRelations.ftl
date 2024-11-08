"groupmanagementsystem.resultcode": "${response.getResultCode()!400}",
"groupmanagementsystem.resultMessage": "${response.getResultMessage()!""}"

<#if response.getGroupRelations()??>,
	"groupmanagementsystem.groupRelations": [
	<#list response.getGroupRelations() as groupRelation>
		{
		"groupmanagementsystem.id": "${groupRelation.getId()!""}",
		"groupmanagementsystem.fromGroupId": "${groupRelation.getFromGroupId()!""}",
		"groupmanagementsystem.operationId": "${groupRelation.getOperationId()!""}",
		"groupmanagementsystem.toGroupId": "${groupRelation.getToGroupId()!""}"
		}
		<#if groupRelation_has_next>, </#if>
	</#list>
	]
<#else>

</#if>