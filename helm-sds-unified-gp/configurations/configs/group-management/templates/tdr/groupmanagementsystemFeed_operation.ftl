"groupmanagementsystem.resultcode": "${response.getResultCode()!400}",
"groupmanagementsystem.resultMessage": "${response.getResultMessage()!""}"

<#if (response.operations)??>,

   "groupmanagementsystem.id": "${response.getOperation().getId()!""}",
   "groupmanagementsystem.code": "${response.getOperation().getCode()!""}",
   "groupmanagementsystem.module": "${response.getOperation().getModule()!""}",
   "groupmanagementsystem.name": "${response.getOperation().getName()!""}",
   "groupmanagementsystem.type": "${response.getOperation().getType()!""}",
   "groupmanagementsystem.fromState": "${response.getOperation().getFromState()!""}",
   "groupmanagementsystem.toState": "${response.getOperation().getToState()!""}"

<#else>

</#if>