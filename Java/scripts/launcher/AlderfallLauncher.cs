using System;
using System.Diagnostics;
using System.IO;
using System.Text;
using System.Windows.Forms;

// Small Windows entry point; the shared script owns Java selection and building.
internal static class AlderfallLauncher {
    [STAThread]
    private static int Main(string[] args) {
        try {
            DirectoryInfo root = new DirectoryInfo(AppDomain.CurrentDomain.BaseDirectory);
            while (root != null && !(File.Exists(Path.Combine(root.FullName, "scripts", "run.ps1"))
                    && Directory.Exists(Path.Combine(root.FullName, "assets")))) root = root.Parent;
            if (root == null) throw new InvalidOperationException("Keep Alderfall.exe inside the Java project folder (or a subfolder). The game files could not be found.");
            if (args.Length == 1 && args[0] == "--check") return 0;
            string script = Path.Combine(root.FullName, "scripts", "run.ps1");
            var info = new ProcessStartInfo {
                FileName = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.System), "WindowsPowerShell", "v1.0", "powershell.exe"),
                Arguments = "-NoProfile -ExecutionPolicy Bypass -File \"" + script + "\" -Launcher -ConservativeJit",
                WorkingDirectory = root.FullName, UseShellExecute = false, CreateNoWindow = true,
                WindowStyle = ProcessWindowStyle.Hidden, RedirectStandardOutput = true, RedirectStandardError = true
            };
            var output = new StringBuilder();
            using (var process = new Process { StartInfo = info }) {
                DataReceivedEventHandler capture = (sender, e) => { if (e.Data != null) lock (output) { if(output.Length > 24000) output.Remove(0,12000); output.AppendLine(e.Data); } };
                process.OutputDataReceived += capture; process.ErrorDataReceived += capture;
                process.Start(); process.BeginOutputReadLine(); process.BeginErrorReadLine(); process.WaitForExit();
                if (process.ExitCode != 0) throw new InvalidOperationException("Alderfall could not start.\n\n" + output);
            }
            return 0;
        } catch (Exception error) {
            MessageBox.Show(error.Message, "Alderfall launcher", MessageBoxButtons.OK, MessageBoxIcon.Error);
            return 1;
        }
    }
}
