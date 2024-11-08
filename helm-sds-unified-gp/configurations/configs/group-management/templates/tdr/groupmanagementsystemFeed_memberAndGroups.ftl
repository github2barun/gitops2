"groupmanagementsystem.resultcode": "${response.getResultCode()!400}",
"groupmanagementsystem.resultMessage": "${response.getResultMessage()!""}"

<#if (response.groups)??>,
	"groupmanagementsystem.groups": [
	<#list response.getGroups() as memberAndGroup>
	{
		"groupmanagementsystem.member": {
			"groupmanagementsystem.id": "${memberAndGroup.getMember().getId()!""}",
			"groupmanagementsystem.name": "${memberAndGroup.getMember().getName()!""}",
			"groupmanagementsystem.userId": "${memberAndGroup.getMember().getUserId()!""}",
			"groupmanagementsystem.userIdType": "${memberAndGroup.getMember().getUserIdType()!""}",
			"groupmanagementsystem.memberType": "${memberAndGroup.getMember().getMemberType()!""}",
			"groupmanagementsystem.status": "${memberAndGroup.getMember().getStatus()!""}",
			"groupmanagementsystem.createdAt": "${memberAndGroup.getMember().getCreatedAt()}",
			"groupmanagementsystem.effectiveFrom": "${memberAndGroup.getMember().getEffectiveFrom()}",
			"groupmanagementsystem.effectiveUntil": "${memberAndGroup.getMember().getEffectiveUntil()}"
			
			<#if memberAndGroup.getMember().getData()??>,
				"groupmanagementsystem.data": [
				<#list memberAndGroup.getMember().getData() as data>
				{
					"groupmanagementsystem.dataName": "${data.getDataName()!""}",
					"groupmanagementsystem.dataValue": "${data.getDataValue()!""}"
				}
				<#if data_has_next>, </#if>
				</#list>
				]
			</#if>
		},
		"groupmanagementsystem.group": {
			"groupmanagementsystem.id": "${memberAndGroup.getGroup().getId()!""}",
			"groupmanagementsystem.name": "${memberAndGroup.getGroup().getName()!""}",
			"groupmanagementsystem.description": "${memberAndGroup.getGroup().getDescription()!""}",
			"groupmanagementsystem.status": "${memberAndGroup.getGroup().getStatus()!""}",
			"groupmanagementsystem.createdAt": "${memberAndGroup.getGroup().getCreatedAt()!""}",
			"groupmanagementsystem.updatedAt": "${memberAndGroup.getGroup().getUpdatedAt()!""}",
			"groupmanagementsystem.minimumMembers": "${memberAndGroup.getGroup().getMinimumMembers()!""}",
			"groupmanagementsystem.maximumMembers": "${memberAndGroup.getGroup().getMaximumMembers()!""}",
			"groupmanagementsystem.availableFrom": "${memberAndGroup.getGroup().getAvailableFrom()}",
			"groupmanagementsystem.availableUntil": "${memberAndGroup.getGroup().getAvailableUntil()}"

			<#if memberAndGroup.getGroup().getData()??>,
				"groupmanagementsystem.data": [
				<#list memberAndGroup.getGroup().getData() as data>
				{
					"groupmanagementsystem.dataName": "${data.getDataName()!""}",
					"groupmanagementsystem.dataValue": "${data.getDataValue()!""}"
				}
				<#if data_has_next>, </#if>
				</#list>
				]
			</#if>
			}
		}	
		<#if memberAndGroup_has_next>, </#if>
	</#list>
]		
<#else>

</#if>