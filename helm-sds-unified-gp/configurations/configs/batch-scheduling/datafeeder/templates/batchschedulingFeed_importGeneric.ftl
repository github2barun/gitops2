"batchscheduling.ersReference": "${response.getErsReference()!""}",
"batchscheduling.resultcode": "${response.getResultCode()!400}",
"batchscheduling.resultMessage": "${response.getResultMessage()!""}"
<#if response.getImportInfo()??>,
 "batchscheduling.importInfo.importId": "${response.getImportInfo().getImportId()!""}",
 "batchscheduling.importInfo.importType": "${response.getImportInfo().getImportType()!""}",
 "batchscheduling.importInfo.importSubType": "${response.getImportInfo().getImportSubType()!""}",
 "batchscheduling.importInfo.fileFormat": "${response.getImportInfo().getFileFormat()!""}",
 "batchscheduling.importInfo.fileId": "${response.getImportInfo().getFileId()!""}",
 "batchscheduling.importInfo.fileName": "${response.getImportInfo().getFileName()!""}",
 "batchscheduling.importInfo.fileLocation": "${response.getImportInfo().getFileLocation()!""}",
 "batchscheduling.importInfo.failOnError": "${response.getImportInfo().getFailOnError()?c}",
 "batchscheduling.importInfo.description": "${response.getImportInfo().getDescription()!""}",
 "batchscheduling.importInfo.uploadedBy": "${response.getImportInfo().getUploadedBy()!""}",
 "batchscheduling.importInfo.creationDate": "${response.getImportInfo().getCreationDate()!""}",
 "batchscheduling.importInfo.lastUpdate": "${response.getImportInfo().getLastUpdate()!""}",
 "batchscheduling.importInfo.schedule.schedulerType": "${response.getImportInfo().getSchedule().getSchedulerType()!""}",
 "batchscheduling.importInfo.schedule.scheduledAt": "${response.getImportInfo().getSchedule().getScheduledAt()!""}"
 <#if response.getImportInfo().getExtraProperties()??>,
  "batchscheduling.importInfo.extraProperties": [
   <#list response.getImportInfo().getExtraProperties() as extraProperties>
         {
          "batchscheduling.importInfo.key": "${extraProperties.getKey()!""}",
          "batchscheduling.importInfo.value": "${extraProperties.getValue()?json_string!""}"
         }
      <#if extraProperties_has_next>, </#if>
   </#list>
  ]
 </#if> 
 <#if response.getImportInfo().getLinks().getLinks()??>,
  "batchscheduling.importInfo.links.links": [
   <#list response.getImportInfo().getLinks().getLinks() as link>
   {
             "batchscheduling.importInfo.links.links.rel": "${link.getRel()!""}",
             "batchscheduling.importInfo.links.links.href": "${link.getHref()!""}"
            }
         <#if link_has_next>, </#if>
   </#list>
  ] 
 </#if> 
 <#if response.getBatchInfo()??>,
  "batchscheduling.batchInfo": [
   <#list response.getBatchInfo() as batchInfo>
          {
           "batchscheduling.batchInfo.batchId": "${batchInfo.getBatchId()!""}",
     "batchscheduling.batchInfo.batchStatus": "${batchInfo.getBatchStatus()!""}",
     "batchscheduling.batchInfo.schedule.schedulerType": "${batchInfo.getSchedule().getSchedulerType()!""}",
     "batchscheduling.batchInfo.schedule.scheduledAt": "${batchInfo.getSchedule().getScheduledAt()!""}",
     "batchscheduling.batchInfo.importId": "${batchInfo.getImportId()!""}",
     "batchscheduling.batchInfo.importType": "${batchInfo.getImportType()!""}",
     "batchscheduling.batchInfo.importSubType": "${batchInfo.getImportSubType()!""}",
     "batchscheduling.batchInfo.description": "${batchInfo.getDescription()!""}",
     "batchscheduling.batchInfo.creationDate": "${batchInfo.getCreationDate()!""}",
     "batchscheduling.batchInfo.lastUpdated": "${batchInfo.getLastUpdated()!""}",
     "batchscheduling.batchInfo.records.total": "${batchInfo.getRecords().getTotal()!""}",
     "batchscheduling.batchInfo.records.processed": "${batchInfo.getRecords().getProcessed()!""}",
     "batchscheduling.batchInfo.records.failed": "${batchInfo.getRecords().getFailed()!""}",
     "batchscheduling.batchInfo.records.success": "${batchInfo.getRecords().getSuccess()!""}"
     <#if batchInfo.getExtraProperties()??>,
      "batchscheduling.batchInfo.extraProperties": [
       <#list batchInfo.getExtraProperties() as extraProperties>
             {
              "batchscheduling.batchInfo.key": "${extraProperties.getKey()!""}",
              "batchscheduling.batchInfo.value": "${extraProperties.getValue()!""}"
             }
          <#if extraProperties_has_next>, </#if>
       </#list>
      ]
     </#if>
     <#if batchInfo.getLinks().getLinks()??>,
      "batchscheduling.batchInfo.links.links": [
       <#list batchInfo.getLinks().getLinks() as link>
       {
                 "batchscheduling.batchInfo.links.links.rel": "${link.getRel()!""}",
                 "batchscheduling.batchInfo.links.links.href": "${link.getHref()!""}"
                }
             <#if link_has_next>, </#if>
       </#list>
      ] 
     </#if>
          }
       <#if batchInfo_has_next>, </#if>
   </#list>
  ]
 </#if> 
</#if> 
