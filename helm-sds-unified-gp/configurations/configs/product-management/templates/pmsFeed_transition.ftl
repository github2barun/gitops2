"pms.resultcode": ${response.resultCode!400},
"pms.resultMessage": "${response.resultDescription!""}",

<#if response.getTransition()??>
	<#assign transition=response.getTransition() />

     "pms.transitionId": "${transition.getId()!""}",
     "pms.transitionStateFrom": "${transition.getFrom().getId()!""}",
     "pms.transitionStateTo": "${transition.getTo().getId()!""}",
     "pms.transitionOperation": "${transition.getOperation().getId()!""}",
     "pms.transitionBusinessRules": "${transition.getBusinessRules()!""}",
     "pms.transitionBusinessActions": "${transition.getBusinessActions()!""}",
     
     <#if transition.getAvailableFrom()??>
       "pms.transitionAvailableFrom": "${transition.getAvailableFrom()}",
     </#if>
     <#if transition.getAvailableUntil()??>
       "pms.transitionAvailableUntil": "${transition.getAvailableUntil()}"
     </#if>
     
<#else>

</#if>