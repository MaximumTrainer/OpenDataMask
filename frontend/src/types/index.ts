// ── Auth ──────────────────────────────────────────────────────────────────

export enum UserRole {
  ADMIN = 'ADMIN',
  USER = 'USER',
  VIEWER = 'VIEWER'
}

export interface User {
  id: number
  username: string
  email: string
  role: UserRole
  createdAt: string
}

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  email: string
  password: string
  role?: UserRole
}

export interface AuthResponse {
  token: string
  user: User
}

// ── Workspace ─────────────────────────────────────────────────────────────

export interface Workspace {
  id: number
  name: string
  description: string
  ownerId: number
  ownerUsername: string
  createdAt: string
  updatedAt: string
}

export interface WorkspaceRequest {
  name: string
  description?: string
}

// ── Data Connection ───────────────────────────────────────────────────────

export enum ConnectionType {
  POSTGRESQL = 'POSTGRESQL',
  MONGODB = 'MONGODB',
  AZURE_SQL = 'AZURE_SQL',
  MONGODB_COSMOS = 'MONGODB_COSMOS',
  FILE = 'FILE',
  MYSQL = 'MYSQL'
}

export interface DataConnection {
  id: number
  workspaceId: number
  name: string
  type: ConnectionType
  host: string
  port: number
  database: string
  username: string
  /** password is never returned from the API */
  sslEnabled: boolean
  createdAt: string
  updatedAt: string
}

export interface DataConnectionRequest {
  name: string
  type: ConnectionType
  host: string
  port: number
  database: string
  username: string
  password: string
  sslEnabled?: boolean
}

export interface ConnectionTestResult {
  success: boolean
  message: string
}

// ── Table Configuration ───────────────────────────────────────────────────

export enum TableMode {
  PASSTHROUGH = 'PASSTHROUGH',
  MASK = 'MASK',
  GENERATE = 'GENERATE',
  SUBSET = 'SUBSET',
  SKIP = 'SKIP'
}

export interface TableConfiguration {
  id: number
  workspaceId: number
  connectionId: number
  schemaName: string
  tableName: string
  mode: TableMode
  whereClause?: string
  createdAt: string
  updatedAt: string
  columnGenerators: ColumnGenerator[]
}

export interface TableConfigurationRequest {
  connectionId: number
  schemaName: string
  tableName: string
  mode: TableMode
  whereClause?: string
}

// ── Column Generator ──────────────────────────────────────────────────────

export enum GeneratorType {
  NAME = 'NAME',
  EMAIL = 'EMAIL',
  PHONE = 'PHONE',
  ADDRESS = 'ADDRESS',
  SSN = 'SSN',
  CREDIT_CARD = 'CREDIT_CARD',
  DATE = 'DATE',
  UUID = 'UUID',
  CONSTANT = 'CONSTANT',
  NULL = 'NULL',
  CUSTOM = 'CUSTOM',
  // Name variants
  FIRST_NAME = 'FIRST_NAME',
  LAST_NAME = 'LAST_NAME',
  FULL_NAME = 'FULL_NAME',
  // Location
  STREET_ADDRESS = 'STREET_ADDRESS',
  CITY = 'CITY',
  STATE = 'STATE',
  ZIP_CODE = 'ZIP_CODE',
  COUNTRY = 'COUNTRY',
  POSTAL_CODE = 'POSTAL_CODE',
  GPS_COORDINATES = 'GPS_COORDINATES',
  // Credentials
  USERNAME = 'USERNAME',
  PASSWORD = 'PASSWORD',
  // Financial
  IBAN = 'IBAN',
  SWIFT_CODE = 'SWIFT_CODE',
  MONEY_AMOUNT = 'MONEY_AMOUNT',
  BTC_ADDRESS = 'BTC_ADDRESS',
  // Identification
  PASSPORT_NUMBER = 'PASSPORT_NUMBER',
  DRIVERS_LICENSE = 'DRIVERS_LICENSE',
  BIRTH_DATE = 'BIRTH_DATE',
  GENDER = 'GENDER',
  // Medical
  ICD_CODE = 'ICD_CODE',
  MEDICAL_RECORD_NUMBER = 'MEDICAL_RECORD_NUMBER',
  HEALTH_PLAN_NUMBER = 'HEALTH_PLAN_NUMBER',
  // Network
  IP_ADDRESS = 'IP_ADDRESS',
  IPV6_ADDRESS = 'IPV6_ADDRESS',
  MAC_ADDRESS = 'MAC_ADDRESS',
  URL = 'URL',
  // Vehicle
  VIN = 'VIN',
  LICENSE_PLATE = 'LICENSE_PLATE',
  // Other
  ORGANIZATION = 'ORGANIZATION',
  ACCOUNT_NUMBER = 'ACCOUNT_NUMBER',
  // Composite / PK generators
  CONDITIONAL = 'CONDITIONAL',
  PARTIAL_MASK = 'PARTIAL_MASK',
  FORMAT_PRESERVING = 'FORMAT_PRESERVING',
  SEQUENTIAL = 'SEQUENTIAL',
  RANDOM_INT = 'RANDOM_INT'
}

export interface ColumnGenerator {
  id: number
  tableConfigId: number
  columnName: string
  generatorType: GeneratorType
  parameters?: Record<string, string>
  createdAt: string
  updatedAt: string
}

export interface ColumnGeneratorRequest {
  columnName: string
  generatorType: GeneratorType
  parameters?: Record<string, string>
}

// ── Job ───────────────────────────────────────────────────────────────────

export enum JobStatus {
  PENDING = 'PENDING',
  RUNNING = 'RUNNING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
  CANCELLED = 'CANCELLED'
}

export interface Job {
  id: number
  workspaceId: number
  name: string
  status: JobStatus
  sourceConnectionId: number
  targetConnectionId: number
  sourceConnectionName: string
  targetConnectionName: string
  startedAt?: string
  completedAt?: string
  errorMessage?: string
  tablesProcessed: number
  tablesTotal: number
  rowsProcessed: number
  createdAt: string
  updatedAt: string
}

export interface JobRequest {
  name: string
  sourceConnectionId: number
  targetConnectionId: number
}

// ── Job Logs ──────────────────────────────────────────────────────────────

export enum LogLevel {
  DEBUG = 'DEBUG',
  INFO = 'INFO',
  WARN = 'WARN',
  ERROR = 'ERROR'
}

export interface JobLog {
  id: number
  jobId: number
  level: LogLevel
  message: string
  timestamp: string
}

// ── Post-Job Action ───────────────────────────────────────────────────────

export enum ActionType {
  WEBHOOK = 'WEBHOOK',
  EMAIL = 'EMAIL',
  SCRIPT = 'SCRIPT'
}

export interface PostJobAction {
  id: number
  workspaceId: number
  actionType: ActionType
  config: string
  enabled: boolean
  createdAt: string
}

export interface PostJobActionRequest {
  actionType: ActionType
  config: string
  enabled?: boolean
}

// ── Pagination ────────────────────────────────────────────────────────────

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export interface ApiError {
  message: string
  status: number
  errors?: Record<string, string>
}

// ── Workspace Stats ───────────────────────────────────────────────────────

export interface WorkspaceStats {
  workspaceId: number
  connectionCount: number
  tableConfigCount: number
  totalJobsRun: number
  lastJobStatus: string | null
  lastJobAt: string | null
}

// ── Workspace Config Export/Import ────────────────────────────────────────

export interface WorkspaceColumnGeneratorExport {
  columnName: string
  generatorType: GeneratorType
  generatorParams: string | null
}

export interface WorkspaceTableConfigExport {
  tableName: string
  schemaName: string | null
  mode: TableMode
  rowLimit: number | null
  whereClause: string | null
  columnGenerators: WorkspaceColumnGeneratorExport[]
}

export interface WorkspaceActionExport {
  actionType: ActionType
  config: string
  enabled: boolean
}

export interface WorkspaceConfigDto {
  version: string
  tables: WorkspaceTableConfigExport[]
  actions: WorkspaceActionExport[]
}
