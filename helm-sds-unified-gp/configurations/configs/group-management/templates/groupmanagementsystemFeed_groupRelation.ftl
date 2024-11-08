"groupmanagementsystem.resultcode": "${response.getResultCode()!400}",
"groupmanagementsystem.resultMessage": "${response.getResultMessage()!""}"

<#if response.getGroupRelation()??>,

   "groupmanagementsystem.id": "${response.getGroupRelation().getId()!""}",
   "groupmanagementsystem.fromGroupId": "${response.getGroupRelation().getFromGroupId()!""}",
   "groupmanagementsystem.operationId": "${response.getGroupRelation().getOperationId()!""}",
   "groupmanagementsystem.toGroupId": "${response.getGroupRelation().getToGroupId()!""}"

<#else>

</#if>