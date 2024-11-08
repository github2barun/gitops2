"batchscheduling.ersReference": "${response.getErsReference()!""}",
"batchscheduling.resultcode": "${response.getResultCode()!400}",
"batchscheduling.resultMessage": "${response.getResultMessage()!""}"

 <#if response.getServiceInfo()??>,
  "batchscheduling.serviceInfo": [
   <#list response.getServiceInfo() as serviceInfo>
          {
             "importService": "${serviceInfo.getImportService()!""}"
              <#if serviceInfo.getImportType()??>,
                  "importType": [
                   <#list serviceInfo.getImportType() as importType>

                             "${importType!""}"
                             <#if importType_has_next>, </#if>

                   </#list>
                  ]
              </#if>

          }
       <#if serviceInfo_has_next>, </#if>
   </#list>
  ]
</#if>
