{{/*
Expand the name of the chart.
*/}}
{{- define "identityconfig-operator-keycloak.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "identityconfig-operator-keycloak.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "identityconfig-operator-keycloak.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "identityconfig-operator-keycloak.labels" -}}
helm.sh/chart: {{ include "identityconfig-operator-keycloak.chart" . }}
{{ include "identityconfig-operator-keycloak.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "identityconfig-operator-keycloak.selectorLabels" -}}
app.kubernetes.io/name: {{ include "identityconfig-operator-keycloak.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "identityconfig-operator-keycloak.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "identityconfig-operator-keycloak.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
build the full idkop docker image name from image + version + prereleaseSuffix
*/}}
{{- define "identityconfig-operator-keycloak.idkopImage" -}}
  {{ include "docker.registry" .}}{{- .Values.deployment.idkopImage -}}:{{- .Values.deployment.idkopVersion -}}
  {{- if .Values.deployment.idkopPrereleaseSuffix -}}
    -{{- .Values.deployment.idkopPrereleaseSuffix -}}
  {{- end -}}
{{- end -}}


{{/*
build the full idlistkey docker image name from image + version + prereleaseSuffix
*/}}
{{- define "identityconfig-operator-keycloak.idlistkeyImage" -}}
  {{ include "docker.registry" .}}{{- .Values.deployment.idlistkeyImage -}}:{{- .Values.deployment.idlistkeyVersion -}}
  {{- if .Values.deployment.idlistkeyPrereleaseSuffix -}}
    -{{- .Values.deployment.idlistkeyPrereleaseSuffix -}}
  {{- end -}}
{{- end -}}


{{/*
overwrite idkop imagePullSecret with "Always" if prereleaseSuffix is set
*/}}
{{- define "identityconfig-operator-keycloak.imagePullPolicy" -}}
  {{- if .Values.deployment.idkopPrereleaseSuffix -}}
    Always
  {{- else -}}
    {{- .Values.deployment.imagePullPolicy -}}
  {{- end -}}
{{- end -}}
