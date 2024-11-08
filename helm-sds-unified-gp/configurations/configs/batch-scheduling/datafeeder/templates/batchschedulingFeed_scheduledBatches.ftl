"batchscheduling.ersReference": "${response.getErsReference()!""}",
"batchscheduling.resultcode": "${response.getResultCode()!400}",
"batchscheduling.resultMessage": "${response.getResultMessage()!""}"

 <#if response.getScheduledBatches()??>,
  "batchscheduling.scheduledBatches": [
   <#list response.getScheduledBatches() as scheduledBatches>
          {
		     "batchId": "${scheduledBatches.getBatchId()!""}",
			 "batchStatus": "${scheduledBatches.getBatchStatus()!""}",
			 "retryCount": "${scheduledBatches.getRetryCount()!""}",
             "maxRetryCount": "${scheduledBatches.getMaxRetryCount()!""}"			 			 
			 
			  <#if scheduledBatches.getImports()??>,
			    "batchscheduling.scheduledBatches.imports": [
                 <#list scheduledBatches.getImports() as imports>
                 	{
						"importId": "${imports.getImportId()!""}",
						"importType": "${imports.getImportType()!""}",
						"importSubType": "${imports.getImportSubType()!""}",
			            "description": "${imports.getDescription()!""}",
			            "creationDate": "${imports.getCreationDate()!""}",
			            "lastUpdated": "${imports.getLastUpdated()!""}",
			
						"records.total": "${imports.getRecords().getTotal()!""}",
						"records.processed": "${imports.getRecords().getProcessed()!""}",
						"records.failed": "${imports.getRecords().getFailed()!""}",
						"records.success": "${imports.getRecords().getSuccess()!""}"
                 	}
			  <#if imports_has_next>, </#if>
                 </#list>
			    ],
			  </#if>						

             "schedule.schedulerType": "${scheduledBatches.getSchedule().getSchedulerType()!""}",
             "schedule.scheduledAt": "${scheduledBatches.getSchedule().getScheduledAt()!""}",
             
             "retriableFileId": "${scheduledBatches.getRetriableFileId()!""}",
			 "retriableFileName": "${scheduledBatches.getRetriableFileName()!""}",
             "retriableFileRecords": "${scheduledBatches.getRetriableFileRecords()!""}",

             "errorFileId": "${scheduledBatches.getErrorFileId()!""}",
             "errorFileName": "${scheduledBatches.getErrorFileName()!""}"

             <#if scheduledBatches.getExtraProperties()??>,
              "extraProperties": [
               <#list scheduledBatches.getExtraProperties() as extraProperties>
                     {
                      "key": "${extraProperties.getKey()!""}",
                      "value": "${extraProperties.getValue()!""}"
                     }
                  <#if extraProperties_has_next>, </#if>
               </#list>
              ]
             </#if>

             <#if scheduledBatches.getLinks().getLinks()??>,
              "links.links": [
               <#list scheduledBatches.getLinks().getLinks() as link>
               {
                         "rel": "${link.getRel()!""}",
                         "href": "${link.getHref()!""}"
                        }
                     <#if link_has_next>, </#if>
               </#list>
              ]
             </#if>
          }
       <#if scheduledBatches_has_next>, </#if>
   </#list>
  ]
</#if>