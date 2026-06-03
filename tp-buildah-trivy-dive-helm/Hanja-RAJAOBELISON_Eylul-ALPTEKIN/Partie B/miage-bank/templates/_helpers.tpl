{{/*
Nom du chart.
*/}}
{{- define "miage-bank.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Nom complet (release + chart).
*/}}
{{- define "miage-bank.fullname" -}}
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
Labels communs (chart + version + managed-by).
*/}}
{{- define "miage-bank.labels" -}}
helm.sh/chart: {{ include "miage-bank.name" . }}-{{ .Chart.Version | replace "+" "_" }}
{{ include "miage-bank.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Labels de sélection (stables — utilisés par les Deployments et Services).
*/}}
{{- define "miage-bank.selectorLabels" -}}
app.kubernetes.io/name: {{ include "miage-bank.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Nom du ServiceAccount.
*/}}
{{- define "miage-bank.serviceAccountName" -}}
{{- .Values.serviceAccount.name | default "miage-bank-sa" }}
{{- end }}
