# ce fichier sert à définir des variables globales pour le chart
# il est appelé dans les autres templates
{{/* Expand the name of the chart. */}}
{{- define "miage-bank.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/* Create a default fully qualified app name. */}}
{{- define "miage-bank.fullname" -}}
{{- printf "%s" .Release.Name | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/* Common labels */}}
{{- define "miage-bank.labels" -}}
helm.sh/chart: {{ include "miage-bank.name" . }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}
