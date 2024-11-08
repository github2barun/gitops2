{
    "batchId":"${request.batchId}",
    "importType":"${request.importType}",
    <#if request.eof??>
        "eof": "${request.eof}",
    </#if>
    "rateCard":[
        <#compress>
        <#list request.requestObjectsList as rateCardList>
        {
            "srNo": ${(rateCardList?counter)!''},
            "vendor": "${(rateCardList.vendor)!''}",
            "pickupLocation": "${(rateCardList.pickupLocation)!''}",
            "dropLocation": "${(rateCardList.dropLocation)!''}",
            "distance": "${(rateCardList.distance)!''}",
            "status": "${(rateCardList.status)!''}",
            "minKg": ${(rateCardList.minKg)!''},
            "maxKg": ${(rateCardList.maxKg)!''},
            "charge": ${(rateCardList.charge)!''},
            "type": "${(rateCardList.type)!''}",
            "flatUptoKg": "${(rateCardList.flatUptoKg)!''}",
            "extraChargeAfterMaxKg": "${(rateCardList.extraChargeAfterMaxKg)!''}",
            "selfload": <#if rateCardList.selfload?? && rateCardList.selfload?lower_case="yes">true<#else>false</#if>,
            "priorityRate": "${(rateCardList.priorityRate)!''}"
        }<#if rateCardList_has_next>,</#if>
        </#list>
        </#compress>
    ]
}