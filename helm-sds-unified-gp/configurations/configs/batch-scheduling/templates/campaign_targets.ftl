{
<#compress>
    "batchId": "${request.batchId}",
    "importType": "${request.importType}",
    <#if request.eof??>
        "lastChunk": ${request.eof},
        <#else>
        "lastChunk": false,
    </#if>

    "resellers": [
    <#list request.requestObjectsList as campaignList>
    {
        "recordId": ${campaignList?counter},
        "resellerMSISDN": "${(campaignList.resellerMSISDN)!''}",
        "campaignId": ${(campaignList.campaignId)!''},
        "kpiName": "${(campaignList.kpiName)!''}",
        "event": ${(campaignList.event)!''},
        "target": ${(campaignList.target)!''}
    }<#if campaignList_has_next>,</#if>
    </#list>
    ]
</#compress>
}