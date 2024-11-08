resultCode:${(response.resultCode)!'N/A'},
resultMessage:${(response.resultMessage)!'N/A'}
<#assign policies = response.policyList>
<#list 0..policies?size-1 as i>
    {
    Policy ID : ${(policies[i].policy.id)!'N/A'},
    Policy Name : ${(policies[i].policy.name)!'N/A'}
    }
</#list>