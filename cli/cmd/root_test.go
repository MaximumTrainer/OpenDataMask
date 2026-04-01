package cmd

import (
	"testing"
)

func TestRootCommandSetup(t *testing.T) {
	if rootCmd == nil {
		t.Fatal("rootCmd should not be nil")
	}
	if rootCmd.Use == "" {
		t.Error("rootCmd.Use should not be empty")
	}
}

func TestSubcommandsRegistered(t *testing.T) {
	subcommandNames := make(map[string]bool)
	for _, sub := range rootCmd.Commands() {
		subcommandNames[sub.Name()] = true
	}

	required := []string{"auth", "workspace", "connection", "job"}
	for _, name := range required {
		if !subcommandNames[name] {
			t.Errorf("expected subcommand %q to be registered", name)
		}
	}
}
