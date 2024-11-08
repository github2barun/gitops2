"pms.resultcode": ${response.resultCode!400},
"pms.resultMessage": "${response.resultDescription!""}",

<#if response.getState()??>
	<#assign state=response.getState() />

     "pms.stateId": "${state.getId()!""}",
     "pms.stateCode": "${state.getCode()!""}",
     "pms.stateName": "${state.getName()!""}",
     "pms.stateDescription": "${state.getDescription()!""}",
  
   	 <#if state.getAvailableFrom()??>
       "pms.stateAvailableFrom": "${state.getAvailableFrom()}",
     </#if>
     <#if state.getAvailableUntil()??>
       "pms.stateAvailableUntil": "${state.getAvailableUntil()}"
     </#if>
     
<#else>

</#if>