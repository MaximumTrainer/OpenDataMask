package cmd

import (
	"fmt"
	"os"

	"github.com/opendatamask/cli/internal/api"
	"github.com/opendatamask/cli/internal/config"
	"github.com/spf13/cobra"
	"github.com/spf13/viper"
)

var (
	// apiClient is the global HTTP client, initialised in PersistentPreRunE.
	apiClient *api.Client

	// flags
	flagServer   string
	flagToken    string
	flagInsecure bool
)

var rootCmd = &cobra.Command{
	Use:   "odm",
	Short: "OpenDataMask CLI",
	Long: `odm is the command-line interface for OpenDataMask.

It connects to the OpenDataMask API server to manage workspaces,
data connections, and masking jobs.`,
	SilenceUsage: true,
}

// Execute runs the root command.
func Execute() {
	if err := rootCmd.Execute(); err != nil {
		os.Exit(1)
	}
}

func init() {
	cobra.OnInitialize(initConfig)

	rootCmd.PersistentFlags().StringVar(&flagServer, "server", "", "OpenDataMask server URL (overrides config)")
	rootCmd.PersistentFlags().StringVar(&flagToken, "token", "", "JWT token (overrides stored token)")
	rootCmd.PersistentFlags().BoolVar(&flagInsecure, "insecure", false, "Skip TLS certificate verification (not for production)")

	rootCmd.AddCommand(authCmd)
	rootCmd.AddCommand(workspaceCmd)
	rootCmd.AddCommand(connectionCmd)
	rootCmd.AddCommand(jobCmd)
}

func initConfig() {
	if err := config.LoadConfig(); err != nil {
		fmt.Fprintln(os.Stderr, "Warning: could not load config:", err)
	}

	// CLI flags override config file values.
	if flagServer != "" {
		viper.Set(config.KeyServerURL, flagServer)
	}
	if flagToken != "" {
		viper.Set(config.KeyToken, flagToken)
	}

	apiClient = api.NewClient(config.GetServerURL(), config.GetToken(), flagInsecure)
}
