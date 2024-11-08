resultCode:${(response.resultCode)!'N/A'},
resultMessage:${(response.resultMessage)!'N/A'},
<#if (response.authorized)!false>
Authorized:true
<#else>
Authorized:false
</#if>