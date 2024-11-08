"groupmanagementsystem.resultcode": "${response.getResultCode()!400}",
"groupmanagementsystem.resultMessage": "${response.getResultMessage()!""}"

<#if response.getGroups()??>,
	"groupmanagementsystem.groupsWithMembers": [
	<#list response.getGroups() as groupsWithMembers>
	{
			"id": "${groupsWithMembers.getId()!""}",
			"name": "${groupsWithMembers.getName()!""}",
			"description": "${groupsWithMembers.getDescription()!""}",
			"status": "${groupsWithMembers.getStatus()!""}",
			"createdAt": "${groupsWithMembers.getCreatedAt()!""}",
			"updatedAt": "${groupsWithMembers.getUpdatedAt()!""}",
			"minimumMembers": "${groupsWithMembers.getMinimumMembers()!""}",
			"maximumMembers": "${groupsWithMembers.getMaximumMembers()!""}",
			"availableFrom": "${groupsWithMembers.getAvailableFrom()}",
			"availableUntil": "${groupsWithMembers.getAvailableUntil()!""}"

			<#if groupsWithMembers.getData()??>,
				"data": [
				<#list groupsWithMembers.getData() as data>
				{
					"dataName": "${data.getDataName()!""}",
					"dataValue": "${data.getDataValue()!""}"
				}
				<#if data_has_next>, </#if>
				</#list>
				]
			</#if>

		<#if groupsWithMembers.getMembers()??>,
			"members": [

			<#list groupsWithMembers.getMembers() as member>
				{
					"id": "${member.getId()!""}",
					"name": "${member.getName()!""}",
					"userId": "${member.getUserId()!""}",
					"userIdType": "${member.getUserIdType()!""}",
					"memberType": "${member.getMemberType()!""}",
					"status": "${member.getStatus()!""}",
					"createdAt": "${member.getCreatedAt()}",
					"effectiveFrom": "${member.getEffectiveFrom()}"
					"effectiveUntil": "${member.getEffectiveUntil()}"

					<#if member.getData()??>,
					"data": [
					<#list member.getData() as data>
							{
							"dataName": "${data.getDataName()!""}",
							"dataValue": "${data.getDataValue()!""}"
							}
							<#if data_has_next>, </#if>
					</#list>
					]
					</#if>
				}
					<#if member_has_next>, </#if>
			</#list>

		]
		<#else>
		</#if>
		}
		<#if groupsWithMembers_has_next>, </#if>
	</#list>
]
<#else>

</#if>