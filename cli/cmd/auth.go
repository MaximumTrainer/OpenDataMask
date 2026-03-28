package cmd

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"strings"

	"github.com/opendatamask/cli/internal/config"
	"github.com/spf13/cobra"
)

var authCmd = &cobra.Command{
	Use:   "auth",
	Short: "Authenticate with the OpenDataMask API",
}

// ---- login ---------------------------------------------------------------

var (
	loginUsername string
	loginPassword string
)

var loginCmd = &cobra.Command{
	Use:   "login",
	Short: "Authenticate and save JWT token",
	RunE: func(cmd *cobra.Command, args []string) error {
		resp, err := apiClient.Login(loginUsername, loginPassword)
		if err != nil {
			return fmt.Errorf("login failed: %w", err)
		}

		if err := config.SaveToken(resp.Token); err != nil {
			return fmt.Errorf("saving token: %w", err)
		}

		fmt.Printf("Logged in as %s (%s)\n", resp.Username, resp.Role)
		return nil
	},
}

// ---- logout --------------------------------------------------------------

var logoutCmd = &cobra.Command{
	Use:   "logout",
	Short: "Remove the stored JWT token",
	RunE: func(cmd *cobra.Command, args []string) error {
		if err := config.ClearToken(); err != nil {
			return fmt.Errorf("clearing token: %w", err)
		}
		fmt.Println("Logged out successfully.")
		return nil
	},
}

// ---- whoami --------------------------------------------------------------

var whoamiCmd = &cobra.Command{
	Use:   "whoami",
	Short: "Show information about the currently authenticated user",
	RunE: func(cmd *cobra.Command, args []string) error {
		token := config.GetToken()
		if token == "" {
			return fmt.Errorf("not logged in – run 'odm auth login' first")
		}

		username, email, role, err := parseJWTClaims(token)
		if err != nil {
			return fmt.Errorf("parsing token: %w", err)
		}

		fmt.Printf("Username : %s\n", username)
		fmt.Printf("Email    : %s\n", email)
		fmt.Printf("Role     : %s\n", role)
		fmt.Printf("Server   : %s\n", config.GetServerURL())
		return nil
	},
}

// parseJWTClaims extracts claims from a JWT without verifying the signature.
// Signature verification is the server's responsibility; here we just display
// the human-readable information already trusted and stored locally.
func parseJWTClaims(token string) (username, email, role string, err error) {
	parts := strings.Split(token, ".")
	if len(parts) != 3 {
		return "", "", "", fmt.Errorf("invalid JWT format")
	}

	// JWT uses raw base64url (no padding).
	payload, err := base64.RawURLEncoding.DecodeString(parts[1])
	if err != nil {
		return "", "", "", fmt.Errorf("decoding payload: %w", err)
	}

	var claims map[string]any
	if err := json.Unmarshal(payload, &claims); err != nil {
		return "", "", "", fmt.Errorf("parsing claims: %w", err)
	}

	username, _ = claims["sub"].(string)
	email, _ = claims["email"].(string)
	role, _ = claims["role"].(string)
	return username, email, role, nil
}

func init() {
	loginCmd.Flags().StringVarP(&loginUsername, "username", "u", "", "Username (required)")
	loginCmd.Flags().StringVarP(&loginPassword, "password", "p", "", "Password (required)")
	loginCmd.MarkFlagRequired("username") //nolint:errcheck
	loginCmd.MarkFlagRequired("password") //nolint:errcheck

	authCmd.AddCommand(loginCmd)
	authCmd.AddCommand(logoutCmd)
	authCmd.AddCommand(whoamiCmd)
}
