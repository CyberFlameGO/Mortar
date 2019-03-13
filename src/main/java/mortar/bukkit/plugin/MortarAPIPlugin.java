package mortar.bukkit.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.bukkit.Bukkit;

import mortar.api.config.Configurator;
import mortar.api.nms.Catalyst;
import mortar.api.nms.NMP;
import mortar.api.sched.J;
import mortar.bukkit.command.Command;
import mortar.bukkit.command.Permission;
import mortar.compute.math.M;
import mortar.lib.control.CacheController;
import mortar.lib.control.MojangProfileController;
import mortar.lib.control.RiftController;
import mortar.util.queue.PhantomQueue;
import mortar.util.queue.Queue;
import mortar.util.text.C;
import mortar.util.text.D;

public class MortarAPIPlugin extends MortarPlugin
{
	@Instance
	public static MortarAPIPlugin p;

	@Control
	private CacheController cacheController;

	@Control
	private MojangProfileController mojangProfileController;

	@Control
	private RiftController riftController;

	@Command
	private CommandMortar mort;

	@Command
	private CommandClearConsole cls;

	@Permission
	public static PermissionMortar perm;
	private static Queue<String> logQueue;
	private MortarConfig cfg;

	@Override
	public void start()
	{
		Configurator.JSON.load(cfg = new MortarConfig(), getDataFile("config.json"));
		v("Configuration Loaded... Looks like we're in debug mode!");
		M.initTicking();
		v("Ticking Initiated");

		if(MortarConfig.UPDATES)
		{
			J.a(() -> checkForUpdates(false));
		}

		J.sr(() -> flushLogBuffer(), 10);
		J.ar(() -> M.uptickAsync(), 0);
		J.sr(() -> M.uptick(), 0);
		v("Updating & Log Flushing Initiated");
		startNMS();
	}

	public MortarConfig getMortarConfig()
	{
		return cfg;
	}

	private void startNMS()
	{
		v("Selecting a suitable NMP Catalyst");
		NMP.host = Catalyst.host;

		if(NMP.host != null)
		{
			v("Starting " + NMP.host.getVersion() + " Catalyst");
			NMP.host.start();
			v("NMP Catalyst " + NMP.host.getVersion() + " Online");
		}
	}

	private void flushLogBuffer()
	{
		if(logQueue == null)
		{
			return;
		}

		while(logQueue.hasNext())
		{
			Bukkit.getConsoleSender().sendMessage(logQueue.next());
		}
	}

	@Override
	public void stop()
	{
		stopNMS();
		flushLogBuffer();
	}

	private void stopNMS()
	{
		v("Stopping Catalyst Host");

		if(NMP.host == null)
		{
			v("Looks like there is no NMP host to shut down. Meh, whatever");
		}

		else
		{
			try
			{
				v("Stopping NMP host " + NMP.host.getVersion());
				NMP.host.stop();
				v("NMP host " + NMP.host.getVersion() + " Offline");
			}

			catch(Throwable e)
			{
				v("NMP host " + NMP.host.getVersion() + " is mostly Offline...");
			}
		}
	}

	@Override
	public String getTag(String t)
	{
		if(t.trim().isEmpty())
		{
			return Mortar.tag(getName());
		}

		return Mortar.tag(getName() + " " + C.GRAY + " - " + C.WHITE + t);
	}

	public void checkForUpdates(boolean install)
	{
		try
		{
			D.as("Mortar Updater").l("Checking for Updates");
			URL dl = new URL("https://raw.githubusercontent.com/VolmitSoftware/Mortar/master/release/Mortar.jar");
			URL url = new URL("https://raw.githubusercontent.com/VolmitSoftware/Mortar/master/version.txt");
			InputStream in = url.openStream();
			BufferedReader bu = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			String version = bu.readLine().trim().toLowerCase();
			String current = getDescription().getVersion().trim().toLowerCase();
			in.close();
			bu.close();

			if(version.equals(current))
			{
				D.as("Mortar Updater").l("Mortar " + current + " is up to date!");
			}

			else
			{
				D.as("Mortar Updater").l("Updates are avalible: " + current + " -> " + version);
				HttpURLConnection con = (HttpURLConnection) dl.openConnection();
				HttpURLConnection.setFollowRedirects(false);
				con.setConnectTimeout(10000);
				con.setReadTimeout(10000);
				D.as("Mortar Updater").l("Downloading Update v" + version);
				InputStream inx = con.getInputStream();
				new File("plugins/update").mkdirs();
				File mortar = new File("plugins/update/" + getFile().getName());
				FileOutputStream fos = new FileOutputStream(mortar);
				byte[] buf = new byte[16819];
				int r = 0;

				while((r = inx.read(buf)) != -1)
				{
					fos.write(buf, 0, r);
				}

				fos.close();
				inx.close();
				con.disconnect();
				D.as("Mortar Updater").w("Update v" + version + " downloaded.");
				D.as("Mortar Updater").w("Restart to apply");
			}
		}

		catch(Throwable e)
		{
			D.as("Mortar Updater").f("Failed to check for updates.");
			if(MortarConfig.DEBUG_LOGGING)
			{
				e.printStackTrace();
			}
		}
	}

	public static void log(String string)
	{
		if(logQueue == null)
		{
			logQueue = new PhantomQueue<>();
		}

		logQueue.queue(string);
	}
}
