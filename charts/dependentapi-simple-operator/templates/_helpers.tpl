{{/*
build the full dependent api docker image name from image + version + prereleaseSuffix
*/}}
{{- define "dependentapi-simple-operator.dockerimage" -}}
  {{ include "docker.registry" .}}{{- .Values.image -}}:{{- .Values.version -}}
  {{- if .Values.prereleaseSuffix -}}
    -{{- .Values.prereleaseSuffix -}}
  {{- end -}}
{{- end -}}


{{/*
overwrite imagePullSecret with "Always" if prereleaseSuffix is set
*/}}
{{- define "dependentapi-simple-operator.imagePullPolicy" -}}
  {{- if .Values.prereleaseSuffix -}}
    Always
  {{- else -}}
    {{- .Values.imagePullPolicy -}}
  {{- end -}}
{{- end -}}


{{/*
build the full dependent api docker image name from image + version + prereleaseSuffix
*/}}
{{- define "dependentapi-serviceinventoryapi.dockerimage" -}}
  {{ include "docker.registry" .}}{{- .Values.serviceInventoryAPI.image -}}:{{- .Values.serviceInventoryAPI.version -}}
  {{- if .Values.serviceInventoryAPI.prereleaseSuffix -}}
    -{{- .Values.serviceInventoryAPI.prereleaseSuffix -}}
  {{- end -}}
{{- end -}}


{{/*
overwrite imagePullSecret with "Always" if prereleaseSuffix is set
*/}}
{{- define "dependentapi-serviceinventoryapi.imagePullPolicy" -}}
  {{- if .Values.serviceInventoryAPI.prereleaseSuffix -}}
    Always
  {{- else -}}
    {{- .Values.serviceInventoryAPI.imagePullPolicy -}}
  {{- end -}}
{{- end -}}