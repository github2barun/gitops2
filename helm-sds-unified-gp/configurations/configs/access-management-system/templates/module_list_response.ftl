resultCode:${(response.resultCode)!'N/A'},
resultMessage:${(response.resultMessage)!'N/A'}
<#assign modules = response.moduleList>
<#list 0..modules?size-1 as i>
    {
    Module ID : ${(modules[i])!'N/A'},
    }
</#list>