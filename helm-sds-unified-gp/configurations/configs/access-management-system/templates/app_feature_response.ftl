resultCode:${(response.resultCode)!'N/A'},
resultMessage:${(response.resultMessage)!'N/A'}
<#assign features = response.appFeatureList>
<#list 0..features?size-1 as i>
    {
    Feature ID : ${(features[i].id)!'N/A'},
    Feature Code : ${(features[i].featureCode)!'N/A'}
    Feature Value : ${(features[i].featureValue)!'N/A'}
    }
</#list>