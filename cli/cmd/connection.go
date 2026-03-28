package cmd

import (
	"fmt"
	"os"
	"text/tabwriter"

	"github.com/opendatamask/cli/internal/api"
	"github.com/spf13/cobra"
)

var connectionCmd = &cobra.Command{
	Use:     "connection",
	Aliases: []string{"conn"},
	Short:   "Manage data connections",
}

// ---- list ---------------------------------------------------------------

var connectionListCmd = &cobra.Command{
	Use:   "list <workspace-id>",
	Short: "List connections in a workspace",
	Args:  cobra.ExactArgs(1),
	RunE: func(cmd *cobra.Command, args []string) error {
		conns, err := apiClient.ListConnections(args[0])
		if err != nil {
			return err
		}

		w := tabwriter.NewWriter(os.Stdout, 0, 0, 3, ' ', 0)
		fmt.Fprintln(w, "ID\tNAME\tTYPE\tSOURCE\tDESTINATION\tCREATED AT")
		for _, c := range conns {
			fmt.Fprintf(w, "%d\t%s\t%s\t%v\t%v\t%s\n",
				c.ID, c.Name, c.Type, c.IsSource, c.IsDestination, c.CreatedAt)
		}
		return w.Flush()
	},
}

// ---- create -------------------------------------------------------------

var (
	connCreateName             string
	connCreateType             string
	connCreateConnectionString string
	connCreateUsername         string
	connCreatePassword         string
	connCreateDatabase         string
	connCreateIsSource         bool
	connCreateIsDestination    bool
)

var connectionCreateCmd = &cobra.Command{
	Use:   "create <workspace-id>",
	Short: "Create a data connection",
	Args:  cobra.ExactArgs(1),
	RunE: func(cmd *cobra.Command, args []string) error {
		req := api.DataConnectionRequest{
			Name:             connCreateName,
			Type:             connCreateType,
			ConnectionString: connCreateConnectionString,
			Username:         connCreateUsername,
			Password:         connCreatePassword,
			Database:         connCreateDatabase,
			IsSource:         connCreateIsSource,
			IsDestination:    connCreateIsDestination,
		}
		conn, err := apiClient.CreateConnection(args[0], req)
		if err != nil {
			return err
		}
		fmt.Printf("Connection created: id=%d name=%s type=%s\n", conn.ID, conn.Name, conn.Type)
		return nil
	},
}

// ---- test ---------------------------------------------------------------

var connectionTestCmd = &cobra.Command{
	Use:   "test <workspace-id> <connection-id>",
	Short: "Test a data connection",
	Args:  cobra.ExactArgs(2),
	RunE: func(cmd *cobra.Command, args []string) error {
		result, err := apiClient.TestConnection(args[0], args[1])
		if err != nil {
			return err
		}
		if result.Success {
			fmt.Printf("Connection test passed: %s\n", result.Message)
		} else {
			fmt.Printf("Connection test FAILED: %s\n", result.Message)
		}
		return nil
	},
}

// ---- delete -------------------------------------------------------------

var connectionDeleteCmd = &cobra.Command{
	Use:   "delete <workspace-id> <connection-id>",
	Short: "Delete a data connection",
	Args:  cobra.ExactArgs(2),
	RunE: func(cmd *cobra.Command, args []string) error {
		if err := apiClient.DeleteConnection(args[0], args[1]); err != nil {
			return err
		}
		fmt.Printf("Connection %s deleted.\n", args[1])
		return nil
	},
}

func init() {
	connectionCreateCmd.Flags().StringVarP(&connCreateName, "name", "n", "", "Connection name (required)")
	connectionCreateCmd.Flags().StringVarP(&connCreateType, "type", "t", "", "Connection type: POSTGRESQL|MONGODB|AZURE_SQL|MONGODB_COSMOS (required)")
	connectionCreateCmd.Flags().StringVarP(&connCreateConnectionString, "connection-string", "c", "", "Connection string (required)")
	connectionCreateCmd.Flags().StringVar(&connCreateUsername, "username", "", "Database username")
	connectionCreateCmd.Flags().StringVar(&connCreatePassword, "password", "", "Database password")
	connectionCreateCmd.Flags().StringVar(&connCreateDatabase, "database", "", "Database name")
	connectionCreateCmd.Flags().BoolVar(&connCreateIsSource, "source", false, "Mark as source connection")
	connectionCreateCmd.Flags().BoolVar(&connCreateIsDestination, "destination", false, "Mark as destination connection")
	connectionCreateCmd.MarkFlagRequired("name")              //nolint:errcheck
	connectionCreateCmd.MarkFlagRequired("type")              //nolint:errcheck
	connectionCreateCmd.MarkFlagRequired("connection-string") //nolint:errcheck

	connectionCmd.AddCommand(connectionListCmd)
	connectionCmd.AddCommand(connectionCreateCmd)
	connectionCmd.AddCommand(connectionTestCmd)
	connectionCmd.AddCommand(connectionDeleteCmd)
}
