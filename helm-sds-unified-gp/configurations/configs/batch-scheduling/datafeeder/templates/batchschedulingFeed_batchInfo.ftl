"batchscheduling.ersReference": "${response.getErsReference()!""}",
"batchscheduling.resultcode": "${response.getResultCode()!400}",
"batchscheduling.resultMessage": "${response.getResultMessage()!""}"

 <#if response.getBatchInfo()??>,
  "batchscheduling.batchInfo": [
   <#list response.getBatchInfo() as batchInfo>  
          {
             "batchId": "${batchInfo.getBatchId()!""}",
             "batchStatus": "${batchInfo.getBatchStatus()!""}",
             "retryCount": "${batchInfo.getRetryCount()!""}",
             "maxRetryCount": "${batchInfo.getMaxRetryCount()!""}",
             "lastUpdated": "${batchInfo.getLastUpdated()!""}",

             "records.total": "${batchInfo.getRecords().getTotal()!""}",
             "records.processed": "${batchInfo.getRecords().getProcessed()!""}",
             "records.failed": "${batchInfo.getRecords().getFailed()!""}",
             "records.success": "${batchInfo.getRecords().getSuccess()!""}",

             "schedule.schedulerType": "${batchInfo.getSchedule().getSchedulerType()!""}",
             "schedule.scheduledAt": "${batchInfo.getSchedule().getScheduledAt()!""}",

             "importId": "${batchInfo.getImportId()!""}",
             "importType": "${batchInfo.getImportType()!""}",
             "importSubType": "${batchInfo.getImportSubType()!""}",
             "description": "${batchInfo.getDescription()!""}",
             "creationDate": "${batchInfo.getCreationDate()!""}",

             "retriableFileId": "${batchInfo.getRetriableFileId()!""}",
			 "retriableFileName": "${batchInfo.getRetriableFileName()!""}",
             "retriableFileRecords": "${batchInfo.getRetriableFileRecords()!""}",

             "errorFileId": "${batchInfo.getErrorFileId()!""}",
             "errorFileName": "${batchInfo.getErrorFileName()!""}"

             <#if batchInfo.getExtraProperties()??>,
              "batchscheduling.batchInfo.extraProperties": [
               <#list batchInfo.getExtraProperties() as extraProperties>
                     {
                      "key": "${extraProperties.getKey()!""}",
                      "value": "${extraProperties.getValue()!""}"
                     }
                  <#if extraProperties_has_next>, </#if>
               </#list>
              ]
             </#if>
             <#if batchInfo.getLinks().getLinks()??>,
              "batchscheduling.batchInfo.links.links": [
               <#list batchInfo.getLinks().getLinks() as link>
               {
                         "rel": "${link.getRel()!""}",
                         "href": "${link.getHref()!""}"
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