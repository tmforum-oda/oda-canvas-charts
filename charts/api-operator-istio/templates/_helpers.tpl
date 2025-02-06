{{/*
Expand the name of the chart.
*/}}
{{- define "api-operator-istio.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "api-operator-istio.fullname" -}}
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
{{- define "api-operator-istio.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "api-operator-istio.labels" -}}
helm.sh/chart: {{ include "api-operator-istio.chart" . }}
{{ include "api-operator-istio.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "api-operator-istio.selectorLabels" -}}
app.kubernetes.io/name: {{ include "api-operator-istio.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "api-operator-istio.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "api-operator-istio.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
build the full apiop docker image name from image + version + prereleaseSuffix
*/}}
{{- define "api-operator-istio.apiopDockerimage" -}}
  {{ include "docker.registry" .}}{{- .Values.deployment.apiopImage -}}:{{- .Values.deployment.apiopVersion -}}
  {{- if .Values.deployment.apiopPrereleaseSuffix -}}
    -{{- .Values.deployment.apiopPrereleaseSuffix -}}
  {{- end -}}
{{- end -}}


{{/*
overwrite apiop imagePullSecret with "Always" if prereleaseSuffix is set
*/}}
{{- define "api-operator-istio.apiopImagePullPolicy" -}}
  {{- if .Values.deployment.apiopPrereleaseSuffix -}}
    Always
  {{- else -}}
    {{- .Values.deployment.apiopImagePullPolicy -}}
  {{- end -}}
{{- end -}}