import apiClient from './client'
import type { GeneratorType } from '@/types'

export enum SensitivityType {
  FIRST_NAME = 'FIRST_NAME',
  LAST_NAME = 'LAST_NAME',
  FULL_NAME = 'FULL_NAME',
  EMAIL = 'EMAIL',
  PHONE = 'PHONE',
  STREET_ADDRESS = 'STREET_ADDRESS',
  ZIP_CODE = 'ZIP_CODE',
  CITY = 'CITY',
  STATE = 'STATE',
  COUNTRY = 'COUNTRY',
  POSTAL_CODE = 'POSTAL_CODE',
  GPS_COORDINATES = 'GPS_COORDINATES',
  USERNAME = 'USERNAME',
  PASSWORD = 'PASSWORD',
  CREDIT_CARD = 'CREDIT_CARD',
  IBAN = 'IBAN',
  SWIFT_CODE = 'SWIFT_CODE',
  MONEY_AMOUNT = 'MONEY_AMOUNT',
  BTC_ADDRESS = 'BTC_ADDRESS',
  SSN = 'SSN',
  PASSPORT_NUMBER = 'PASSPORT_NUMBER',
  DRIVERS_LICENSE = 'DRIVERS_LICENSE',
  BIRTH_DATE = 'BIRTH_DATE',
  GENDER = 'GENDER',
  BIOMETRIC = 'BIOMETRIC',
  ICD_CODE = 'ICD_CODE',
  MEDICAL_RECORD_NUMBER = 'MEDICAL_RECORD_NUMBER',
  HEALTH_PLAN_NUMBER = 'HEALTH_PLAN_NUMBER',
  ACCOUNT_NUMBER = 'ACCOUNT_NUMBER',
  IP_ADDRESS = 'IP_ADDRESS',
  IPV6_ADDRESS = 'IPV6_ADDRESS',
  MAC_ADDRESS = 'MAC_ADDRESS',
  URL = 'URL',
  VIN = 'VIN',
  LICENSE_PLATE = 'LICENSE_PLATE',
  ORGANIZATION = 'ORGANIZATION',
  UNKNOWN = 'UNKNOWN'
}

export enum ConfidenceLevel {
  HIGH = 'HIGH',
  MEDIUM = 'MEDIUM',
  LOW = 'LOW',
  FULL = 'FULL'
}

export interface SensitivityScanLog {
  id: number
  workspaceId: number
  startedAt: string
  completedAt: string | null
  status: string
  tablesScanned: number
  columnsScanned: number
  sensitiveColumnsFound: number
  errorMessage: string | null
}

export interface ColumnSensitivity {
  id: number
  workspaceId: number
  tableName: string
  columnName: string
  isSensitive: boolean
  sensitivityType: SensitivityType
  confidenceLevel: ConfidenceLevel
  recommendedGeneratorType: GeneratorType | null
  detectedAt: string
}

export interface SensitivityScanLogResponse {
  id: number
  workspaceId: number
  startedAt: string
  completedAt: string | null
  status: string
  tablesScanned: number
  columnsScanned: number
  sensitiveColumnsFound: number
  errorMessage: string | null
}

export interface UpdateColumnSensitivityRequest {
  isSensitive: boolean
  sensitivityType: SensitivityType
  confidenceLevel: ConfidenceLevel
}

export async function runSensitivityScan(workspaceId: number): Promise<SensitivityScanLog> {
  const { data } = await apiClient.post<SensitivityScanLog>(
    `/workspaces/${workspaceId}/sensitivity-scan/run`
  )
  return data
}

export async function getSensitivityScanStatus(workspaceId: number): Promise<SensitivityScanLog | null> {
  const { data } = await apiClient.get<SensitivityScanLog | null>(
    `/workspaces/${workspaceId}/sensitivity-scan/status`
  )
  return data
}

export async function getSensitivityScanResults(workspaceId: number): Promise<ColumnSensitivity[]> {
  const { data } = await apiClient.get<ColumnSensitivity[]>(
    `/workspaces/${workspaceId}/sensitivity-scan/results`
  )
  return data
}

export async function getSensitivityScanLog(workspaceId: number): Promise<SensitivityScanLogResponse[]> {
  const { data } = await apiClient.get<SensitivityScanLogResponse[]>(
    `/workspaces/${workspaceId}/sensitivity-scan/log`
  )
  return data
}

export async function downloadSensitivityScanLog(workspaceId: number): Promise<Blob> {
  const { data } = await apiClient.get(`/workspaces/${workspaceId}/sensitivity-scan/log/download`, {
    responseType: 'blob'
  })
  return data
}

export async function updateColumnSensitivity(
  workspaceId: number,
  table: string,
  column: string,
  payload: UpdateColumnSensitivityRequest
): Promise<ColumnSensitivity> {
  const { data } = await apiClient.patch<ColumnSensitivity>(
    `/workspaces/${workspaceId}/sensitivity-scan/columns/${table}/${column}`,
    payload
  )
  return data
}
