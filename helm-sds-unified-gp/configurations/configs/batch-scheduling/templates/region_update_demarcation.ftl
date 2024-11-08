{
<#compress>
    "requests": [

     <#list request.requestObjectsList as row>
        <#-- Split the data for the Region -->
         <#assign regionData = row.region?split(":")>
             <#-- Split the data for the Cluster -->
         <#assign clusterData = row.cluster?split(":")>
            <#-- Split the data for the Territory -->
         <#assign territoryData = row.territory?split(":")>
            <#-- Split the data for the Thana -->
         <#assign thanaData = row.thana?split(":")>

         {
             "regionName": "${regionData[1]}",
             "id": "${regionData[0]}",
             "level": "2",
             "parentRegionName": "BD",
             "data": "${regionData[1]}",
                 "subRegions": [
                     {
                         "regionName": "${clusterData[1]}",
                         "id": "${clusterData[0]}",
                         "data": "${clusterData[1]}",
                         "subRegions": [
                             {
                             "regionName": "${territoryData[1]}",
                             "id": "${territoryData[0]}",
                             "data": "${territoryData[1]}",
                             "subRegions": [
                                 {
                                     "regionName": "${thanaData[1]}",
                                     "id": "${thanaData[0]}",
                                     "data": "${thanaData[1]}"
                                 }
                             ]
                         }
                     ]
                 }
             ]
         }


     </#list>
    ]
</#compress>
}