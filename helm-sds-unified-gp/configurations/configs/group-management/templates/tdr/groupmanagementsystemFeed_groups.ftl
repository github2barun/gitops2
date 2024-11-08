"groupmanagementsystem.resultcode": "${response.getResultCode()!400}",
"groupmanagementsystem.resultMessage": "${response.getResultMessage()!""}"

<#if (response.groups)??>,
   "groupmanagementsystem.groups": [
   <#list response.getGroups() as group>
   		{
   		"groupmanagementsystem.id": "${group.getId()!""}",
   		"groupmanagementsystem.name": "${group.getName()!""}",
   		"groupmanagementsystem.description": "${group.getDescription()!""}",
   		"groupmanagementsystem.status": "${group.getStatus()!""}",
   		"groupmanagementsystem.createdAt": "${group.getCreatedAt()!""}",
   		"groupmanagementsystem.updatedAt": "${group.getUpdatedAt()!""}",
   		"groupmanagementsystem.minimumMembers": "${group.getMinimumMembers()!""}",
   		"groupmanagementsystem.maximumMembers": "${group.getMaximumMembers()!""}",
   		"groupmanagementsystem.availableFrom": "${group.getAvailableFrom()}",
   		"groupmanagementsystem.availableUntil": "${group.getAvailableUntil()}"
   		
   		<#if group.getData()??>,
   		"groupmanagementsystem.data": [
   		<#list group.getData() as data>
   			{
   			"groupmanagementsystem.dataName": "${data.getDataName()!""}",
   			"groupmanagementsystem.dataValue": "${data.getDataValue()!""}"
   			}
   			<#if data_has_next>, </#if>
   		</#list>
   		]
   		</#if>
   		}
   		<#if group_has_next>, </#if>
   	</#list>	 
	]
<#else>

</#if>