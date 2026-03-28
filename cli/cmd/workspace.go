package cmd

import (
	"fmt"
	"text/tabwriter"
	"os"

	"github.com/spf13/cobra"
)

var workspaceCmd = &cobra.Command{
	Use:     "workspace",
	Aliases: []string{"ws"},
	Short:   "Manage workspaces",
}

// ---- list ---------------------------------------------------------------

var workspaceListCmd = &cobra.Command{
	Use:   "list",
	Short: "List all workspaces",
	RunE: func(cmd *cobra.Command, args []string) error {
		workspaces, err := apiClient.ListWorkspaces()
		if err != nil {
			return err
		}

		w := tabwriter.NewWriter(os.Stdout, 0, 0, 3, ' ', 0)
		fmt.Fprintln(w, "ID\tNAME\tDESCRIPTION\tCREATED AT")
		for _, ws := range workspaces {
			fmt.Fprintf(w, "%d\t%s\t%s\t%s\n", ws.ID, ws.Name, ws.Description, ws.CreatedAt)
		}
		return w.Flush()
	},
}

// ---- get ----------------------------------------------------------------

var workspaceGetCmd = &cobra.Command{
	Use:   "get <workspace-id>",
	Short: "Get workspace details",
	Args:  cobra.ExactArgs(1),
	RunE: func(cmd *cobra.Command, args []string) error {
		ws, err := apiClient.GetWorkspace(args[0])
		if err != nil {
			return err
		}

		fmt.Printf("ID          : %d\n", ws.ID)
		fmt.Printf("Name        : %s\n", ws.Name)
		fmt.Printf("Description : %s\n", ws.Description)
		fmt.Printf("Owner ID    : %d\n", ws.OwnerID)
		fmt.Printf("Created At  : %s\n", ws.CreatedAt)
		fmt.Printf("Updated At  : %s\n", ws.UpdatedAt)
		return nil
	},
}

// ---- create -------------------------------------------------------------

var (
	wsCreateName        string
	wsCreateDescription string
)

var workspaceCreateCmd = &cobra.Command{
	Use:   "create",
	Short: "Create a new workspace",
	RunE: func(cmd *cobra.Command, args []string) error {
		ws, err := apiClient.CreateWorkspace(wsCreateName, wsCreateDescription)
		if err != nil {
			return err
		}
		fmt.Printf("Workspace created: id=%d name=%s\n", ws.ID, ws.Name)
		return nil
	},
}

// ---- delete -------------------------------------------------------------

var workspaceDeleteCmd = &cobra.Command{
	Use:   "delete <workspace-id>",
	Short: "Delete a workspace",
	Args:  cobra.ExactArgs(1),
	RunE: func(cmd *cobra.Command, args []string) error {
		if err := apiClient.DeleteWorkspace(args[0]); err != nil {
			return err
		}
		fmt.Printf("Workspace %s deleted.\n", args[0])
		return nil
	},
}

func init() {
	workspaceCreateCmd.Flags().StringVarP(&wsCreateName, "name", "n", "", "Workspace name (required)")
	workspaceCreateCmd.Flags().StringVarP(&wsCreateDescription, "description", "d", "", "Workspace description")
	workspaceCreateCmd.MarkFlagRequired("name") //nolint:errcheck

	workspaceCmd.AddCommand(workspaceListCmd)
	workspaceCmd.AddCommand(workspaceGetCmd)
	workspaceCmd.AddCommand(workspaceCreateCmd)
	workspaceCmd.AddCommand(workspaceDeleteCmd)
}
