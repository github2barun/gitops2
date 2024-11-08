<#compress>
"groupmanagementsystem.resultCode": "${response.getResultCode()!400}",
"groupmanagementsystem.resultMessage": "${response.getResultMessage()!""}"
<#if (response.getGroupIdAndMember()?has_content)??>,
"groupmanagementsystem.groupIdAndMember": [
<#list response.getGroupIdAndMember() as groupIdAndMember>
{
"groupmanagementsystem.groupId": "${groupIdAndMember.getGroupId()!""}"
,"groupmanagementsystem.message": "${groupIdAndMember.getMessage()!""}"
<#if groupIdAndMember.getMember()??> ,
<#assign member=groupIdAndMember.getMember()>
"groupmanagementsystem.member.id":"${member.getId()!""}",
"groupmanagementsystem.member.name": "${member.getName()!""}",
"groupmanagementsystem.member.userId": "${member.getUserId()!""}",
"groupmanagementsystem.member.userIdType": "${member.getUserIdType()!""}",
"groupmanagementsystem.member.memberType": "${member.getMemberType()!""}",
"groupmanagementsystem.member.status": "${member.getStatus()!""}",
"groupmanagementsystem.member.createdAt": "${member.getCreatedAt()!""}",
"groupmanagementsystem.member.effectiveFrom": "${member.getEffectiveFrom()!""}",
"groupmanagementsystem.member.effectiveUntil": "${member.getEffectiveUntil()!""}"
<#if member.getData()??>,
"groupmanagementsystem.member.data": [
<#list member.getData() as data>
{
"groupmanagementsystem.member.dataName": "${data.getDataName()!""}",
"groupmanagementsystem.member.dataValue": "${data.getDataValue()!""}"
}
<#if data_has_next>, </#if>
</#list>
]
</#if>
</#if>
}
<#if groupIdAndMember_has_next>, </#if>
</#list>
]
<#else>
</#if>
</#compress>