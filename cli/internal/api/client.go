package api

import (
	"bytes"
	"crypto/tls"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"time"
)

// Client is the HTTP client for the OpenDataMask API.
type Client struct {
	baseURL    string
	token      string
	httpClient *http.Client
}

// NewClient creates a new API client.
// If insecure is true TLS certificate verification is skipped (development only).
func NewClient(baseURL, token string, insecure bool) *Client {
	transport := &http.Transport{
		TLSClientConfig: &tls.Config{
			InsecureSkipVerify: insecure, //nolint:gosec // opt-in flag for dev environments
			MinVersion:         tls.VersionTLS12,
		},
	}
	return &Client{
		baseURL: baseURL,
		token:   token,
		httpClient: &http.Client{
			Timeout:   30 * time.Second,
			Transport: transport,
		},
	}
}

// ---- Auth ---------------------------------------------------------------

type LoginRequest struct {
	Username string `json:"username"`
	Password string `json:"password"`
}

type RegisterRequest struct {
	Username string `json:"username"`
	Email    string `json:"email"`
	Password string `json:"password"`
	Role     string `json:"role,omitempty"`
}

type AuthResponse struct {
	Token    string `json:"token"`
	Username string `json:"username"`
	Email    string `json:"email"`
	Role     string `json:"role"`
}

func (c *Client) Login(username, password string) (*AuthResponse, error) {
	return post[AuthResponse](c, "/api/auth/login", LoginRequest{Username: username, Password: password}, false)
}

func (c *Client) Register(username, email, password, role string) (*AuthResponse, error) {
	return post[AuthResponse](c, "/api/auth/register", RegisterRequest{
		Username: username, Email: email, Password: password, Role: role,
	}, false)
}

func (c *Client) WhoAmI() (*AuthResponse, error) {
	// Decode the JWT claims locally so we don't need a dedicated /me endpoint.
	// Instead we call a lightweight workspace list to verify the token is valid
	// and return the username from the stored token claim.
	// We expose a helper that simply re-uses stored info rather than a round-trip.
	return nil, fmt.Errorf("use GetStoredUserInfo instead")
}

// ---- Workspaces ---------------------------------------------------------

type WorkspaceRequest struct {
	Name        string `json:"name"`
	Description string `json:"description,omitempty"`
}

type WorkspaceResponse struct {
	ID          int64  `json:"id"`
	Name        string `json:"name"`
	Description string `json:"description"`
	OwnerID     int64  `json:"ownerId"`
	CreatedAt   string `json:"createdAt"`
	UpdatedAt   string `json:"updatedAt"`
}

func (c *Client) ListWorkspaces() ([]WorkspaceResponse, error) {
	return get[[]WorkspaceResponse](c, "/api/workspaces")
}

func (c *Client) GetWorkspace(id string) (*WorkspaceResponse, error) {
	ws, err := get[WorkspaceResponse](c, "/api/workspaces/"+id)
	if err != nil {
		return nil, err
	}
	return &ws, nil
}

func (c *Client) CreateWorkspace(name, description string) (*WorkspaceResponse, error) {
	return post[WorkspaceResponse](c, "/api/workspaces", WorkspaceRequest{Name: name, Description: description}, true)
}

func (c *Client) DeleteWorkspace(id string) error {
	return del(c, "/api/workspaces/"+id)
}

// ---- Connections --------------------------------------------------------

type DataConnectionRequest struct {
	Name             string `json:"name"`
	Type             string `json:"type"`
	ConnectionString string `json:"connectionString"`
	Username         string `json:"username,omitempty"`
	Password         string `json:"password,omitempty"`
	Database         string `json:"database,omitempty"`
	IsSource         bool   `json:"isSource"`
	IsDestination    bool   `json:"isDestination"`
}

type DataConnectionResponse struct {
	ID               int64  `json:"id"`
	WorkspaceID      int64  `json:"workspaceId"`
	Name             string `json:"name"`
	Type             string `json:"type"`
	ConnectionString string `json:"connectionString"`
	Username         string `json:"username"`
	Database         string `json:"database"`
	IsSource         bool   `json:"isSource"`
	IsDestination    bool   `json:"isDestination"`
	CreatedAt        string `json:"createdAt"`
}

type ConnectionTestResult struct {
	Success bool   `json:"success"`
	Message string `json:"message"`
}

func (c *Client) ListConnections(workspaceID string) ([]DataConnectionResponse, error) {
	return get[[]DataConnectionResponse](c, "/api/workspaces/"+workspaceID+"/connections")
}

func (c *Client) CreateConnection(workspaceID string, req DataConnectionRequest) (*DataConnectionResponse, error) {
	return post[DataConnectionResponse](c, "/api/workspaces/"+workspaceID+"/connections", req, true)
}

func (c *Client) TestConnection(workspaceID, connectionID string) (*ConnectionTestResult, error) {
	return post[ConnectionTestResult](c, "/api/workspaces/"+workspaceID+"/connections/"+connectionID+"/test", nil, true)
}

func (c *Client) DeleteConnection(workspaceID, connectionID string) error {
	return del(c, "/api/workspaces/"+workspaceID+"/connections/"+connectionID)
}

// ---- Jobs ---------------------------------------------------------------

type JobResponse struct {
	ID           int64  `json:"id"`
	WorkspaceID  int64  `json:"workspaceId"`
	Status       string `json:"status"`
	StartedAt    string `json:"startedAt"`
	CompletedAt  string `json:"completedAt"`
	CreatedAt    string `json:"createdAt"`
	ErrorMessage string `json:"errorMessage"`
	CreatedBy    int64  `json:"createdBy"`
}

type JobLogResponse struct {
	ID        int64  `json:"id"`
	JobID     int64  `json:"jobId"`
	Level     string `json:"level"`
	Message   string `json:"message"`
	CreatedAt string `json:"createdAt"`
}

func (c *Client) ListJobs(workspaceID string) ([]JobResponse, error) {
	return get[[]JobResponse](c, "/api/workspaces/"+workspaceID+"/jobs")
}

func (c *Client) RunJob(workspaceID string) (*JobResponse, error) {
	return post[JobResponse](c, "/api/workspaces/"+workspaceID+"/jobs", nil, true)
}

func (c *Client) GetJob(workspaceID, jobID string) (*JobResponse, error) {
	job, err := get[JobResponse](c, "/api/workspaces/"+workspaceID+"/jobs/"+jobID)
	if err != nil {
		return nil, err
	}
	return &job, nil
}

func (c *Client) GetJobLogs(workspaceID, jobID string) ([]JobLogResponse, error) {
	return get[[]JobLogResponse](c, "/api/workspaces/"+workspaceID+"/jobs/"+jobID+"/logs")
}

func (c *Client) CancelJob(workspaceID, jobID string) (*JobResponse, error) {
	return post[JobResponse](c, "/api/workspaces/"+workspaceID+"/jobs/"+jobID+"/cancel", nil, true)
}

// ---- generic HTTP helpers -----------------------------------------------

func get[T any](c *Client, path string) (T, error) {
	var zero T
	req, err := http.NewRequest(http.MethodGet, c.baseURL+path, nil)
	if err != nil {
		return zero, err
	}
	return doRequest[T](c, req)
}

func post[T any](c *Client, path string, body any, auth bool) (*T, error) {
	var buf io.Reader
	if body != nil {
		data, err := json.Marshal(body)
		if err != nil {
			return nil, err
		}
		buf = bytes.NewBuffer(data)
	}

	req, err := http.NewRequest(http.MethodPost, c.baseURL+path, buf)
	if err != nil {
		return nil, err
	}
	if body != nil {
		req.Header.Set("Content-Type", "application/json")
	}
	if !auth {
		// unauthenticated endpoints (login/register)
		resp, err := doRequestRaw(c, req)
		if err != nil {
			return nil, err
		}
		var result T
		if err := json.Unmarshal(resp, &result); err != nil {
			return nil, fmt.Errorf("decoding response: %w", err)
		}
		return &result, nil
	}
	result, err := doRequest[T](c, req)
	if err != nil {
		return nil, err
	}
	return &result, nil
}

func del(c *Client, path string) error {
	req, err := http.NewRequest(http.MethodDelete, c.baseURL+path, nil)
	if err != nil {
		return err
	}
	req.Header.Set("Authorization", "Bearer "+c.token)
	resp, err := c.httpClient.Do(req)
	if err != nil {
		return fmt.Errorf("request failed: %w", err)
	}
	defer resp.Body.Close()
	if resp.StatusCode >= 400 {
		body, _ := io.ReadAll(resp.Body)
		return fmt.Errorf("server returned %d: %s", resp.StatusCode, string(body))
	}
	return nil
}

func doRequest[T any](c *Client, req *http.Request) (T, error) {
	var zero T
	req.Header.Set("Authorization", "Bearer "+c.token)
	body, err := doRequestRaw(c, req)
	if err != nil {
		return zero, err
	}
	var result T
	if err := json.Unmarshal(body, &result); err != nil {
		return zero, fmt.Errorf("decoding response: %w", err)
	}
	return result, nil
}

func doRequestRaw(c *Client, req *http.Request) ([]byte, error) {
	req.Header.Set("Accept", "application/json")
	resp, err := c.httpClient.Do(req)
	if err != nil {
		return nil, fmt.Errorf("request failed: %w", err)
	}
	defer resp.Body.Close()
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return nil, fmt.Errorf("reading response: %w", err)
	}
	if resp.StatusCode >= 400 {
		return nil, fmt.Errorf("server returned %d: %s", resp.StatusCode, string(body))
	}
	return body, nil
}
