"feedFor":"bulkUpdate",
"importBatchId" : "${(response.importBatchId)!'N/A'}",
"importType" : "${(response.importType)!'N/A'}",
"failed": [<#list response.getFailedRecords() as failed>
    {
    "recordId":"${(failed.recordId)!'N/A'}",
    "batchId":"${(failed.batchId)!'N/A'}",
    "serialNumber":"${(failed.serialNumber)!'N/A'}",
    "productSKU":"${(failed.productSKU)!'N/A'}",
    "owner":"${(failed.owner)!'N/A'}",
    "statusMessage":"${(failed.statusDescription)!'N/A'}"
    } <#sep>,
</#list>],
"passed": [<#list response.getSuccessfulRecords() as success>
    {
    "recordId":"${(success.recordId)!'N/A'}",
    "batchId":"${(success.batchId)!'N/A'}",
    "serialNumber":"${(success.serialNumber)!'N/A'}",
    "productSKU":"${(success.productSKU)!'N/A'}",
    "owner":"${(success.owner)!'N/A'}",
    "statusMessage":"${(success.statusDescription)!'N/A'}"
    } <#sep>,
</#list>]