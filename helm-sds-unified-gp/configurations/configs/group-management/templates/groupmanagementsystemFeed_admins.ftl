"groupmanagementsystem.resultcode": "${response.getResultCode()!400}",
"groupmanagementsystem.resultMessage": "${response.getResultMessage()!""}"

<#if response.getAdmins()??>,
   "groupmanagementsystem.admins": [
   <#list response.getAdmins() as admin>
        	{
        	"groupmanagementsystem.id": "${admin.getId()!""}",
        	"groupmanagementsystem.name": "${admin.getName()!""}",
        	"groupmanagementsystem.userId": "${admin.getUserId()!""}",
        	"groupmanagementsystem.userIdType": "${admin.getUserIdType()!""}",
        	"groupmanagementsystem.status": "${admin.getStatus()!""}",
        	"groupmanagementsystem.createdAt": "${admin.getCreatedAt()}",
        	"groupmanagementsystem.effectiveFrom": "${admin.getEffectiveFrom()}"
        	"groupmanagementsystem.effectiveUntil": "${admin.getEffectiveUntil()}"

		   	<#if admin.getData()??>,
		   	"groupmanagementsystem.data": [
		   	<#list admin.getData() as data>
		   		{
		   		"groupmanagementsystem.dataName": "${data.getDataName()!""}",
		   		"groupmanagementsystem.dataValue": "${data.getDataValue()!""}"
		   		}
		   	<#if data_has_next>, </#if>
			</#list>
			]
			</#if>
			} 
	<#if admin_has_next>, </#if>
   </#list>
   ]
<#else>

</#if>