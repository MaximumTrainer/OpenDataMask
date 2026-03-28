package cmd

import (
	"fmt"
	"os"
	"text/tabwriter"

	"github.com/spf13/cobra"
)

var jobCmd = &cobra.Command{
	Use:   "job",
	Short: "Manage masking jobs",
}

// ---- list ---------------------------------------------------------------

var jobListCmd = &cobra.Command{
	Use:   "list <workspace-id>",
	Short: "List jobs in a workspace",
	Args:  cobra.ExactArgs(1),
	RunE: func(cmd *cobra.Command, args []string) error {
		jobs, err := apiClient.ListJobs(args[0])
		if err != nil {
			return err
		}

		w := tabwriter.NewWriter(os.Stdout, 0, 0, 3, ' ', 0)
		fmt.Fprintln(w, "ID\tSTATUS\tCREATED AT\tSTARTED AT\tCOMPLETED AT")
		for _, j := range jobs {
			fmt.Fprintf(w, "%d\t%s\t%s\t%s\t%s\n",
				j.ID, j.Status, j.CreatedAt, j.StartedAt, j.CompletedAt)
		}
		return w.Flush()
	},
}

// ---- run ----------------------------------------------------------------

var jobRunCmd = &cobra.Command{
	Use:   "run <workspace-id>",
	Short: "Create and start a masking job",
	Args:  cobra.ExactArgs(1),
	RunE: func(cmd *cobra.Command, args []string) error {
		job, err := apiClient.RunJob(args[0])
		if err != nil {
			return err
		}
		fmt.Printf("Job started: id=%d status=%s\n", job.ID, job.Status)
		return nil
	},
}

// ---- status -------------------------------------------------------------

var jobStatusCmd = &cobra.Command{
	Use:   "status <workspace-id> <job-id>",
	Short: "Get the status of a job",
	Args:  cobra.ExactArgs(2),
	RunE: func(cmd *cobra.Command, args []string) error {
		job, err := apiClient.GetJob(args[0], args[1])
		if err != nil {
			return err
		}
		fmt.Printf("ID           : %d\n", job.ID)
		fmt.Printf("Status       : %s\n", job.Status)
		fmt.Printf("Created At   : %s\n", job.CreatedAt)
		fmt.Printf("Started At   : %s\n", job.StartedAt)
		fmt.Printf("Completed At : %s\n", job.CompletedAt)
		if job.ErrorMessage != "" {
			fmt.Printf("Error        : %s\n", job.ErrorMessage)
		}
		return nil
	},
}

// ---- logs ---------------------------------------------------------------

var jobLogsCmd = &cobra.Command{
	Use:   "logs <workspace-id> <job-id>",
	Short: "Stream logs for a job",
	Args:  cobra.ExactArgs(2),
	RunE: func(cmd *cobra.Command, args []string) error {
		logs, err := apiClient.GetJobLogs(args[0], args[1])
		if err != nil {
			return err
		}

		for _, l := range logs {
			fmt.Printf("[%s] %s  %s\n", l.CreatedAt, l.Level, l.Message)
		}
		return nil
	},
}

// ---- cancel -------------------------------------------------------------

var jobCancelCmd = &cobra.Command{
	Use:   "cancel <workspace-id> <job-id>",
	Short: "Cancel a running job",
	Args:  cobra.ExactArgs(2),
	RunE: func(cmd *cobra.Command, args []string) error {
		job, err := apiClient.CancelJob(args[0], args[1])
		if err != nil {
			return err
		}
		fmt.Printf("Job %d cancelled (status: %s)\n", job.ID, job.Status)
		return nil
	},
}

func init() {
	jobCmd.AddCommand(jobListCmd)
	jobCmd.AddCommand(jobRunCmd)
	jobCmd.AddCommand(jobStatusCmd)
	jobCmd.AddCommand(jobLogsCmd)
	jobCmd.AddCommand(jobCancelCmd)
}
