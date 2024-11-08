# Standard Helm Charts For ERS 5.0 
Helm Repo For GP S&amp;D

## Any Components With Normal Directory Structure/Files, Use Below Helm Pattern For Creating Config
```
data:
{{ (.Files.Glob "configs/access-management-system/*").AsConfig | indent 2 }}
```

## Any Components With Normal Directory Structure/Binary_Files, Use Below Helm Pattern For Creating Config
```
binaryData:
{{- $files := .Files }}
{{- range tuple "configs/coreproxy/digitalwarehouse.key" }}
  {{ (base .) }}: |-
{{ $files.Get . | b64enc | indent 4 }}
{{- end }}
data:
{{- range $path, $_ := .Files.Glob "configs/coreproxy/*" }}
{{- if and (not (hasSuffix ".key" $path)) }}
  {{ regexReplaceAll "(.*)/" $path "" }}: |-
{{ $.Files.Get $path | indent 4 }}
{{- end }}
{{- end }}
```

## If Any/Many File(s) Need To Be Skipped
```
binaryData:
{{- $files := .Files }}
{{- range tuple "configs/integration-services/keystore.pkcs12" }}
  {{ (base .) }}: |-
{{ $files.Get . | b64enc | indent 4 }}
{{- end }}
data:
{{- range $path, $_ := .Files.Glob "configs/integration-services/*" }}
{{- if and (not (hasSuffix ".pkcs12" $path)) (not (hasSuffix ".txt" $path)) }}
  {{ regexReplaceAll "(.*)/" $path "" }}: |-
{{ $.Files.Get $path | indent 4 }}
{{- end }}
{{- end }}
{{ tpl (.Files.Glob "configs/integration-services/*.txt").AsConfig . | indent 2 }}
```
