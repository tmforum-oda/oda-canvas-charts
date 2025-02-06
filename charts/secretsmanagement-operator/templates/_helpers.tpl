{{/*
build the full docker image name from image + version + prereleaseSuffix
*/}}
{{- define "secretsmanagementoperator.dockerimage" -}}
  {{ include "docker.registry" .}}{{- .Values.image -}}:{{- .Values.version -}}
  {{- if .Values.prereleaseSuffix -}}
    -{{- .Values.prereleaseSuffix -}}
  {{- end -}}
{{- end -}}


{{/*
build the full sidecar docker image name from image + version + prereleaseSuffix
*/}}
{{- define "secretsmanagementoperator.sidecarDockerimage" -}}
  {{ include "docker.registry" .}}{{- .Values.sidecarImage -}}:{{- .Values.sidecarVersion -}}
  {{- if .Values.sidecarPrereleaseSuffix -}}
    -{{- .Values.sidecarPrereleaseSuffix -}}
  {{- end -}}
{{- end -}}


{{/*
overwrite imagePullSecret with "Always" if prereleaseSuffix is set
*/}}
{{- define "secretsmanagementoperator.imagePullPolicy" -}}
  {{- if .Values.prereleaseSuffix -}}
    Always
  {{- else -}}
    {{- .Values.imagePullPolicy -}}
  {{- end -}}
{{- end -}}


{{/*
overwrite sidecar imagePullSecret with "Always" if prereleaseSuffix is set
*/}}
{{- define "secretsmanagementoperator.sidecarImagePullPolicy" -}}
  {{- if .Values.sidecarPrereleaseSuffix -}}
    Always
  {{- else -}}
    {{- .Values.sidecarImagePullPolicy -}}
  {{- end -}}
{{- end -}}