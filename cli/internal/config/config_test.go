package config

import (
	"os"
	"testing"

	"github.com/spf13/viper"
)

func resetViper() {
	viper.Reset()
}

func TestKeyConstants(t *testing.T) {
	if KeyServerURL == "" {
		t.Error("KeyServerURL should not be empty")
	}
	if KeyToken == "" {
		t.Error("KeyToken should not be empty")
	}
	if DefaultServerURL == "" {
		t.Error("DefaultServerURL should not be empty")
	}
}

func TestConfigRoundTrip(t *testing.T) {
	resetViper()
	tmpDir := t.TempDir()
	t.Setenv("USERPROFILE", tmpDir)
	t.Setenv("HOME", tmpDir)

	if err := LoadConfig(); err != nil {
		t.Fatalf("LoadConfig failed: %v", err)
	}

	const testToken = "test-jwt-token-value"
	if err := SaveToken(testToken); err != nil {
		t.Fatalf("SaveToken failed: %v", err)
	}

	got := GetToken()
	if got != testToken {
		t.Errorf("GetToken() = %q, want %q", got, testToken)
	}

	if err := ClearToken(); err != nil {
		t.Fatalf("ClearToken failed: %v", err)
	}
	if token := GetToken(); token != "" {
		t.Errorf("after ClearToken, GetToken() = %q, want empty", token)
	}
}

func TestGetServerURLDefault(t *testing.T) {
	resetViper()
	tmpDir := t.TempDir()
	t.Setenv("USERPROFILE", tmpDir)
	t.Setenv("HOME", tmpDir)
	os.Unsetenv("OPENDATAMASK_SERVER")

	if err := LoadConfig(); err != nil {
		t.Fatalf("LoadConfig failed: %v", err)
	}

	url := GetServerURL()
	if url == "" {
		t.Error("GetServerURL() should return a non-empty default URL")
	}
	if url != DefaultServerURL {
		t.Errorf("GetServerURL() = %q, want %q", url, DefaultServerURL)
	}
}

func TestSetServerURL(t *testing.T) {
	resetViper()
	tmpDir := t.TempDir()
	t.Setenv("USERPROFILE", tmpDir)
	t.Setenv("HOME", tmpDir)

	if err := LoadConfig(); err != nil {
		t.Fatalf("LoadConfig failed: %v", err)
	}

	const testURL = "https://example.com:8080"
	if err := SetServerURL(testURL); err != nil {
		t.Fatalf("SetServerURL failed: %v", err)
	}

	got := GetServerURL()
	if got != testURL {
		t.Errorf("GetServerURL() = %q, want %q", got, testURL)
	}
}
