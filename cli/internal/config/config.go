package config

import (
	"fmt"
	"os"
	"path/filepath"

	"github.com/spf13/viper"
)

const (
	KeyServerURL = "server_url"
	KeyToken     = "token"

	DefaultServerURL = "https://localhost:8080"
)

// LoadConfig initialises viper and reads ~/.opendatamask/config.yaml.
// It does NOT return an error if the file does not exist yet.
func LoadConfig() error {
	configDir, err := configDir()
	if err != nil {
		return err
	}

	viper.SetConfigName("config")
	viper.SetConfigType("yaml")
	viper.AddConfigPath(configDir)

	viper.SetDefault(KeyServerURL, DefaultServerURL)

	if err := viper.ReadInConfig(); err != nil {
		if _, ok := err.(viper.ConfigFileNotFoundError); !ok {
			return fmt.Errorf("reading config: %w", err)
		}
	}
	return nil
}

// SaveToken writes the JWT token to the config file.
func SaveToken(token string) error {
	viper.Set(KeyToken, token)
	return writeConfig()
}

// ClearToken removes the JWT token from the config file.
func ClearToken() error {
	viper.Set(KeyToken, "")
	return writeConfig()
}

// GetToken returns the stored JWT token (may be empty).
func GetToken() string {
	return viper.GetString(KeyToken)
}

// GetServerURL returns the configured server URL.
func GetServerURL() string {
	return viper.GetString(KeyServerURL)
}

// SetServerURL persists the server URL.
func SetServerURL(url string) error {
	viper.Set(KeyServerURL, url)
	return writeConfig()
}

// writeConfig ensures the config directory exists and writes all viper settings.
func writeConfig() error {
	dir, err := configDir()
	if err != nil {
		return err
	}
	if err := os.MkdirAll(dir, 0700); err != nil {
		return fmt.Errorf("creating config dir: %w", err)
	}

	cfgFile := filepath.Join(dir, "config.yaml")
	if err := viper.WriteConfigAs(cfgFile); err != nil {
		return fmt.Errorf("writing config: %w", err)
	}
	// Restrict permissions so only the owner can read the token.
	return os.Chmod(cfgFile, 0600)
}

func configDir() (string, error) {
	home, err := os.UserHomeDir()
	if err != nil {
		return "", fmt.Errorf("finding home dir: %w", err)
	}
	return filepath.Join(home, ".opendatamask"), nil
}
