{{/*
Expand the name of the chart.
*/}}
{{- define "canvas-oda.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "canvas-oda.fullname" -}}
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
{{- define "canvas-oda.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "canvas-oda.labels" -}}
helm.sh/chart: {{ include "canvas-oda.chart" . }}
{{ include "canvas-oda.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "canvas-oda.selectorLabels" -}}
app.kubernetes.io/name: {{ include "canvas-oda.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "canvas-oda.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "canvas-oda.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Custom/private docker image registry
*/}}
{{- define "docker.registry" -}}
{{- if .Values.global.image.registry }}
{{- $registry := (trimSuffix "/" .Values.global.image.registry) }}
{{- printf "%s/" $registry }}
{{- end }}
{{- end }}


{{/*
Create image pull secrets
*/}}
{{- define "image-pull-secrets" -}}
{{- range .Values.global.imagePullSecrets }}
  {{- if eq (typeOf .) "map[string]interface {}" }}
- {{ toYaml . | trim }}
  {{- else }}
- name: {{ . }}
  {{- end }}
{{- end }}
{{- end -}}