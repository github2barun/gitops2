resultCode:${(response.resultCode)!'N/A'},
resultMessage:${(response.resultMessage)!'N/A'}
<#if response.resellerPolicyList??>,
    <#assign resellerPolicies = response.resellerPolicyList>
    <#list 0..resellerPolicies?size-1 as i>
    {
      ResellerType : ${(resellerPolicies[i].resellerType)!'N/A'},
      PolicyID : ${(resellerPolicies[i].policyId)!'N/A'}
     }
    </#list>
</#if>