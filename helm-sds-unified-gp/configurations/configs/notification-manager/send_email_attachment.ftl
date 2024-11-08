<#assign customerData = customer>
<#if customerData.extraFields.brandName?matches("TARAJI", "i")>
<html>
<head>
  <title>title</title>
</head>
<body>
     <h1>Contract Information</h1>
     <p>
     This is the TARAJI brand contract. The amount is ${customerData.extraFields.contractPrice}
     </p>
     <img src="file:///opt/seamless/conf/mail-engine/taraji.jpeg"  width="100" height="100"/>
     <p>Customer Signature</p>
     <img src="http://localhost:8199/getFile?${customerData.uploadReferences.customerSignPath}" width="100" height="100"/>
     <p>Operator Signature</p>
     <img src="http://localhost:8199/getFile?${customerData.uploadReferences.agentSignPath}" width="100" height="100"/>

</body>
</html>

<#elseif customerData.extraFields.brandName?matches("TT", "i")>
<html>
<head>
  <title>title</title>
</head>
<body>
     <h1>Contract Information</h1>
     <p>
     This is the TT brand contract. The amount is ${customerData.extraFields.contractPrice}
     </p>
     <img src="file:///opt/seamless/conf/mail-engine/tt.jpeg"  width="100" height="100"/>
     <p>Customer Signature</p>
     <img src="http://localhost:8199/getFile?${customerData.uploadReferences.customerSignPath}" width="100" height="100"/>
     <p>Operator Signature</p>
     <img src="http://localhost:8199/getFile?${customerData.uploadReferences.agentSignPath}" width="100" height="100"/>
</body>
</html>

</#if>