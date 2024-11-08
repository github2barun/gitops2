"resultcode": ${response.resultCode!0},
"resultMessage": "${response.resultMessage!""}"
"regionId": "${response.regionId!""}",
"regionType": "${response.regionType!""}",
"regionName": "${response.regionName!""}",
"path": "${response.path!""}",
"location": "${response.location!""}",
"cluster": "${response.cluster!""}",
"distributor": "${response.distributor!""}",
"ovaAccounts": "${response.ovaAccounts!""}",
"ownerUserName": "${response.ownerUserName!""}",
"ownerLevel": "${response.ownerLevel!""}",
"regionLevel": "${response.regionLevel!""}",
"id": "${response.id!""}",
"name": "${response.name!""}",
"region": "${response.region!""}"

<#compress>
    <#if response.class.simpleName == "DataResponse" && response.getData()?? && response.getData()?size gt 0>
        <#assign regionData = response.getData() />
        ,
        "data": [
        <#list regionData as data>
            "resultcode": ${data.resultCode!0},
			"resultMessage": "${data.resultMessage!""}"
			"regionId": "${data.regionId!""}",
			"regionType": "${data.regionType!""}",
			"regionName": "${data.regionName!""}",
			"path": "${data.path!""}",
			"data": "${data.data!""}",
			"location": "${data.location!""}",
			"cluster": "${data.cluster!""}",
			"distributor": "${data.distributor!""}",
			"ovaAccounts": "${data.ovaAccounts!""}",
			"ownerUserName": "${data.ownerUserName!""}",
			"ownerLevel": "${response.ownerLevel!""}",<#if data_has_next>,</#if>
        </#list>
        ]
    <#else>
    </#if>
</#compress>

<#compress>
    <#if response.class.simpleName == "RegionResponse">

     ,"data": "${response.data!""}"

         <#if response.getSubRegions()?? && response.getSubRegions()?size gt 0>
            <#assign regionData = response.getSubRegions() />
            ,
            "subRegions": [
            <#list regionData as data>
                "resultcode": ${data.resultCode!0},
                "resultMessage": "${data.resultMessage!""}"
                "regionId": "${data.regionId!""}",
                "regionType": "${data.regionType!""}",
                "regionName": "${data.regionName!""}",
                "path": "${data.path!""}",
                "data": "${data.data!""}",
                "location": "${data.location!""}",
                "cluster": "${data.cluster!""}",
                "distributor": "${data.distributor!""}",
                "ovaAccounts": "${data.ovaAccounts!""}",
                "ownerUserName": "${data.ownerUserName!""}",
                "ownerLevel": "${response.ownerLevel!""}",<#if data_has_next>,</#if>
            </#list>
            ]
        <#else>
        </#if>
    <#else>
    </#if>
</#compress>