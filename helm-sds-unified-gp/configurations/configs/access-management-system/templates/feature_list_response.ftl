resultCode:${(response.resultCode)!'N/A'},
resultMessage:${(response.resultMessage)!'N/A'}
<#assign features = response.featureList>
<#list 0..features?size-1 as i>
{
   Feature ID : ${(features[i].id)!'N/A'},
   Feature Name : ${(features[i].name)!'N/A'}
 }
</#list>