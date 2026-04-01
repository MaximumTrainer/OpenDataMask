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
  FILE = 'FILE'
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
  CUSTOM = 'CUSTOM'
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
