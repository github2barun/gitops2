"pms.resultcode": ${response.resultCode!400},
"pms.resultMessage": "${response.resultDescription!""}",

<#if response.getWorkflow()??>
	<#assign workflow=response.getWorkflow() />

     "pms.workflowId": "${workflow.getId()!""}",
     "pms.workflowName": "${workflow.getName()!""}",
     "pms.workflowDescription": "${workflow.getDescription()!""}",
     
   <#if workflow.getTransitions()??>
   "transitions": [
   <#list workflow.getTransitions() as transition>
    	${transition.getId()}<#if transition_has_next>, </#if>
   </#list>
   ]
   </#if> 
<#else>

</#if>