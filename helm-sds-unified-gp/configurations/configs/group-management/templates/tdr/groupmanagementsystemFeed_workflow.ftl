"groupmanagementsystem.resultcode": "${response.getResultCode()!400}",
"groupmanagementsystem.resultMessage": "${response.getResultMessage()!""}",
"groupmanagementsystem.ersReference": "${response.getErsReference()!""}",

<#if (response.workflow)??>,

   "groupmanagementsystem.id": "${response.getWorkflow().getId()!""}",
   "groupmanagementsystem.name": "${response.getWorkflow().getName()!""}",
   "groupmanagementsystem.description": "${response.getWorkflow().getDescription()!""}",
   "groupmanagementsystem.createdBy": "${response.getWorkflow().getCreatedBy()!""}",
   "groupmanagementsystem.updatedAt": "${response.getWorkflow().getUpdatedAt()!""}",
   "groupmanagementsystem.CreatedDate": "${response.getWorkflow().getCreatedDate()}",
   "groupmanagementsystem.LastModifiedDate": "${response.getWorkflow().getLastModifiedDate()}",
   "groupmanagementsystem.validUntil": "${response.getWorkflow().getValidUntil()}",
   "groupmanagementsystem.isUpdatable": "${response.getWorkflow().getIsUpdatable()}",
   "groupmanagementsystem.isDeletable": "${response.getWorkflow().isDeletable()}",

   <#if response.getWorkflow().getGroups??>,
   "groupmanagementsystem.groups": [
   <#list response.getWorkflow().getGroups() as group>
        {
        "groupmanagementsystem.id": "${group.getId()!""}",
        "groupmanagementsystem.workflowId": "${group.getWorkflowId()!""}",
        "groupmanagementsystem.workflowOrder": "${group.getWorkflowOrder()!""}",
        "groupmanagementsystem.group": {
        			"groupmanagementsystem.id": "${group.getGroup().getId()!""}",
        			"groupmanagementsystem.name": "${group.getGroup().getName()!""}",
        			"groupmanagementsystem.description": "${group.getGroup().getDescription()!""}",
        			"groupmanagementsystem.status": "${group.getGroup().getStatus()!""}",
        			"groupmanagementsystem.createdAt": "${group.getGroup().getCreatedAt()!""}",
        			"groupmanagementsystem.updatedAt": "${group.getGroup().getUpdatedAt()!""}",
        			"groupmanagementsystem.minimumMembers": "${group.getGroup().getMinimumMembers()!""}",
        			"groupmanagementsystem.maximumMembers": "${group.getGroup().getMaximumMembers()!""}",
        			"groupmanagementsystem.availableFrom": "${group.getGroup().getAvailableFrom()}",
        			"groupmanagementsystem.availableUntil": "${group.getGroup().getAvailableUntil()}"

        			<#if group.getGroup().getData()??>,
        				"groupmanagementsystem.data": [
        				<#list group.getGroup().getData() as data>
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
    	<#if group_has_next>, </#if>
   </#list>
   ]
   </#if> 

<#else>

</#if>