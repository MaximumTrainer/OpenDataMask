// ── Auth ──────────────────────────────────────────────────────────────────

export enum UserRole {
  ADMIN = 'ADMIN',
  USER = 'USER'
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
  MYSQL = 'MYSQL',
  MARIADB = 'MARIADB',
  MSSQL = 'MSSQL',
  ORACLE = 'ORACLE',
  SQLITE = 'SQLITE',
  MONGODB = 'MONGODB'
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
  PASSTHROUGH = 'PASSTHROUGH',
  NULLIFY = 'NULLIFY',
  RANDOM_STRING = 'RANDOM_STRING',
  RANDOM_NUMBER = 'RANDOM_NUMBER',
  RANDOM_EMAIL = 'RANDOM_EMAIL',
  RANDOM_PHONE = 'RANDOM_PHONE',
  RANDOM_NAME = 'RANDOM_NAME',
  RANDOM_UUID = 'RANDOM_UUID',
  RANDOM_DATE = 'RANDOM_DATE',
  HASH_MD5 = 'HASH_MD5',
  HASH_SHA256 = 'HASH_SHA256',
  FIXED_VALUE = 'FIXED_VALUE',
  LOOKUP = 'LOOKUP',
  REGEX = 'REGEX',
  SEQUENCE = 'SEQUENCE',
  FAKER = 'FAKER'
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
