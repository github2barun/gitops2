"groupmanagementsystem.resultcode": "${response.getResultCode()!400}",
"groupmanagementsystem.resultMessage": "${response.getResultMessage()!""}"

<#if response.getMembers()??>,
   "groupmanagementsystem.members": [
   {
   <#list response.getMembers() as member>

		   "groupmanagementsystem.id": "${member.getId()!""}",
		   "groupmanagementsystem.name": "${member.getName()!""}",
		   "groupmanagementsystem.userId": "${member.getUserId()!""}",
		   "groupmanagementsystem.userIdType": "${member.getUserIdType()!""}",
		   "groupmanagementsystem.memberType": "${member.getMemberType()!""}",
		   "groupmanagementsystem.status": "${member.getStatus()!""}",
		   "groupmanagementsystem.createdAt": "${member.getCreatedAt()}",
		   "groupmanagementsystem.effectiveFrom": "${member.getEffectiveFrom()}"
		   "groupmanagementsystem.effectiveUntil": "${member.getEffectiveUntil()}"
		   
		   <#if member.getData()??>,
		   "groupmanagementsystem.data": [
		   <#list member.getData() as data>
		        {
		        "groupmanagementsystem.dataName": "${data.getDataName()!""}",
		        "groupmanagementsystem.dataValue": "${data.getDataValue()!""}"
		        }
		    	<#if data_has_next>, </#if>
		   </#list>
		   ]
		   </#if> 
        
    	<#if member_has_next>, </#if>
   </#list>
   }
   ]

<#else>

</#if>