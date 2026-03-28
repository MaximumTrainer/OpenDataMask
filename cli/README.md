# odm – OpenDataMask CLI

`odm` is the command-line interface for [OpenDataMask](https://github.com/opendatamask/OpenDataMask). It lets you manage workspaces, data connections, and masking jobs directly from the terminal.

---

## Installation

### Pre-built binary

Download the latest release from the [Releases page](https://github.com/opendatamask/OpenDataMask/releases) and place `odm` on your `PATH`.

### Build from source

```bash
cd cli
go build -o odm .
sudo mv odm /usr/local/bin/
```

### Docker

```bash
docker build -t odm .
docker run --rm odm --help
```

---

## Configuration

`odm` stores its configuration in `~/.opendatamask/config.yaml` (permissions `0600`).

| Key          | Description                      | Default                   |
|--------------|----------------------------------|---------------------------|
| `server_url` | OpenDataMask API base URL        | `https://localhost:8080`  |
| `token`      | JWT token (written by `login`)   | *(empty)*                 |

You can override values at runtime with global flags:

```
--server   <url>   Override the server URL
--token    <jwt>   Override the stored token
--insecure         Skip TLS certificate verification (dev only)
```

---

## Quick start

```bash
# 1. Point the CLI at your server (saved to config)
odm --server https://api.example.com auth login --username alice --password s3cr3t

# 2. Confirm who you are
odm auth whoami

# 3. Create a workspace
odm workspace create --name "Production" --description "Prod masking workspace"

# 4. List workspaces
odm workspace list

# 5. Add a connection
odm connection create 1 \
  --name "Source DB" \
  --type POSTGRESQL \
  --connection-string "host=db port=5432 sslmode=require" \
  --source

# 6. Run a masking job
odm job run 1

# 7. Check job status
odm job status 1 42

# 8. Log out
odm auth logout
```

---

## Commands

### Authentication (`odm auth`)

| Command | Description |
|---------|-------------|
| `odm auth login --username <u> --password <p>` | Authenticate and store token |
| `odm auth logout` | Remove stored token |
| `odm auth whoami` | Show current user info from stored token |

### Workspaces (`odm workspace` / `odm ws`)

| Command | Description |
|---------|-------------|
| `odm workspace list` | List all workspaces |
| `odm workspace get <id>` | Show workspace details |
| `odm workspace create --name <n> [--description <d>]` | Create a workspace |
| `odm workspace delete <id>` | Delete a workspace |

### Connections (`odm connection` / `odm conn`)

| Command | Description |
|---------|-------------|
| `odm connection list <workspace-id>` | List connections in a workspace |
| `odm connection create <workspace-id> --name <n> --type <t> --connection-string <cs>` | Create a connection |
| `odm connection test <workspace-id> <connection-id>` | Test a connection |
| `odm connection delete <workspace-id> <connection-id>` | Delete a connection |

Supported `--type` values: `POSTGRESQL`, `MONGODB`, `AZURE_SQL`, `MONGODB_COSMOS`

Optional `connection create` flags: `--username`, `--password`, `--database`, `--source`, `--destination`

### Jobs (`odm job`)

| Command | Description |
|---------|-------------|
| `odm job list <workspace-id>` | List jobs |
| `odm job run <workspace-id>` | Create and start a masking job |
| `odm job status <workspace-id> <job-id>` | Get job status |
| `odm job logs <workspace-id> <job-id>` | Retrieve job logs |
| `odm job cancel <workspace-id> <job-id>` | Cancel a running job |

---

## TLS / HTTPS

All connections use HTTPS by default with a minimum TLS version of 1.2.

To connect to a server with a self-signed certificate during development:

```bash
odm --insecure auth login --username admin --password admin
```

> ⚠️ Never use `--insecure` in production.

---

## Environment

The CLI respects the following environment variables (lower priority than CLI flags, higher than config file defaults):

| Variable | Equivalent flag |
|----------|-----------------|
| *(none)* | All config via file or flags |

---

## License

Apache-2.0 – see [LICENSE](../LICENSE).
